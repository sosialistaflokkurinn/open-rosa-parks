/**
 * Rósa Parks — Cloudflare Worker bakendi
 *
 * Svæðagögn sótt beint úr opinberri ArcGIS REST þjónustu Reykjavíkurborgar
 * (Borgarvefsja) — engin millimannabúð (Parka/EasyPark).
 *
 * Layer 39: Gjaldsvæði Bílastæðasjóðs
 * Layer 47: Bílastæðahús
 */

interface Env {
  BETA_SIGNUPS_DB: D1Database;
  ASSETS: Fetcher;
}

const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, DELETE, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type",
};

// In-memory session store (resets on Worker redeploy — fine for mock)
const MAX_SESSIONS = 1000;
const SESSION_TTL_MS = 24 * 60 * 60 * 1000; // 24 hours
const sessions = new Map<string, ParkingSessionData>();

interface ParkingSessionData {
  id: string;
  plate: string;
  zoneId: string;
  startTime: string; // ISO 8601
  startMillis: number;
}

function generateId(): string {
  return crypto.randomUUID();
}

/** Evict expired sessions */
function evictExpiredSessions(): void {
  const now = Date.now();
  for (const [id, session] of sessions) {
    if (now - session.startMillis > SESSION_TTL_MS) {
      sessions.delete(id);
    }
  }
}

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

// Borgarvefsjá ArcGIS REST — Reykjavíkurborg
const BVS_BASE =
  "https://borgarvefsja.reykjavik.is/arcgis/rest/services/Borgarvefsja/Borgarvefsja_over/MapServer";
const LAYER_GJALDSVAEDI = 39;
const LAYER_BILASTAEDAHUS = 47;

// GJALDSVAEDI value → our zone ID. Values 1 og 5 eru bæði rauð svæði; borgin skilur
// þar á milli vegna íbúakorta (gilda/gilda ekki). Við sameinum í P1 fyrir v1;
// íbúakortarökræn bíður síðari útgáfu.
const GJALDSVAEDI_TO_ZONE: Record<number, string> = {
  1: "P1",
  5: "P1",
  2: "P2",
  3: "P3",
  4: "P4",
};

// Verðskrá Bílastæðasjóðs (opinber, uppfært handvirkt úr bilastaedasjodur.is).
// Worker gefur verð í svari; appið birtir þau. Verð geta breyst — endurskoða við
// hverja gjaldskrárbreytingu.
const ZONE_PRICING: Record<string, ZonePricing> = {
  P1: {
    name: "P1 — Rautt svæði",
    description: "Miðborg innri (Bankastræti, Laugavegur)",
    pricePerHour: 660,
    maxHours: 3,
    weekdayHours: "09:00–21:00",
    weekendHours: "10:00–21:00",
  },
  P2: {
    name: "P2 — Blátt svæði",
    description: "Miðborg ytri (Hlemmur, Hljómskálagarður)",
    pricePerHour: 240,
    weekdayHours: "09:00–21:00",
    weekendHours: "10:00–21:00",
  },
  P3: {
    name: "P3 — Grænt svæði",
    description: "Vesturbær, Þingholt",
    pricePerHour: 240,
    initialHours: 2,
    pricePerHourAfterInitial: 70,
    weekdayHours: "09:00–18:00",
  },
  P4: {
    name: "P4 — Gult svæði",
    description: "Laugardalur, Háskóli",
    pricePerHour: 240,
    weekdayHours: "08:00–16:00",
  },
};

interface ZonePricing {
  name: string;
  description: string;
  pricePerHour: number;
  maxHours?: number;
  initialHours?: number;
  pricePerHourAfterInitial?: number;
  weekdayHours?: string;
  weekendHours?: string;
}

interface ArcGisGeoJson {
  type: "FeatureCollection";
  features: Array<{
    type: "Feature";
    geometry: { type: "Polygon"; coordinates: number[][][] } | null;
    properties: Record<string, unknown>;
  }>;
}

function json(data: unknown, status = 200, extraHeaders?: Record<string, string>): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      "Content-Type": "application/json",
      ...CORS_HEADERS,
      ...(extraHeaders ?? {}),
    },
  });
}

/**
 * Edge-cache wrapper. Returns a cached response when available; otherwise runs
 * `producer`, stores the result in Cloudflare's default cache, and returns it.
 *
 * Cache key is derived from the request URL + method (standard Workers cache
 * behaviour). Use for idempotent GETs where stale-by-up-to-TTL is acceptable.
 */
async function cached(
  request: Request,
  ctx: ExecutionContext,
  ttlSeconds: number,
  producer: () => Promise<Response>,
): Promise<Response> {
  const cache = caches.default;
  const hit = await cache.match(request);
  if (hit) return hit;

  const fresh = await producer();
  if (fresh.ok) {
    const cacheable = new Response(fresh.clone().body, {
      status: fresh.status,
      headers: {
        ...Object.fromEntries(fresh.headers),
        "Cache-Control": `public, max-age=${ttlSeconds}, s-maxage=${ttlSeconds}`,
      },
    });
    ctx.waitUntil(cache.put(request, cacheable.clone()));
    return cacheable;
  }
  return fresh;
}

/** GET /api/zones — Gjaldsvæði Bílastæðasjóðs með polygons og verðlista */
async function handleZones(): Promise<Response> {
  const url =
    `${BVS_BASE}/${LAYER_GJALDSVAEDI}/query` +
    `?where=1%3D1&outFields=GJALDSVAEDI&f=geojson&outSR=4326`;
  // 24h upstream cache — zone boundaries only change on council decisions.
  const res = await fetch(url, { cf: { cacheTtl: 86400, cacheEverything: true } });
  if (!res.ok) return json({ error: "upstream_error", status: res.status }, 502);
  const geo = (await res.json()) as ArcGisGeoJson;

  // Group polygons by zone ID. Each feature has a single Polygon geometry.
  // We flatten into one array of rings per zone — ZoneMapScreen renders each
  // ring as a separate feature (MultiPolygon-like behaviour).
  const polygonsByZone: Record<string, number[][][]> = {};
  for (const feat of geo.features) {
    const gjald = feat.properties?.GJALDSVAEDI;
    const zoneId =
      typeof gjald === "number" ? GJALDSVAEDI_TO_ZONE[gjald] : undefined;
    if (!zoneId || !feat.geometry) continue;
    const rings = feat.geometry.coordinates; // number[][][]  — rings of [lng,lat]
    (polygonsByZone[zoneId] ??= []).push(...rings);
  }

  const zones = Object.entries(ZONE_PRICING).map(([id, pricing]) => ({
    id,
    name: pricing.name,
    description: pricing.description,
    coordinates: polygonsByZone[id] ?? [],
    pricing: {
      pricePerHour: pricing.pricePerHour,
      maxHours: pricing.maxHours ?? null,
      initialHours: pricing.initialHours ?? null,
      pricePerHourAfterInitial: pricing.pricePerHourAfterInitial ?? null,
      weekdayHours: pricing.weekdayHours ?? null,
      weekendHours: pricing.weekendHours ?? null,
    },
    fee: "0 kr.",
  }));

  return json({
    zones,
    source: "borgarvefsja-arcgis",
    sourceUrl: `${BVS_BASE}/${LAYER_GJALDSVAEDI}`,
  });
}

/** GET /api/garages — Bílastæðahús Bílastæðasjóðs */
async function handleGarages(): Promise<Response> {
  const url =
    `${BVS_BASE}/${LAYER_BILASTAEDAHUS}/query` +
    `?where=1%3D1&outFields=*&f=geojson&outSR=4326`;
  // 24h upstream cache — garage footprints are stable.
  const res = await fetch(url, { cf: { cacheTtl: 86400, cacheEverything: true } });
  if (!res.ok) return json({ error: "upstream_error", status: res.status }, 502);
  const geo = (await res.json()) as ArcGisGeoJson;

  // Borgarvefsja prefixar reit-nöfn með schema. Dæmi:
  //   LUK_VEFSJA.RVK_FASTEIGN_BILASTAEDAHUS.HEITI
  const garages = geo.features
    .filter((f) => f.geometry !== null)
    .map((f, idx) => {
      const props = f.properties ?? {};
      const heiti =
        findProp(props, "HEITI") ??
        findProp(props, "NAFN") ??
        `Bílastæðahús ${idx + 1}`;
      const url = findProp(props, "HEIMASIDA_URL") ?? "https://bilastaedasjodur.is";
      return {
        id: `G${idx + 1}`,
        name: String(heiti),
        url: String(url),
        coordinates: f.geometry!.coordinates,
      };
    });

  return json({
    garages,
    source: "borgarvefsja-arcgis",
    sourceUrl: `${BVS_BASE}/${LAYER_BILASTAEDAHUS}`,
  });
}

function findProp(
  props: Record<string, unknown>,
  suffix: string,
): string | undefined {
  for (const [k, v] of Object.entries(props)) {
    if (k.endsWith(`.${suffix}`) || k === suffix) {
      return typeof v === "string" ? v : undefined;
    }
  }
  return undefined;
}

/** POST /api/parking/start — Start a parking session */
async function handleParkingStart(request: Request): Promise<Response> {
  let body: { plate?: string; zoneId?: string };
  try {
    body = (await request.json()) as { plate?: string; zoneId?: string };
  } catch {
    return json({ error: "invalid JSON body" }, 400);
  }
  const { plate, zoneId } = body;

  if (!plate || !zoneId) {
    return json({ error: "plate and zoneId required" }, 400);
  }

  if (!/^[A-Z0-9]{2,6}$/i.test(plate)) {
    return json({ error: "invalid plate format" }, 400);
  }
  if (!/^(P[1-4]|G[1-7])$/i.test(zoneId)) {
    return json({ error: "invalid zoneId" }, 400);
  }

  evictExpiredSessions();

  // Check for existing active session for this plate
  for (const session of sessions.values()) {
    if (session.plate === plate.toUpperCase()) {
      return json({ error: "active_session_exists", sessionId: session.id }, 409);
    }
  }

  if (sessions.size >= MAX_SESSIONS) {
    return json({ error: "too_many_sessions" }, 503);
  }

  const now = new Date();
  const session: ParkingSessionData = {
    id: generateId(),
    plate: plate.toUpperCase(),
    zoneId: zoneId.toUpperCase(),
    startTime: now.toISOString(),
    startMillis: now.getTime(),
  };
  sessions.set(session.id, session);

  return json(
    {
      sessionId: session.id,
      plate: session.plate,
      zoneId: session.zoneId,
      startTime: session.startTime,
      status: "active",
    },
    201,
  );
}

/** DELETE /api/parking/:id — Stop a parking session */
function handleParkingStop(sessionId: string): Response {
  if (!UUID_RE.test(sessionId)) {
    return json({ error: "invalid_session_id" }, 400);
  }
  const session = sessions.get(sessionId);
  if (!session) {
    return json({ error: "session_not_found" }, 404);
  }

  const now = new Date();
  const durationMinutes = Math.round((now.getTime() - session.startMillis) / 60_000);

  sessions.delete(sessionId);

  return json({
    sessionId: session.id,
    plate: session.plate,
    zoneId: session.zoneId,
    startTime: session.startTime,
    endTime: now.toISOString(),
    durationMinutes,
    status: "completed",
  });
}

/** GET /api/parking/active?plate=XX123 — Get active session for plate */
function handleParkingActive(url: URL): Response {
  const plate = url.searchParams.get("plate")?.toUpperCase();

  if (!plate) {
    return json({ error: "plate query parameter required" }, 400);
  }

  for (const session of sessions.values()) {
    if (session.plate === plate) {
      const elapsed = Math.round((Date.now() - session.startMillis) / 60_000);
      return json({
        sessionId: session.id,
        plate: session.plate,
        zoneId: session.zoneId,
        startTime: session.startTime,
        elapsedMinutes: elapsed,
        status: "active",
      });
    }
  }
  return json({ session: null, status: "no_active_session" });
}

// Beta-tester signup — RFC 5322-ish email regex. Not perfectly strict (full
// RFC is impractical), but catches the common shape and blocks obviously bad
// input before we hit the database.
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;
const ALLOWED_LOCALES = new Set(["en", "is"]);

interface BetaSignupPayload {
  email?: unknown;
  locale?: unknown;
  source?: unknown;
  consent?: unknown;
}

async function handleBetaSignup(request: Request, env: Env): Promise<Response> {
  let payload: BetaSignupPayload;
  try {
    payload = (await request.json()) as BetaSignupPayload;
  } catch {
    return json({ error: "invalid_json" }, 400);
  }

  const rawEmail = typeof payload.email === "string" ? payload.email.trim().toLowerCase() : "";
  if (rawEmail.length === 0 || rawEmail.length > 254 || !EMAIL_REGEX.test(rawEmail)) {
    return json({ error: "invalid_email" }, 400);
  }

  const locale = typeof payload.locale === "string" && ALLOWED_LOCALES.has(payload.locale)
    ? payload.locale
    : "en";

  if (payload.consent !== true) {
    return json({ error: "consent_required" }, 400);
  }

  const source = typeof payload.source === "string" && payload.source.length > 0
    ? payload.source.slice(0, 64)
    : null;

  const userAgent = request.headers.get("User-Agent") ?? "";
  const userAgentPrefix = userAgent.slice(0, 120);

  const result = await env.BETA_SIGNUPS_DB.prepare(
    `INSERT INTO beta_signups (email, locale, source, user_agent_prefix)
     VALUES (?1, ?2, ?3, ?4)
     ON CONFLICT(email) DO NOTHING`,
  )
    .bind(rawEmail, locale, source, userAgentPrefix)
    .run();

  const created = (result.meta?.changes ?? 0) > 0;
  return json({ status: created ? "registered" : "already_registered" });
}

async function handleBetaSignupCount(env: Env): Promise<Response> {
  const row = await env.BETA_SIGNUPS_DB.prepare(
    "SELECT COUNT(*) AS total FROM beta_signups",
  ).first<{ total: number }>();
  return json({ total: row?.total ?? 0 });
}

export default {
  async fetch(request: Request, env: Env, ctx: ExecutionContext): Promise<Response> {
    const url = new URL(request.url);

    if (request.method === "OPTIONS") {
      return new Response(null, { headers: CORS_HEADERS });
    }

    // Parking session routes — never cached, they mutate server state
    if (url.pathname === "/api/parking/start" && request.method === "POST") {
      return handleParkingStart(request);
    }
    if (url.pathname.startsWith("/api/parking/stop/") && request.method === "DELETE") {
      const sessionId = url.pathname.replace("/api/parking/stop/", "");
      return handleParkingStop(sessionId);
    }
    if (url.pathname === "/api/parking/active" && request.method === "GET") {
      return handleParkingActive(url);
    }

    // Landing-page beta tester signup. Writes to the BETA_SIGNUPS_DB D1 binding;
    // emails are later exported and imported into the Play Console internal-
    // testing email list.
    if (url.pathname === "/beta/signup" && request.method === "POST") {
      return handleBetaSignup(request, env);
    }
    if (url.pathname === "/beta/signup-count" && request.method === "GET") {
      return handleBetaSignupCount(env);
    }

    // API help at /api — the root "/" serves the landing page via the
    // ASSETS binding now.
    if (url.pathname === "/api" || url.pathname === "/api/") {
      return new Response(
        "Rósa Parks API — bílastæðaapp án þjónustugjalds\n" +
          "Gögn úr opinberu ArcGIS Reykjavíkurborgar (Borgarvefsja).\n\n" +
          "GET  /api/zones             — Gjaldsvæði Bílastæðasjóðs + verðlisti\n" +
          "GET  /api/garages           — Bílastæðahús Bílastæðasjóðs\n" +
          "POST /api/parking/start     — Hefja stöðumælalotu\n" +
          "DELETE /api/parking/stop/:id — Ljúka lotu\n" +
          "GET  /api/parking/active    — Virk lota (?plate=XX123)\n",
        { headers: { "Content-Type": "text/plain; charset=utf-8", ...CORS_HEADERS } },
      );
    }

    // Edge-cache static GIS data for 6 hours. Combined with the 24h upstream
    // cache on the ArcGIS fetch itself, this limits real requests to
    // borgarvefsja.reykjavik.is to at most a few per day per Cloudflare POP,
    // regardless of how many clients are opening the app.
    if (url.pathname === "/api/zones") {
      return cached(request, ctx, 6 * 60 * 60, handleZones);
    }
    if (url.pathname === "/api/garages") {
      return cached(request, ctx, 6 * 60 * 60, handleGarages);
    }

    // Unknown /api/* paths are a JSON 404; everything else falls through to
    // the static landing-page bundle (signup landing, rosapark-icon, og-image,
    // fonts, built Vite JS/CSS).
    if (url.pathname.startsWith("/api/")) {
      return json({ error: "Not found" }, 404);
    }
    return env.ASSETS.fetch(request);
  },
} satisfies ExportedHandler<Env>;
