#!/usr/bin/env bash
# Self-test for guard_protected.py: feeds PreToolUse payloads and checks allow/deny.
# Usage: .claude/hooks/test_guard.sh
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
GUARD="$ROOT/.claude/hooks/guard_protected.py"
export CLAUDE_PROJECT_DIR="$ROOT"
fail=0

run() { # expected(allow|deny) tool field value [cwd]
  local want="$1" tool="$2" field="$3" value="$4" cwd="${5:-$ROOT}" out got
  out="$(jq -n --arg t "$tool" --arg f "$field" --arg v "$value" --arg c "$cwd" \
          '{hook_event_name:"PreToolUse", cwd:$c, tool_name:$t, tool_input:{($f):$v}}' \
        | python3 "$GUARD")"
  if grep -q '"permissionDecision": "deny"' <<<"$out"; then got=deny; else got=allow; fi
  if [[ "$got" == "$want" ]]; then
    printf 'ok    %-5s %-6s %s\n' "$want" "$tool" "$value"
  else
    printf 'FAIL  want %s got %s: %s %s\n' "$want" "$got" "$tool" "$value"; fail=1
  fi
}

E=src/test/resources/expected
# File tools
run deny  Write        file_path     "$ROOT/$E/probe.json"
run deny  Edit         file_path     "$ROOT/obsi-methodology.md"
run deny  Edit         file_path     "$E/.gitkeep"
run deny  Edit         file_path     "expected/x.json"                "$ROOT/src/test/resources"
run deny  NotebookEdit notebook_path "$ROOT/$E/x.ipynb"
run deny  Write        file_path     "$ROOT/src/../$E/x.json"
run allow Write        file_path     "$ROOT/src/main/java/X.java"
run allow Edit         file_path     "$ROOT/docs/decisions.md"
run allow Write        file_path     "$ROOT/src/test/resources/expected-notes.md"
# Bash: blocked
run deny  Bash command "echo x > $E/probe.json"
run deny  Bash command "echo x >> obsi-methodology.md"
run deny  Bash command "sed -i s/5%/6%/ obsi-methodology.md"
run deny  Bash command "cp /tmp/a.json $E/a.json"
run deny  Bash command "mv $E/a.json /tmp/"
run deny  Bash command "echo x | tee $E/a.json"
run deny  Bash command "rm -rf $E"
run deny  Bash command "truncate -s0 obsi-methodology.md"
run deny  Bash command "python3 -c 'open(\"obsi-methodology.md\",\"w\")'"
run deny  Bash command "perl -pi -e s/a/b/ obsi-methodology.md"
run deny  Bash command "git checkout -- obsi-methodology.md"
run deny  Bash command "cd src/test/resources && rm -r expected"
run deny  Bash command "rm obsi-meth*.md"
run deny  Bash command "rm -rf src/test"
run deny  Bash command "find . -name '*.json' -delete"
run deny  Bash command "cat obsi-methodology.md > /tmp/copy.md"
# Bash: allowed
run allow Bash command "cat obsi-methodology.md"
run allow Bash command "sha256sum obsi-methodology.md"
run allow Bash command "ls -la $E 2>/dev/null"
run allow Bash command "grep -n PED obsi-methodology.md | head -5"
run allow Bash command "git diff -- $E"
run allow Bash command "./mvnw -q test -DexcludedGroups=wip"
run allow Bash command "git commit -m 'update expected values'"
run allow Bash command "echo hi > /tmp/x.txt"
run allow Bash command "sha256sum obsi-methodology.md && git log --oneline 2>&1 | head -1"
run deny  Bash command "cat x &> obsi-methodology.md"
run deny  Bash command "ls $E 2>&1 && rm $E/a.json"

exit "$fail"
