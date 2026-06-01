#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"

pass() {
  echo "PASS: $1"
}

fail() {
  echo "FAIL: $1"
  exit 1
}

check_contains() {
  local path="$1"
  local expected="$2"
  local label="$3"
  local body

  body="$(curl -fsS "${BASE_URL}${path}")" || fail "${label} (${path})"
  if grep -Fq "$expected" <<<"$body"; then
    pass "${label}"
  else
    fail "${label} missing expected content: ${expected}"
  fi
}

check_health() {
  local body

  body="$(curl -fsS "${BASE_URL}/api/health")" || fail "Health endpoint"
  if grep -Fq '"status":"UP"' <<<"$body"; then
    pass "Health endpoint"
  else
    fail "Health endpoint did not report UP"
  fi
}

echo "Smoke testing ${BASE_URL}"

check_contains "/" "SocietyCrave" "Landing page"
check_contains "/index.html" "SocietyCrave" "Index HTML"
check_contains "/app.js" "const api =" "Static app.js"
check_contains "/styles.css" ":root" "Static styles.css"
check_health

echo "Smoke HTTP check completed successfully."
