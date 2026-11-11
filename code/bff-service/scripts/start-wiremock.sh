#!/usr/bin/env bash
# Start WireMock Claims Service stub server for BFF development
# Usage: ./scripts/start-wiremock.sh [port]
#
# This serves the Claims Service API stubs so BFF can develop without the real Claims Service.
# Stubs are generated from the Claims Service OpenAPI spec via:
#   mvn generate-test-resources -DskipWiremockGeneration=false

set -euo pipefail

PORT="${1:-8082}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
STUBS_DIR="$PROJECT_DIR/mocks/claims-service-stubs"

if [ ! -d "$STUBS_DIR/mappings" ]; then
  echo "❌ No stubs found. Generate them first:"
  echo "   mvn generate-test-resources -DskipWiremockGeneration=false"
  exit 1
fi

echo "🚀 Starting WireMock Claims Service stub server on port $PORT"
echo "   Mappings: $STUBS_DIR/mappings/"
echo "   Responses: $STUBS_DIR/__files/"
echo "   Admin UI: http://localhost:$PORT/__admin"
echo ""

docker run --rm -p "$PORT:8080" \
  -v "$STUBS_DIR/mappings:/home/wiremock/mappings:ro" \
  -v "$STUBS_DIR/__files:/home/wiremock/__files:ro" \
  wiremock/wiremock:3.3.1 --verbose --global-response-templating
