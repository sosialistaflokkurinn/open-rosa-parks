/**
 * Rósa Parks — Cloudflare Worker bakendi
 *
 * Svæðagögn sótt beint úr opinberri ArcGIS REST þjónustu Reykjavíkurborgar
 * (Borgarvefsja) — engin millimannabúð (Parka/EasyPark).
 *
 * Layer 39: Gjaldsvæði Bílastæðasjóðs
 * Layer 47: Bílastæðahús
 */

import {
  BlikkApiError,
  BlikkClient,
  isPaidStatus,
  isTerminalStatus,
} from "./blikk";

interface Env {
  BETA_SIGNUPS_DB: D1Database;
  PARKING_DB: D1Database;
  ASSETS: Fetcher;
  /** Blikk Ecom sales-channel key. Unset on the deployed worker — the Blikk
   * test routes answer 501 without it, so they are inert in production. */
  BLIKK_API_KEY?: string;
}

const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, DELETE, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type",
};

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

// Backpressure on writes — fail fast if there are absurdly many open
// sessions. D1 itself can handle vastly more, but a stuck-open count this
// high is almost certainly spam, not legitimate concurrent parking. Cheap
// to evaluate via the partial index idx_parking_sessions_plate_active.
const MAX_OPEN_SESSIONS = 1000;

// Retention — enforced by the hourly `scheduled` handler below, honoring the
// privacy policy's promise that session rows are deleted within 24 h of the
// session ending. 23 h threshold + hourly cron ⇒ worst-case deletion 24 h
// after end_millis is set. Open sessions with no /stop for 7 days are treated
// as orphaned (client uninstalled/reset mid-session) and deleted outright —
// no legitimate on-street parking lasts a week, and dropping them also keeps
// the MAX_OPEN_SESSIONS backpressure from filling with dead rows.
const ENDED_RETENTION_MS = 23 * 60 * 60 * 1000;
const ORPHAN_OPEN_MAX_AGE_MS = 7 * 24 * 60 * 60 * 1000;

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
    name: "P1: Rautt svæði",
    description: "Miðborg innri (Bankastræti, Laugavegur)",
    pricePerHour: 660,
    maxHours: 3,
    weekdayHours: "09:00–21:00",
    weekendHours: "10:00–21:00",
  },
  P2: {
    name: "P2: Blátt svæði",
    description: "Miðborg ytri (Hlemmur, Hljómskálagarður)",
    pricePerHour: 240,
    weekdayHours: "09:00–21:00",
    weekendHours: "10:00–21:00",
  },
  P3: {
    name: "P3: Grænt svæði",
    description: "Vesturbær, Þingholt",
    pricePerHour: 240,
    initialHours: 2,
    pricePerHourAfterInitial: 70,
    weekdayHours: "09:00–18:00",
  },
  P4: {
    name: "P4: Gult svæði",
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

interface ParkingSessionRow {
  id: string;
  plate: string;
  zone_id: string;
  start_millis: number;
  end_millis: number | null;
}

/** POST /api/parking/start — Start a parking session */
async function handleParkingStart(request: Request, env: Env): Promise<Response> {
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

  const normalizedPlate = plate.toUpperCase();
  const normalizedZone = zoneId.toUpperCase();

  // Active-session guard. Uses idx_parking_sessions_plate_active (partial
  // index on open rows). TOCTOU race window between SELECT and INSERT is
  // acceptable for v1 — same characteristic as the in-memory implementation
  // it replaces. If duplicate creation becomes a real problem, add a UNIQUE
  // partial index in a follow-up migration.
  const existingActive = await env.PARKING_DB
    .prepare("SELECT id FROM parking_sessions WHERE plate = ?1 AND end_millis IS NULL LIMIT 1")
    .bind(normalizedPlate)
    .first<{ id: string }>();
  if (existingActive) {
    return json({ error: "active_session_exists", sessionId: existingActive.id }, 409);
  }

  const openCount = await env.PARKING_DB
    .prepare("SELECT COUNT(*) AS n FROM parking_sessions WHERE end_millis IS NULL")
    .first<{ n: number }>();
  if ((openCount?.n ?? 0) >= MAX_OPEN_SESSIONS) {
    return json({ error: "too_many_sessions" }, 503);
  }

  const now = new Date();
  const sessionId = crypto.randomUUID();
  await env.PARKING_DB
    .prepare(
      `INSERT INTO parking_sessions (id, plate, zone_id, start_millis)
       VALUES (?1, ?2, ?3, ?4)`,
    )
    .bind(sessionId, normalizedPlate, normalizedZone, now.getTime())
    .run();

  return json(
    {
      sessionId,
      plate: normalizedPlate,
      zoneId: normalizedZone,
      startTime: now.toISOString(),
      status: "active",
    },
    201,
  );
}

/** DELETE /api/parking/stop/:id — Stop a parking session */
async function handleParkingStop(sessionId: string, env: Env): Promise<Response> {
  if (!UUID_RE.test(sessionId)) {
    return json({ error: "invalid_session_id" }, 400);
  }

  const row = await env.PARKING_DB
    .prepare(
      `SELECT id, plate, zone_id, start_millis, end_millis
       FROM parking_sessions WHERE id = ?1`,
    )
    .bind(sessionId)
    .first<ParkingSessionRow>();
  if (!row || row.end_millis !== null) {
    return json({ error: "session_not_found" }, 404);
  }

  const now = new Date();
  const endMillis = now.getTime();
  const durationMinutes = Math.round((endMillis - row.start_millis) / 60_000);

  // total_cost_kr stays NULL for now — pricing logic lives client-side in
  // ParkingViewModel. Future: either move pricing into the worker or have
  // the client send the computed cost in the stop request body.
  //
  // The `end_millis IS NULL` guard makes the UPDATE idempotent under a
  // race: if a concurrent DELETE has already closed the session between
  // our SELECT and this UPDATE, changes will be 0 and we report 404
  // instead of silently overwriting the prior end_millis.
  const result = await env.PARKING_DB
    .prepare(
      "UPDATE parking_sessions SET end_millis = ?1 WHERE id = ?2 AND end_millis IS NULL",
    )
    .bind(endMillis, sessionId)
    .run();
  if ((result.meta?.changes ?? 0) === 0) {
    return json({ error: "session_not_found" }, 404);
  }

  return json({
    sessionId: row.id,
    plate: row.plate,
    zoneId: row.zone_id,
    startTime: new Date(row.start_millis).toISOString(),
    endTime: now.toISOString(),
    durationMinutes,
    status: "completed",
  });
}

/** GET /api/parking/active?plate=XX123 — Get active session for plate */
async function handleParkingActive(url: URL, env: Env): Promise<Response> {
  const plate = url.searchParams.get("plate")?.toUpperCase();

  if (!plate) {
    return json({ error: "plate query parameter required" }, 400);
  }

  // ORDER BY start_millis DESC: if duplicate active sessions exist for the
  // same plate (only possible via the documented TOCTOU on /start, no
  // legitimate path), prefer the most recent. This is a deliberate change
  // from the in-memory implementation, which iterated in insertion order
  // and returned the oldest. New behaviour matches user intent: when
  // resuming, show the session they just started.
  const row = await env.PARKING_DB
    .prepare(
      `SELECT id, plate, zone_id, start_millis
       FROM parking_sessions
       WHERE plate = ?1 AND end_millis IS NULL
       ORDER BY start_millis DESC
       LIMIT 1`,
    )
    .bind(plate)
    .first<Pick<ParkingSessionRow, "id" | "plate" | "zone_id" | "start_millis">>();

  if (!row) {
    return json({ session: null, status: "no_active_session" });
  }

  const elapsed = Math.round((Date.now() - row.start_millis) / 60_000);
  return json({
    sessionId: row.id,
    plate: row.plate,
    zoneId: row.zone_id,
    startTime: new Date(row.start_millis).toISOString(),
    elapsedMinutes: elapsed,
    status: "active",
  });
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

// --- Blikk payment probe (docs/greidslugatt/DECISION.md v2) -----------------
//
// A deliberately minimal surface for exercising the Blikk Ecom rail from a
// debug build on a real phone. It is NOT the top-up feature: the architecture
// question of whether money lands in our account or the fund's is still open
// (DECISION.md §3.1), and nothing here presumes an answer. No balance, no
// ledger, no persistence — create a payment, hand back its id, read its
// status. That is the whole surface.
//
// Three properties keep it safe to have in the tree:
//
//  1. It answers 501 unless BLIKK_API_KEY is set. The secret is deliberately
//     NOT set on the deployed worker, so the routes are dead on rp.xj.is.
//  2. The amount is capped at BLIKK_MAX_TEST_AMOUNT_KR. This surface can never
//     initiate a payment big enough to matter.
//  3. Payer identifiers are passed through, never kept. The anonymous path
//     sends none at all — the payer identifies on Blikk's own hosted page, so
//     no kennitala reaches this worker. The Direct Debtor path necessarily
//     carries kennitala + BBAN, and they are used for exactly one outbound
//     call and then dropped: not stored, not logged, not echoed. Before this
//     path ships to real users it needs a purpose statement in the privacy
//     policy (`blikk-integration/rosa-parks-topup-flow.md`), which is a
//     separate decision from proving the rail works.
//
// Blikk binds the creditor to the sales channel's own kennitala (the name is
// not overridable at all, and 4/4 third-party overrides were REJECTED at SCA
// — flue-lab#76), so the money can only ever reach the channel's own account.
const BLIKK_MAX_TEST_AMOUNT_KR = 3;

// Blikk's default TTL is 120 s, which is not enough for a human on a phone
// switching to a banking app. 10 minutes is comfortable and still short.
const BLIKK_PAYMENT_TTL_SECONDS = 600;

// Blikk payment ids are opaque; guard the shape before putting one in a URL.
const BLIKK_ID_RE = /^[A-Za-z0-9-]{8,64}$/;

function blikkClient(env: Env): BlikkClient | null {
  return env.BLIKK_API_KEY ? new BlikkClient({ apiKey: env.BLIKK_API_KEY }) : null;
}

/**
 * Blikk's error body is echoed to the caller so a probe failure is legible on
 * the phone — a 403 on the Direct Debtor path means the sales channel does not
 * have it enabled, which is a finding, not a bug. It is deliberately NOT
 * logged: on the Direct Debtor path the body can quote the request, and the
 * request carries a kennitala.
 */
function blikkErrorResponse(error: unknown): Response {
  if (error instanceof BlikkApiError) {
    console.log(`blikk api error status=${error.status}`);
    return json({ error: "blikk_rejected", status: error.status, detail: error.body.slice(0, 300) }, 502);
  }
  console.log(`blikk unexpected error: ${String(error)}`);
  return json({ error: "blikk_unavailable" }, 502);
}

/** 12 digits, no separators: banki(4) + höfuðbók(2) + reikningsnúmer(6). */
const BBAN_RE = /^\d{12}$/;
const KENNITALA_RE = /^\d{10}$/;

async function handleBlikkTestPayment(request: Request, env: Env): Promise<Response> {
  const client = blikkClient(env);
  if (!client) return json({ error: "blikk_not_configured" }, 501);

  let payload: { amount?: unknown; debtor?: unknown };
  try {
    payload = (await request.json()) as { amount?: unknown; debtor?: unknown };
  } catch {
    return json({ error: "invalid_json" }, 400);
  }

  const amount = typeof payload.amount === "number" ? payload.amount : 1;
  if (!Number.isInteger(amount) || amount < 1 || amount > BLIKK_MAX_TEST_AMOUNT_KR) {
    return json({ error: "invalid_amount", maxKr: BLIKK_MAX_TEST_AMOUNT_KR }, 400);
  }

  // Three modes, in descending order of how much the payer is spared:
  //
  //   direct-debtor  kennitala + BBAN + name. No Blikk onboarding at all; the
  //                  payer goes straight to their own banking app. This is
  //                  what the top-up design assumes, and it is enabled PER
  //                  SALES CHANNEL — a channel without it answers
  //                  403 "sales channel does not allow direct debtor
  //                  payments", which is what gognin.org answers today
  //                  (probed 2026-08-09). Opening it is a request to Blikk.
  //   identified     kennitala (and/or phone) only, on the ordinary create.
  //                  Blikk already knows who is paying, so the hosted page
  //                  skips the identification step.
  //   anonymous      nothing sent; the payer identifies at Blikk. Also the
  //                  only mode that keeps payer data out of this worker.
  const debtorInput = payload.debtor;
  let debtor: { kennitala: string; bban: string; name: string } | null = null;
  if (debtorInput !== undefined && debtorInput !== null) {
    const raw = debtorInput as { kennitala?: unknown; bban?: unknown; name?: unknown };
    const kennitala = typeof raw.kennitala === "string" ? raw.kennitala.replace(/\D/g, "") : "";
    const bban = typeof raw.bban === "string" ? raw.bban.replace(/\D/g, "") : "";
    const name = typeof raw.name === "string" ? raw.name.trim() : "";
    if (!KENNITALA_RE.test(kennitala)) return json({ error: "invalid_kennitala" }, 400);
    // A BBAN is what escalates an identified payment to Direct Debtor. Empty
    // is legitimate and means "identified, but through the ordinary create".
    if (bban.length > 0 && !BBAN_RE.test(bban)) return json({ error: "invalid_bban" }, 400);
    if (bban.length > 0 && name.length === 0) return json({ error: "debtor_name_required" }, 400);
    debtor = { kennitala, bban, name };
  }

  const common = {
    amount,
    currency: "ISK",
    source: "rosaparks-android-debug",
    sourceReferenceId: crypto.randomUUID(),
    // Verified on stage 2026-07-14: a custom scheme is accepted and comes
    // back with ?paymentId=<id> appended, handing the payer to the app.
    partnerRedirectUrl: "rosaparks://payment-complete",
    // No callbackUrl on purpose — the client polls, and omitting the
    // webhook means Blikk delivers no payment data anywhere but here.
    expiresAt: Math.floor(Date.now() / 1000) + BLIKK_PAYMENT_TTL_SECONDS,
  };

  const mode = debtor === null ? "anonymous" : debtor.bban.length > 0 ? "direct-debtor" : "identified";

  try {
    const created =
      debtor !== null && debtor.bban.length > 0
        ? await client.createDirectDebtorPayment({
            ...common,
            debtorExternalId: debtor.kennitala,
            debtorBban: debtor.bban,
            debtorName: debtor.name,
          })
        : await client.createPayment({
            ...common,
            ...(debtor === null ? {} : { debtorExternalId: debtor.kennitala }),
          });
    return json({
      mode,
      id: created.id,
      status: created.status,
      scaRedirectUrl: created.scaRedirectUrl,
      message: created.message,
      amount,
      expiresInSeconds: BLIKK_PAYMENT_TTL_SECONDS,
    });
  } catch (error) {
    return blikkErrorResponse(error);
  }
}

/**
 * Zero-funds check that an account exists and belongs to a kennitala. Creates
 * no payment and moves nothing, so it is the right thing to run before a
 * Direct Debtor create — and it is what a real signup flow would call when
 * the user first enters their details.
 */
async function handleBlikkValidateAccount(request: Request, env: Env): Promise<Response> {
  const client = blikkClient(env);
  if (!client) return json({ error: "blikk_not_configured" }, 501);

  let payload: { kennitala?: unknown; bban?: unknown };
  try {
    payload = (await request.json()) as { kennitala?: unknown; bban?: unknown };
  } catch {
    return json({ error: "invalid_json" }, 400);
  }

  const kennitala = typeof payload.kennitala === "string" ? payload.kennitala.replace(/\D/g, "") : "";
  const bban = typeof payload.bban === "string" ? payload.bban.replace(/\D/g, "") : "";
  if (!KENNITALA_RE.test(kennitala)) return json({ error: "invalid_kennitala" }, 400);
  if (!BBAN_RE.test(bban)) return json({ error: "invalid_bban" }, 400);

  try {
    const result = await client.validateBban(bban, kennitala);
    return json({
      valid: result.isValid,
      corporate: result.isCorporateAccount,
      message: result.errorMessage,
      // Blikk can hand back a different identifier to use for the payment —
      // surface it rather than silently ignoring it.
      substituteExternalId: result.newExternalId ?? "",
    });
  } catch (error) {
    return blikkErrorResponse(error);
  }
}

async function handleBlikkTestPaymentStatus(paymentId: string, env: Env): Promise<Response> {
  const client = blikkClient(env);
  if (!client) return json({ error: "blikk_not_configured" }, 501);
  if (!BLIKK_ID_RE.test(paymentId)) return json({ error: "invalid_payment_id" }, 400);

  try {
    const payment = await client.getPayment(paymentId);
    return json({
      id: payment.id,
      status: payment.status,
      paid: isPaidStatus(payment.status),
      terminal: isTerminalStatus(payment.status),
      scaRedirectUrl: payment.scaRedirectUrl,
      // Blikk's human-readable reason, e.g. "greiðsla rann út á tíma".
      message: payment.message,
    });
  } catch (error) {
    if (error instanceof BlikkApiError && error.status === 404) {
      return json({ error: "payment_not_found" }, 404);
    }
    return blikkErrorResponse(error);
  }
}

export default {
  async fetch(request: Request, env: Env, ctx: ExecutionContext): Promise<Response> {
    const url = new URL(request.url);

    if (request.method === "OPTIONS") {
      return new Response(null, { headers: CORS_HEADERS });
    }

    // Tag every API call with the calling platform so observability queries
    // can tell the iOS app (URLSession → CFNetwork/Darwin) from the Android
    // app (HttpURLConnection → Dalvik) from browsers/scanners. The automatic
    // invocation log carries no User-Agent, so this is the only place the
    // distinction is recorded.
    if (url.pathname.startsWith("/api/")) {
      const ua = request.headers.get("User-Agent") ?? "";
      const platform =
        ua.includes("CFNetwork") || ua.includes("Darwin")
          ? "ios"
          : ua.includes("Dalvik") || ua.includes("okhttp") || ua.includes("Android")
            ? "android"
            : "other";
      console.log(
        `api ${request.method} ${url.pathname} platform=${platform} ua="${ua.slice(0, 120)}"`,
      );
    }

    // Parking session routes — never cached, they mutate server state.
    // Persisted in D1 via PARKING_DB so an active session survives Worker
    // redeploys, Android process death, and device reboots (#58).
    if (url.pathname === "/api/parking/start" && request.method === "POST") {
      return handleParkingStart(request, env);
    }
    if (url.pathname.startsWith("/api/parking/stop/") && request.method === "DELETE") {
      const sessionId = url.pathname.replace("/api/parking/stop/", "");
      return handleParkingStop(sessionId, env);
    }
    if (url.pathname === "/api/parking/active" && request.method === "GET") {
      return handleParkingActive(url, env);
    }

    // Blikk payment probe — inert (501) unless BLIKK_API_KEY is bound, which
    // it is not on the deployed worker. See the block above these handlers.
    if (url.pathname === "/api/blikk/test-payment" && request.method === "POST") {
      return handleBlikkTestPayment(request, env);
    }
    if (url.pathname === "/api/blikk/validate-account" && request.method === "POST") {
      return handleBlikkValidateAccount(request, env);
    }
    if (url.pathname.startsWith("/api/blikk/test-payment/") && request.method === "GET") {
      return handleBlikkTestPaymentStatus(
        url.pathname.replace("/api/blikk/test-payment/", ""),
        env,
      );
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
        "Rósa Parks API - bílastæðaapp án þjónustugjalds\n" +
          "Gögn úr opinberu ArcGIS Reykjavíkurborgar (Borgarvefsja).\n\n" +
          "GET  /api/zones             - Gjaldsvæði Bílastæðasjóðs + verðlisti\n" +
          "GET  /api/garages           - Bílastæðahús Bílastæðasjóðs\n" +
          "POST /api/parking/start     - Hefja stöðumælalotu\n" +
          "DELETE /api/parking/stop/:id - Ljúka lotu\n" +
          "GET  /api/parking/active    - Virk lota (?plate=XX123)\n",
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

    // Block secret-probe paths explicitly so they 404 instead of leaking into
    // the SPA fallback below. rosa-parks#84 / #86: scanners had been receiving
    // 200s for /.env, /.git/config, /.aws/credentials because the asset bundle
    // (or its SPA fallback) was answering everything.
    if (
      /^\/\.(env|git|aws|ssh|docker|wlwmanifest)/.test(url.pathname) ||
      /^\/(backend\/)?\.env$/.test(url.pathname) ||
      url.pathname === "/config.json"
    ) {
      return new Response("Not Found", { status: 404 });
    }

    // Privacy paths redirect to the canonical privacy surface, which moved from
    // the party site to the co-op site on 2026-07-26 (samtakamatt-vefur#151)
    // now that Samtakamáttur develops and operates the app. The party is still
    // the data controller — it owns this Cloudflare account, the D1 database,
    // the Play developer account and the Apple team — and the page says so.
    //
    // The old party URL stays alive as a redirect; it is still the registered
    // policy URL in Google Play, IARC and Apple TestFlight until those three
    // are repointed, and it should outlive the switch by a good margin.
    //
    // No ?lang= any more: the party page took one, the co-op page is Icelandic
    // with an English-on-request note. #96 rewrote these to /index.html, but
    // Cloudflare's ASSETS binding canonicalizes /index.html → / (303), so the
    // SPA never mounted. #106.
    const PRIVACY_CANONICAL = "https://samtakamatt.is/personuvernd/rosaparks";
    if (url.pathname === "/privacy" || url.pathname === "/personuvernd") {
      return Response.redirect(PRIVACY_CANONICAL, 307);
    }

    return env.ASSETS.fetch(request);
  },

  /**
   * Hourly cleanup (cron in wrangler.jsonc). Deletes ended sessions past the
   * retention window and week-old orphaned open sessions. Uses
   * controller.scheduledTime (not Date.now()) so a delayed invocation still
   * measures age from the tick it was scheduled for.
   */
  async scheduled(controller: ScheduledController, env: Env): Promise<void> {
    const now = controller.scheduledTime;

    const ended = await env.PARKING_DB
      .prepare(
        "DELETE FROM parking_sessions WHERE end_millis IS NOT NULL AND end_millis < ?1",
      )
      .bind(now - ENDED_RETENTION_MS)
      .run();

    // Uses idx_parking_sessions_start; the ended-row DELETE above has no
    // end_millis index but scans a table the cleanup itself keeps small.
    const orphaned = await env.PARKING_DB
      .prepare(
        "DELETE FROM parking_sessions WHERE end_millis IS NULL AND start_millis < ?1",
      )
      .bind(now - ORPHAN_OPEN_MAX_AGE_MS)
      .run();

    console.log(
      `cleanup ended_deleted=${ended.meta?.changes ?? 0} orphans_deleted=${orphaned.meta?.changes ?? 0}`,
    );
  },
} satisfies ExportedHandler<Env>;
