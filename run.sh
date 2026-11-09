#!/usr/bin/env bash
# Run the OBSI calculator. Needs only a JRE (25+) and target/obsi.jar; no network, no Maven.
# Usage: ./run.sh [--new-ped] <input.json>
set -euo pipefail
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR="$DIR/target/obsi.jar"
if [[ ! -f "$JAR" ]]; then
  # Same failure shape as the calculator (docs/brief.md, "Output").
  echo '{"index":"OBSI","calculationTime":null,"status":"failed","reason":"target/obsi.jar not found; run ./build.sh first"}' >&2
  exit 3 # D-08: 3 is "unexpected / environment", not 2 (input or usage)
fi
exec java -jar "$JAR" "$@"
