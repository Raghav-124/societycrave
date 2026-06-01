#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PORT="${PORT:-8080}"

cd "$ROOT_DIR"

if command -v lsof >/dev/null 2>&1 && lsof -iTCP:"${PORT}" -sTCP:LISTEN >/dev/null 2>&1; then
  echo "Port ${PORT} is already in use."
  echo
  echo "Inspect the listener with:"
  echo "  lsof -i :${PORT}"
  echo
  echo "Then stop it explicitly if needed, for example:"
  echo "  kill -9 <PID>"
  echo
  echo "Or run SocietyCrave on another port, for example:"
  echo "  PORT=8081 ./scripts/dev-up.sh"
  exit 1
fi

echo "Starting SocietyCrave on http://localhost:${PORT}"
echo "Also available at http://127.0.0.1:${PORT}"
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=${PORT}"
