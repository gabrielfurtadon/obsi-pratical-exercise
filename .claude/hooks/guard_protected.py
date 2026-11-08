#!/usr/bin/env python3
"""PreToolUse guard: block writes to the spec and to src/test/resources/expected/.

Covers file tools (Edit, Write, MultiEdit, NotebookEdit) and Bash. For Bash, any command that
references a protected path (or an ancestor directory, or a glob that could match one) is only
allowed when every segment is a known read-only command and there is no output redirection. The
target of a `cd` does not count as a reference merely because it is an ancestor (so
`cd <root> && ./mvnw test` is fine); `cd` is followed, so later relative paths resolve against it.

This is a guardrail against accidents, not a security boundary: it does not interpret shell
variables, eval, scripts in other languages, or git commands that rewrite the work tree
without naming paths.
"""
import fnmatch
import json
import os
import re
import shlex
import sys

SPEC = "obsi-methodology.md"
EXPECTED = os.path.join("src", "test", "resources", "expected")

READ_ONLY = {
    "cat", "head", "tail", "less", "more", "grep", "egrep", "fgrep", "rg", "wc", "diff", "cmp",
    "ls", "stat", "file", "sha256sum", "sha1sum", "md5sum", "shasum", "jq", "tree", "du",
    "realpath", "readlink", "basename", "dirname", "echo", "printf", "true", "test", "[", "cd",
    "pwd", "sort", "uniq", "cut", "column", "find", "git", "hexdump", "xxd", "od",
}
GIT_READ_ONLY = {"diff", "log", "show", "status", "ls-files", "blame", "grep", "rev-parse", "cat-file"}
FIND_WRITES = {"-delete", "-exec", "-execdir", "-ok", "-okdir", "-fprint", "-fprint0", "-fprintf", "-fls"}
WRAPPERS = {"sudo", "env", "command", "time", "nice", "nohup", "builtin"}
SEGMENT_SPLIT = re.compile(r"\|\||&&|[;|&\n()`]|\$\(")
REDIRECT = re.compile(r"(?:\d*|&)>>?\|?\s*(\S*)")


def deny(reason):
    print(json.dumps({"hookSpecificOutput": {
        "hookEventName": "PreToolUse",
        "permissionDecision": "deny",
        "permissionDecisionReason": reason,
    }}))
    sys.exit(0)


def protected_paths(root):
    return os.path.realpath(os.path.join(root, SPEC)), os.path.realpath(os.path.join(root, EXPECTED))


def is_protected(path, root):
    spec, expected = protected_paths(root)
    real = os.path.realpath(path)
    return real == spec or real == expected or real.startswith(expected + os.sep)


def is_ancestor_of_protected(path, root):
    real = os.path.realpath(path)
    return any(p.startswith(real.rstrip(os.sep) + os.sep) for p in protected_paths(root))


def check_file_tool(tool_input, cwd, root):
    path = tool_input.get("file_path") or tool_input.get("notebook_path")
    if not path:
        return
    full = path if os.path.isabs(path) else os.path.join(cwd, path)
    if is_protected(full, root):
        deny(f"{path} is protected: the spec and src/test/resources/expected/ must never be "
             "edited (see AGENTS.md). Log the issue in docs/decisions.md instead.")


def words(text):
    try:
        return shlex.split(text, comments=False, posix=True)
    except ValueError:
        return text.split()


def cd_target(argv):
    """The directory a `cd` segment changes to, or None if this segment is not a plain cd."""
    if not argv or os.path.basename(argv[0]) != "cd":
        return None
    return next((a for a in argv[1:] if not a.startswith("-")), "")


def references_protected(command, cwd, root):
    if "obsi-methodology" in command or "expected/" in command:
        return True
    rel_forms = [SPEC, EXPECTED, "expected", "resources/expected", "test/resources/expected"]
    # Walk the command one segment at a time so a `cd` is followed: later relative paths resolve
    # against the directory it moved to. A cd target is only a reference when it is a protected path
    # itself. Being an ancestor of one (such as the project root in `cd <root> && ./mvnw test`) is not,
    # because cd alone changes nothing. Any other token that is an ancestor still counts, so
    # `rm -rf <root>` stays denied.
    here = cwd
    for segment in SEGMENT_SPLIT.split(re.sub(r"\d*[<>]&\d+|&>>?", " ", command)):
        argv = words(segment.strip())
        target = cd_target(argv)
        for raw in argv:
            is_cd_target = target is not None and raw == target
            for tok in {raw, raw.lstrip("<>&|0123456789"), raw.split("=", 1)[-1]}:
                if not tok:
                    continue
                if os.path.basename(tok.rstrip("/")) in ("expected", SPEC):
                    return True
                if any(c in tok for c in "*?[") and any(fnmatch.fnmatch(f, tok) for f in rel_forms):
                    return True
                full = tok if os.path.isabs(tok) else os.path.join(here, tok)
                if os.path.exists(full) and (
                        is_protected(full, root)
                        or (is_ancestor_of_protected(full, root) and not is_cd_target)):
                    return True
        if target:
            moved = target if os.path.isabs(target) else os.path.join(here, target)
            if os.path.isdir(moved):
                here = moved
    return False


def segment_is_read_only(segment):
    argv = words(segment)
    while argv and (re.fullmatch(r"[A-Za-z_][A-Za-z0-9_]*=.*", argv[0]) or argv[0] in WRAPPERS):
        argv = argv[1:]
    if not argv:
        return True
    prog = os.path.basename(argv[0])
    if prog not in READ_ONLY:
        return False
    if prog == "git":
        sub = next((a for a in argv[1:] if not a.startswith("-")), None)
        return sub in GIT_READ_ONLY
    if prog == "find":
        return not any(a in FIND_WRITES for a in argv[1:])
    return True


def check_bash(command, cwd, root):
    if not references_protected(command, cwd, root):
        return
    for target in REDIRECT.findall(command):
        if target not in ("/dev/null",) and not re.fullmatch(r"&\d|\d", target or "x"):
            deny("Bash command references the spec or src/test/resources/expected/ and contains "
                 "an output redirection. Those files must never be modified (see AGENTS.md).")
    if ">(" in command:
        deny("Process substitution in a command touching protected paths is not allowed.")
    # Drop fd duplications (2>&1, >&2) and the redirect-both operator (&>, already checked above)
    # so their "&" is not taken for a command separator.
    unsplittable = re.sub(r"\d*[<>]&\d+|&>>?", " ", command)
    for segment in SEGMENT_SPLIT.split(unsplittable):
        if not segment_is_read_only(segment.strip()):
            deny("Bash command references the spec or src/test/resources/expected/ and runs "
                 f"something that is not on the read-only allowlist: {segment.strip()!r}. "
                 "Those files must never be modified (see AGENTS.md).")


def main():
    try:
        payload = json.load(sys.stdin)
    except Exception as e:  # unreadable payload: fail closed
        deny(f"guard_protected.py could not parse the hook payload: {e}")
    root = os.environ.get("CLAUDE_PROJECT_DIR") or payload.get("cwd") or os.getcwd()
    cwd = payload.get("cwd") or root
    tool = payload.get("tool_name", "")
    tool_input = payload.get("tool_input") or {}
    if tool in ("Edit", "Write", "MultiEdit", "NotebookEdit"):
        check_file_tool(tool_input, cwd, root)
    elif tool == "Bash":
        check_bash(tool_input.get("command", ""), cwd, root)
    sys.exit(0)


if __name__ == "__main__":
    main()
