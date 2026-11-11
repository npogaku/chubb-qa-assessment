#!/usr/bin/env bash
# Start WireMock BFF stub server for frontend development
# Usage: ./scripts/start-wiremock.sh [port]
#
# This serves the BFF API stubs so frontend can develop without the real BFF service.
# Stubs are generated from the BFF OpenAPI spec via: npm run generate:stubs

set -euo pipefail

PORT="${1:-8081}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
STUBS_DIR="$PROJECT_DIR/mocks/bff-stubs"

if [ ! -d "$STUBS_DIR/mappings" ]; then
  echo "❌ No stubs found. Generate them first: npm run generate:stubs"
  exit 1
fi

echo "🚀 Starting WireMock BFF stub server on port $PORT"
echo "   Mappings: $STUBS_DIR/mappings/"
echo "   Responses: $STUBS_DIR/__files/"
echo "   Admin UI: http://localhost:$PORT/__admin"
echo ""

docker run --rm -p "$PORT:8080" \
  -v "$STUBS_DIR/mappings:/home/wiremock/mappings:ro" \
  -v "$STUBS_DIR/__files:/home/wiremock/__files:ro" \
  wiremock/wiremock:3.3.1 --verbose --global-response-templating
