#!/usr/bin/env python3
"""Stop / SessionEnd: copy this session's transcript (and its subagent transcripts) into
docs/ai-sessions/claude-code/<session_id>/. Idempotent: each run overwrites the previous copy.
Never blocks; errors are reported on stderr only."""
import json
import os
import shutil
import sys


def main():
    try:
        payload = json.load(sys.stdin)
        src = payload.get("transcript_path")
        session = payload.get("session_id")
        if not src or not session or not os.path.isfile(src):
            return
        root = os.environ.get("CLAUDE_PROJECT_DIR") or payload.get("cwd") or os.getcwd()
        dest = os.path.join(root, "docs", "ai-sessions", "claude-code", session)
        os.makedirs(dest, exist_ok=True)
        shutil.copyfile(src, os.path.join(dest, "transcript.jsonl"))
        subagents = os.path.join(os.path.splitext(src)[0], "subagents")
        if os.path.isdir(subagents):
            out = os.path.join(dest, "subagents")
            os.makedirs(out, exist_ok=True)
            for name in os.listdir(subagents):
                if name.endswith((".jsonl", ".meta.json")):
                    shutil.copyfile(os.path.join(subagents, name), os.path.join(out, name))
    except Exception as e:
        print(f"save_transcript.py: {e}", file=sys.stderr)


if __name__ == "__main__":
    main()
    sys.exit(0)
