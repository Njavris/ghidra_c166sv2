#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
GHIDRA="${GHIDRA_INSTALL_DIR:-/opt/ghidra_12.1.4_PUBLIC}"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
python3 - "$ROOT/tests/extr_mac.hex" "$TMP/extr_mac.bin" <<'PY'
import sys
open(sys.argv[2], "wb").write(bytes.fromhex(open(sys.argv[1]).read().strip()))
PY
mkdir -p "$TMP/proj"
"$GHIDRA/support/analyzeHeadless" "$TMP/proj" test \
  -import "$TMP/extr_mac.bin" \
  -loader BinaryLoader -loader-baseAddr 0xC00000 \
  -processor C166SV2:LE:16:xc164cs -cspec tasking \
  -scriptPath "$ROOT/c166sv2/ghidra_scripts" \
  -postScript DumpRange.java -noanalysis > "$TMP/log" 2>&1
for expected in \
  "C00002 push 0xf000" \
  "C00004 push 0xf002" \
  "C00006 push 0xf004" \
  "C00008 push 0xf006" \
  "C0000A push 0xfe08" \
  "C00012 co_mac_unknown 0x7a" \
  "C00014 co_mac_unknown 0x85" \
  "C00016 mov r4,r1" \
  "C00020 jmpa cc_NE,0xc002d6" \
  "C00024 jmps 0xc10000"; do
  if ! grep -Fq "$expected" "$TMP/log"; then
    echo "FAIL: expected: $expected" >&2
    grep "DumpRange.java>" "$TMP/log" >&2 || true
    exit 1
  fi
done
echo "PASS: EXTR #4 window and C166SV2 0x85 decoding"
