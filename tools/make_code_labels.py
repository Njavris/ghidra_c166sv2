#!/usr/bin/env python3
"""Extract instruction addresses from a Ghidra listing for CreateKnownFunctions seeding."""
import re
import sys


def main() -> int:
    if len(sys.argv) not in (2, 3):
        print("usage: make_code_labels.py INPUT_LISTING [OUTPUT_LABELS]", file=sys.stderr)
        return 2
    out = []
    with open(sys.argv[1], encoding="utf-8", errors="replace") as fh:
        for line in fh:
            match = re.match(r"^([0-9A-Fa-f]{6})\s+[0-9A-Fa-f]+", line)
            if match:
                out.append(match.group(1).lower())
    text = "\n".join(out) + ("\n" if out else "")
    if len(sys.argv) == 3:
        open(sys.argv[2], "w", encoding="utf-8").write(text)
    else:
        sys.stdout.write(text)
    print(f"wrote {len(out)} labels", file=sys.stderr)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
