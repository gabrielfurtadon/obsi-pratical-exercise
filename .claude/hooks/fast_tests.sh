#!/usr/bin/env bash
# PostToolUse: after an edit under src/ or to pom.xml, run the fast tests (everything except
# @Tag("slow") and @Tag("wip"), the latter being tests written ahead of their code) and feed
# failures back to Claude. The tool has already run; this cannot undo it.
set -uo pipefail
ROOT="${CLAUDE_PROJECT_DIR:-$(pwd)}"
payload="$(cat)"
path="$(jq -r '.tool_input.file_path // .tool_input.notebook_path // empty' <<<"$payload")"
[[ -z "$path" ]] && exit 0
[[ "$path" != /* ]] && path="$(jq -r '.cwd // empty' <<<"$payload")/$path"
case "$path" in
  "$ROOT"/src/*|"$ROOT"/pom.xml) ;;
  *) exit 0 ;;
esac

cd "$ROOT" || exit 0
if output="$(./mvnw -q test -DexcludedGroups=slow,wip 2>&1)"; then
  exit 0
fi
jq -n --arg out "$(tail -n 60 <<<"$output")" \
  '{decision: "block", reason: ("Fast tests failed after this edit (./mvnw -q test -DexcludedGroups=slow,wip):\n" + $out)}'
exit 0
