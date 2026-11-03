# Ghidra C166SV2 / XC164CS extension

The extension source is in [`c166sv2/`](c166sv2/README.md).  It adds:

* language `C166SV2:LE:16:xc164cs`
* processor `Infineon XC164CS`
* corrected `EXTR #n` context windows
* explicit decoding of the observed C166SV2 `0x85` MAC opcode
* XC164 SFR/ESFR names and MT05.2 memory/function scripts

Build and install from `c166sv2/`:

```bash
export GHIDRA_INSTALL_DIR=/opt/ghidra_12.1.4_PUBLIC
./build.sh
./install.sh
```

Regression test after installation:

```bash
GHIDRA_INSTALL_DIR=/opt/ghidra_12.1.4_PUBLIC emulator/ghidra/tests/run.sh
```

The full build, verification results and limitations are documented in
[`c166sv2/README.md`](c166sv2/README.md) and
[`c166sv2/FINDINGS.md`](c166sv2/FINDINGS.md).

The existing artifacts under `../artifacts/ghidra-full/` were produced with the
upstream C167 module and are not modified.
