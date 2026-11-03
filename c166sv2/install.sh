#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
GHIDRA="${GHIDRA_INSTALL_DIR:-/opt/ghidra_12.1.4_PUBLIC}"
VERSION="$(grep '^application.version=' "$GHIDRA/Ghidra/application.properties" | cut -d= -f2)"
RELEASE="$(grep '^application.release.name=' "$GHIDRA/Ghidra/application.properties" | cut -d= -f2)"
if [ -n "$RELEASE" ]; then
  CONFIG_NAME="ghidra_${VERSION}_${RELEASE}"
else
  CONFIG_NAME="ghidra_${VERSION}"
fi
DEST="${GHIDRA_EXTENSION_DIR:-$HOME/.config/ghidra/${CONFIG_NAME}/Extensions}"
if [ ! -f "$ROOT/build/C166SV2.zip" ]; then
  "$ROOT/build.sh"
fi
mkdir -p "$DEST"
if [ -e "$DEST/C166SV2" ]; then
  if [ "${1:-}" != "--force" ]; then
    echo "refusing to overwrite $DEST/C166SV2 (pass --force)" >&2
    exit 1
  fi
  rm -rf "$DEST/C166SV2"
fi
python3 - "$ROOT/build/C166SV2.zip" "$DEST" <<'PY'
import sys, zipfile
zipfile.ZipFile(sys.argv[1]).extractall(sys.argv[2])
print("installed to", sys.argv[2])
PY
