# C166SV2 / XC164CS Ghidra extension

This is a local fork of the MIT `keyhana/c166-ghidra-module`, extended with an
explicit Infineon XC164CS (`C166SV2`) language and fixes required by the
Delphi MT05.2 image.

## Language

| Item | Value |
|---|---|
| Language ID | `C166SV2:LE:16:xc164cs` |
| Processor | `Infineon XC164CS` |
| Variant | `XC164CS` |
| Endian / size | little / 16 bit |
| SLEIGH file | `data/languages/c166.sla` |
| Processor spec | `data/languages/xc164cs.pspec` |
| Compiler specs | `tasking`, `keil` |

The original C166/C167 languages are intentionally **not** registered in this
extension's `c166.ldefs`; keep the upstream module installed separately if you
need `C166:LE:16:default` or `C166:CS:LE:16:default`.

## Changes from the upstream module

1. **`EXTR #n` window corrected.**  The upstream SLEIGH cleared the extended
   SFR context one instruction too early.  The fork now applies the override to
   exactly the next *n* instructions, as specified by the C166 manual.
2. **C166SV2 MAC opcode `0x85`.**  The upstream module treats `0x85` as an
   undefined instruction.  The fork decodes it as a 2-byte `co_mac_unknown`
   no-effect instruction so the checksum epilogue (`0xC003B0`) continues
   correctly.  Full CoMUL/CoMAC semantics are not claimed.
3. **XC164CS processor spec.**  Adds the XC164CS language variant, firmware
   SFR/ESFR names from `firmware/SEMANTIC_INDEX.csv`, and the XC164
   TwinCAN/segment-E0 windows.
4. **Analyzer support.**  `C166AddressAnalyzer` now registers
   `Infineon XC164CS` so DPP/EXTP/EXTS constant propagation and decompiler
   references work with the new language.
5. **MT05.2 scripts.**  `CreateMT05Memory`, known-function seeding, far-call
   table discovery and full-listing export scripts are included.

## Build

Requires Ghidra 12.1.4 and a JDK compatible with it.  No network access is
needed.

```bash
cd emulator/ghidra/c166sv2
export GHIDRA_INSTALL_DIR=/opt/ghidra_12.1.4_PUBLIC
./build.sh
# builds build/C166SV2.zip and build/c166sv2.jar
```

The build compiles the SLEIGH language with `support/sleigh` and the Java
analyzer with `javac` against the local Ghidra jars.

## Install

```bash
cd emulator/ghidra/c166sv2
./install.sh                 # user Ghidra extensions directory
# or: GHIDRA_EXTENSION_DIR=/path/to/Extensions ./install.sh
```

The install script extracts `build/C166SV2.zip` under
`$HOME/.config/ghidra/ghidra_12.1.4_PUBLIC/Extensions/C166SV2` by default.

## Regression test

After installing the extension:

```bash
GHIDRA_INSTALL_DIR=/opt/ghidra_12.1.4_PUBLIC emulator/ghidra/tests/run.sh
```

The test imports a small fixture containing `EXTR #4` and the `0x85` sequence
and checks the four ESFR pushes, the following SFR push, and the two
`co_mac_unknown` instructions.

## Headless usage

```bash
GHIDRA=/opt/ghidra_12.1.4_PUBLIC
$GHIDRA/support/analyzeHeadless /tmp/mt05proj MT05 \
  -import dumps/mash_dump.bin \
  -loader BinaryLoader -loader-baseAddr 0xC00000 \
  -processor C166SV2:LE:16:xc164cs -cspec tasking \
  -scriptPath emulator/ghidra/c166sv2/ghidra_scripts \
  -preScript CreateMT05Memory.java \
  -postScript CreateKnownFunctions.java firmware/SEMANTIC_INDEX.csv \
  -postScript AddFarCallFunctions.java \
  -postScript DisassembleKnownCode.java /tmp/opencode/code_labels.txt \
  -postScript ExportFullListing.java /tmp/mt05-out \
  -postScript ExportAllDecompiled.java /tmp/mt05-out
```

## Verified behaviour

The extension was exercised with Ghidra 12.1.4 against
`dumps/mash_dump.bin` (SHA-256 `e413f54f...37d2`) and a seeded MT05.2
function set.  It decompiled 208 functions without decompiler failures.

Known-good listing examples:

| Address | Bytes | New listing | Previous module |
|---|---|---|---|
| `0xC002E2` | `E6E8137F` | `mov 0xf1d0,#0x7f13` | `mov 0xffd0,#0x7f13` |
| `0xC00346` | `E6605455` | `mov 0xf0c0,#0x5554` | `mov 0xfec0,#0x5554` |
| `0xC00A86` | `E6EE0000` | `mov 0xf1dc,#0x0` | `mov 0xffdc,#0x0` |
| `0xC00A8C` | `E6EA0000` | `mov 0xf1d4,#0x0` | `mov 0xffd4,#0x0` |
| `0xC00CB2` | `E62AE0FB` | `mov 0xf054,#0xfbe0` | `mov 0xfe54,#0xfbe0` |
| `0xC00CCC` | `E6BD5800` | `mov 0xf17a,#0x58` | `mov 0xff7a,#0x58` |
| `0xC003B0` | `857A` | `co_mac_unknown 0x7a` | undefined / gap |
| `0xC003B2` | `8585` | `co_mac_unknown 0x85` | undefined / gap |

Across 8,853 addresses common with the previous whole-image listing, all 103
differences were operand text only; byte lengths and mnemonics were identical.
The differences are the expected EXTR/ESFR corrections.

## Limitations

* Full C166SV2 MAC-unit instruction semantics (CoMUL/CoMAC/CoLOAD/CoSTORE) are
  not available; only the observed `0x85` bytes are decoded, as a no-op.
* The extension does not replace Ghidra's whole-program code/data discovery.
  The included MT05.2 scripts seed known code and can be extended.
* Default memory blocks come from the processor spec plus
  `CreateMT05Memory`; external RAM contents are unknown and remain zero.
* The original C166/C167 module must not be installed with a conflicting copy of
  the same language IDs.  This extension registers only
  `C166SV2:LE:16:xc164cs`.
