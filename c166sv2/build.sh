#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
GHIDRA="${GHIDRA_INSTALL_DIR:-/opt/ghidra_12.1.4_PUBLIC}"
cd "$ROOT"
rm -rf build
mkdir -p build/classes build/stage/C166SV2
CP="$(find "$GHIDRA/Ghidra" -name '*.jar' -printf '%p:' | sed 's/:$//')"
javac -cp "$CP" -d build/classes $(find src/main/java -name '*.java')
jar cf build/c166sv2.jar -C build/classes .
"$GHIDRA/support/sleigh" -i data/sleighArgs.txt -a data/languages
cp extension.properties Module.manifest README.md FINDINGS.md LICENSE build/stage/C166SV2/ 2>/dev/null || true
mkdir -p build/stage/C166SV2/data build/stage/C166SV2/lib build/stage/C166SV2/ghidra_scripts
cp -r data/languages data/manuals build/stage/C166SV2/data/
rm -f build/stage/C166SV2/data/languages/*.orig build/stage/C166SV2/data/languages/*.bak
cp build/c166sv2.jar build/stage/C166SV2/lib/
cp ghidra_scripts/*.java build/stage/C166SV2/ghidra_scripts/
find build/stage/C166SV2 -name '__pycache__' -type d -prune -exec rm -rf {} + 2>/dev/null || true
python3 - <<'PY'
import zipfile
from pathlib import Path
root=Path("build/stage")
out=Path("build/C166SV2.zip")
with zipfile.ZipFile(out,"w",zipfile.ZIP_DEFLATED) as z:
    for f in sorted((root/"C166SV2").rglob("*")):
        z.write(f, f.relative_to(root))
print("zip",out)
PY
echo "built $ROOT/build/C166SV2.zip"
