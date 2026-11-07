#!/usr/bin/env bash
# Stop: run the full suite except @Tag("wip") before Claude finishes a turn. If it fails, block
# the stop so Claude fixes it. Tests tagged wip (written before their code) never block.
# Loop guard: stop_hook_active is true when Claude is already continuing because of this hook,
# so a second failure lets the turn end instead of looping (Claude Code also caps consecutive
# blocks at 8).
set -uo pipefail
ROOT="${CLAUDE_PROJECT_DIR:-$(pwd)}"
payload="$(cat)"
[[ "$(jq -r '.stop_hook_active // false' <<<"$payload")" == "true" ]] && exit 0

cd "$ROOT" || exit 0
STAMP="target/.stop-hook-green"
# Skip when nothing the suite depends on has changed since the last green run.
if [[ -f "$STAMP" ]] && [[ -z "$(find src pom.xml README.md proposal.md obsi-methodology.md \
      -newer "$STAMP" -print -quit 2>/dev/null)" ]]; then
  exit 0
fi

if output="$(./mvnw -q verify -DexcludedGroups=wip 2>&1)"; then
  mkdir -p target && touch "$STAMP"
  exit 0
fi
jq -n --arg out "$(tail -n 80 <<<"$output")" \
  '{decision: "block", reason: ("Test suite failed (./mvnw -q verify -DexcludedGroups=wip). Fix it, or tag a test written ahead of its code with @Tag(\"wip\"):\n" + $out)}'
exit 0
