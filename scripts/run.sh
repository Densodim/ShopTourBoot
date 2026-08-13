#!/usr/bin/env bash
# Start Voyage API with Testcontainers (no local Postgres/Redis).
# Requires colima (or another Docker daemon) and the Gradle wrapper.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if ! command -v colima >/dev/null 2>&1; then
	echo "colima is not installed. Install it, or start Docker Desktop and set DOCKER_HOST yourself." >&2
	exit 1
fi

if ! colima status >/dev/null 2>&1; then
	echo "Starting colima…"
	colima start
fi

export DOCKER_HOST="unix://${HOME}/.colima/default/docker.sock"
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
export VOYAGE_JWT_SECRET="${VOYAGE_JWT_SECRET:-dev-only-change-me-to-a-32byte-secret!!}"

if [[ -z "${JAVA_HOME:-}" ]] && [[ -x /usr/libexec/java_home ]]; then
	JAVA_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null || /usr/libexec/java_home -v 22 2>/dev/null || /usr/libexec/java_home)"
	export JAVA_HOME
fi

echo "Voyage API → http://localhost:8080"
echo "  ping     GET /api/_ping"
echo "  health   GET /actuator/health"
echo "  swagger  http://localhost:8080/swagger-ui.html"
echo

exec ./gradlew bootTestRun "$@"
