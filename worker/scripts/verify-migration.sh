#!/usr/bin/env bash
# Post-migration smoke query for the rosaparks D1 database.
#
# Runs after `wrangler d1 migrations apply` and exercises the expected schema
# plus a representative existing query. Read-only by design — never DML,
# never DDL. Fails (exit 1) if any check returns an unexpected shape or value.
#
# Usage:
#   ./worker/scripts/verify-migration.sh             # --remote (production D1)
#   ./worker/scripts/verify-migration.sh --local     # local Miniflare D1
#
# Added per claude-config#21 (per-stack migration verification gates). Sibling
# to verify-deploy.sh (Worker post-deploy gate, rosa-parks#93).

set -euo pipefail

DB_NAME="rosaparks-beta-signups"
TARGET="--remote"

if [[ "${1:-}" == "--local" ]]; then
  TARGET="--local"
elif [[ "${1:-}" == "--remote" || -z "${1:-}" ]]; then
  TARGET="--remote"
else
  echo "Unknown argument: ${1}" >&2
  echo "Usage: $0 [--remote | --local]" >&2
  exit 2
fi

if ! command -v jq >/dev/null 2>&1; then
  echo "Missing 'jq' (required to parse wrangler --json output)." >&2
  echo "Install: 'sudo dnf install jq' on Fedora, 'brew install jq' on macOS." >&2
  exit 2
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WORKER_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$WORKER_DIR"

# Run a read-only SELECT and emit the parsed `results` array as a single JSON line.
d1_query() {
  local sql="$1"
  npx --no-install wrangler d1 execute "$DB_NAME" "$TARGET" --json --command "$sql" \
    | jq -c '.[0].results'
}

FAILED=0
PASSED=0

# check <label> <sql> <jq_filter> <expected>
check() {
  local label="$1"
  local sql="$2"
  local jq_filter="$3"
  local expected="$4"

  local result
  if ! result=$(d1_query "$sql"); then
    printf '  FAIL  %-44s wrangler d1 execute failed\n' "$label"
    FAILED=$((FAILED + 1))
    return
  fi

  local actual
  actual=$(printf '%s' "$result" | jq -r "$jq_filter")
  if [[ "$actual" == "$expected" ]]; then
    printf '  PASS  %-44s → %s\n' "$label" "$actual"
    PASSED=$((PASSED + 1))
  else
    printf '  FAIL  %-44s → %s  (expected %s)\n' "$label" "$actual" "$expected"
    FAILED=$((FAILED + 1))
  fi
}

printf 'Verifying D1 schema for %s (%s)\n\n' "$DB_NAME" "${TARGET#--}"

# --- 0001_beta_signups ----------------------------------------------------
check "beta_signups table exists" \
  "SELECT name FROM sqlite_master WHERE type='table' AND name='beta_signups'" \
  '.[0].name // ""' \
  "beta_signups"

check "beta_signups column count" \
  "SELECT COUNT(*) AS n FROM pragma_table_info('beta_signups')" \
  '.[0].n' \
  "5"

check "idx_beta_signups_created_at exists" \
  "SELECT name FROM sqlite_master WHERE type='index' AND name='idx_beta_signups_created_at'" \
  '.[0].name // ""' \
  "idx_beta_signups_created_at"

# Mirrors handleBetaSignupCount() at worker/src/index.ts:463 — pure shape check.
check "beta_signups COUNT(*) shape" \
  "SELECT COUNT(*) AS total FROM beta_signups" \
  '.[0] | has("total") | tostring' \
  "true"

# --- 0002_parking_sessions ------------------------------------------------
check "parking_sessions table exists" \
  "SELECT name FROM sqlite_master WHERE type='table' AND name='parking_sessions'" \
  '.[0].name // ""' \
  "parking_sessions"

check "parking_sessions column count" \
  "SELECT COUNT(*) AS n FROM pragma_table_info('parking_sessions')" \
  '.[0].n' \
  "7"

# Partial index — load-bearing for the hot "active session by plate" lookup at
# worker/src/index.ts:283. Losing it would silently turn /api/parking/start
# into a full-table scan; the index is exactly the thing a regression would drop.
check "idx_parking_sessions_plate_active exists" \
  "SELECT name FROM sqlite_master WHERE type='index' AND name='idx_parking_sessions_plate_active'" \
  '.[0].name // ""' \
  "idx_parking_sessions_plate_active"

# Mirrors the active-session lookup at worker/src/index.ts:283. Sentinel plate
# guarantees zero matches; the check is that the query parses and returns a
# well-formed result against the live schema.
check "active-session lookup query shape" \
  "SELECT id FROM parking_sessions WHERE plate = '__VERIFY_NONE__' AND end_millis IS NULL LIMIT 1" \
  'length | tostring' \
  "0"

printf '\n%d passed, %d failed\n' "$PASSED" "$FAILED"

if [[ $FAILED -gt 0 ]]; then
  printf '\nVerification failed. Likely causes:\n'
  printf '  - The migration applied to a different DB than expected (check wrangler.jsonc d1_databases)\n'
  printf '  - A new migration dropped/renamed an expected table/column/index without updating this script\n'
  printf '  - The checks drifted from the schema after a change — update both sides together\n'
  printf '  - Running --remote against an environment that has not been migrated yet\n'
  exit 1
fi
