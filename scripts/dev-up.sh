#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PORT="${PORT:-8080}"

cd "$ROOT_DIR"

if command -v lsof >/dev/null 2>&1 && lsof -iTCP:"${PORT}" -sTCP:LISTEN >/dev/null 2>&1; then
  echo "Port ${PORT} is already in use."
  echo "Stop the existing process or run with a different port, for example:"
  echo "  PORT=8081 ./scripts/dev-up.sh"
  exit 1
fi

echo "Starting SocietyCrave on http://localhost:${PORT}"
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=${PORT}"
