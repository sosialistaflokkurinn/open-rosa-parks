#!/usr/bin/env bash
# Post-deploy verification for the rosa-parks-api Worker.
#
# Runs after `wrangler deploy` (or any change that could affect routing) and
# probes canonical paths against the live worker. Fails (exit 1) if any path
# returns a status that diverges from the expected response.
#
# Usage:
#   ./worker/scripts/verify-deploy.sh                       # probe https://rp.xj.is
#   ./worker/scripts/verify-deploy.sh https://staging.xj.is # probe a different URL
#
# The script was added per claude-config#6 (per-stack verification gates).
# Back-tests against rosa-parks#84 (SPA-fallback masked 404s on /.env, /.git/*,
# /.aws/credentials, etc.) and #86 (the fix that flipped not_found_handling to
# "none" on the ASSETS binding).

set -euo pipefail

BASE_URL="${1:-https://rp.xj.is}"

# Each entry: "path|expected_code|description"
CHECKS=(
  "/|200|landing page (served by ASSETS binding)"
  "/api|200|API help text"
  "/api/zones|200|GIS zones — Bílastæðasjóður price list"
  "/api/garages|200|GIS garages — Bílastæðahús inventory"
  "/privacy|307|307 → canonical xj-site privacy URL (#106)"
  "/personuvernd|307|307 → canonical xj-site privacy URL, Icelandic (#106)"
  "/api/bogus|404|unknown /api/* path returns JSON 404"
  "/.env|404|secret-probe block (rosa-parks#84 — pre-fix returned 200)"
  "/.git/config|404|secret-probe block (rosa-parks#84)"
  "/.aws/credentials|404|secret-probe block (rosa-parks#84)"
  "/api/.env|404|api-prefixed secret probe"
  "/api/parking/active|400|missing required plate query param"
)

printf 'Verifying %s\n\n' "$BASE_URL"

FAILED=0
PASSED=0
for entry in "${CHECKS[@]}"; do
  path="${entry%%|*}"
  rest="${entry#*|}"
  expected="${rest%%|*}"
  desc="${rest#*|}"

  actual=$(curl -s -o /dev/null -w '%{http_code}' --max-time 10 "${BASE_URL}${path}" || echo "000")
  if [[ "$actual" == "$expected" ]]; then
    printf '  PASS  %-22s → %s   (%s)\n' "$path" "$actual" "$desc"
    PASSED=$((PASSED + 1))
  else
    printf '  FAIL  %-22s → %s  (expected %s — %s)\n' "$path" "$actual" "$expected" "$desc"
    FAILED=$((FAILED + 1))
  fi
done

printf '\n%d passed, %d failed\n' "$PASSED" "$FAILED"

if [[ $FAILED -gt 0 ]]; then
  printf '\nVerification failed. Likely causes:\n'
  printf '  - assets.not_found_handling drifted from "none" (SPA fallback masking 404s, like rosa-parks#84)\n'
  printf '  - a route handler was removed or its method check changed\n'
  printf '  - the upstream ArcGIS feed is down (zones/garages will 502 — check before rolling back)\n'
  printf '  - the worker just deployed and edge cache is stale; retry after 30s\n'
  exit 1
fi
