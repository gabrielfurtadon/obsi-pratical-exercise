#!/usr/bin/env bash
# Build and run every test. Produces target/obsi.jar.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"
exec ./mvnw -q clean verify "$@"
