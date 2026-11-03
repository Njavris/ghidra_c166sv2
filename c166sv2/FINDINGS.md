# C166SV2 extension — validation findings

## 1. Build and import

Built offline against `/opt/ghidra_12.1.4_PUBLIC`:

```bash
cd emulator/ghidra/c166sv2
GHIDRA_INSTALL_DIR=/opt/ghidra_12.1.4_PUBLIC ./build.sh
```

The SLEIGH compiler produced `data/languages/c166.sla` and `javac` produced
`build/c166sv2.jar`.  Headless import selected:

```
Using Language/Compiler: C166SV2:LE:16:xc164cs:tasking
```

The test project was created from `dumps/mash_dump.bin` at loader base
`0xC00000`.  A seeded run using `CreateKnownFunctions`,
`AddFarCallFunctions` and `DisassembleKnownCode` produced 8,878 instruction
rows and 208 decompiled functions with `decompiled_ok=208`.

## 2. EXTR fix

The upstream module cleared the extended context one instruction too early.
The fork leaves the context active through the full `#irang2` count and clears
it at the next instruction boundary.

Test binary:

```
D1B0            extr #4
EC00 EC01 EC02 EC03   push reg 0..3
EC04 EC05            push reg 4..5
DB00            rets
```

Expected with EXTR #4: first four pushes use ESFR (F000/F002/F004/F006), the
fifth uses SFR (FE08).  The new module produces exactly that:

```
c00002 push 0xf000
c00004 push 0xf002
c00006 push 0xf004
c00008 push 0xf006
c0000a push 0xfe08
c0000c push 0xfe0a
```

The upstream module produced `F000/F002/F004/FE06`.

Real-image examples:

| Address | New | Upstream | Meaning |
|---|---|---|---|
| `0xC002E2` | `mov 0xf1d0,#0x7f13` | `mov 0xffd0,#0x7f13` | ESFR after `EXTR #1` |
| `0xC00346` | `mov 0xf0c0,#0x5554` | `mov 0xfec0,#0x5554` | second of `EXTR #2` |
| `0xC00A86` | `mov 0xf1dc,#0x0` | `mov 0xffdc,#0x0` | SYSCON1 |
| `0xC00A8C` | `mov 0xf1d4,#0x0` | `mov 0xffd4,#0x0` | SYSCON3 |
| `0xC00CB2` | `mov 0xf054,#0xfbe0` | `mov 0xfe54,#0xfbe0` | second of `EXTR #2` (T7REL) |
| `0xC00CCC` | `mov 0xf17a,#0x58` | `mov 0xff7a,#0x58` | third of `EXTR #3` (T7IC) |
| `0xC006C8` | `push 0xf006` | `push 0xfe06` | fourth of `EXTR #4` |
| `0xC0075E` | `pop 0xf000` | `pop 0xfe00` | fourth of `EXTR #4` |

`T7REL` at `0xF054` and the symmetric ISR save/restore pair are independent
sanity checks that the new window is correct.

## 3. C166SV2 opcode 0x85

The C166 manual marks `0x85` undefined.  The local image contains one
4-byte sequence at `0xC003B0`:

```
85 7A 85 85 F0 41 DB 00
```

`F0 41` is `mov r4,r1`; `DB 00` is `RETS`.  The checksum result is therefore
returned in R4.  The fork decodes the two `0x85` words as 2-byte
`co_mac_unknown` instructions.  The new listing is:

```
c003b0  co_mac_unknown 0x7a
c003b2  co_mac_unknown 0x85
c003b4  mov r4,r1
c003b6  rets
```

The function size grows from 50 to 58 bytes, and the checksum path is no
longer reported as bad instruction data.  The exact CoMUL/CoMAC semantics are
not known and are not claimed.

## 4. Comparison with the previous whole-image listing

A pre-JMPA-sync run compared the new seeded listing to
`artifacts/ghidra-full/disassembly.txt` on 8,853 common addresses:

* 103 differing rows
* 103 had identical bytes and mnemonic text except for the operand address
* 0 instruction-length changes
* 0 mnemonic changes

Those differences are the corrected EXTR/ESFR operands listed above.  The run
also showed 133 old-only `JMPA`/`CALLA` rows because it used the older SLEIGH
copy that still required a zero reserved nibble.  The fork now includes the
upstream `JMPA`/`CALLA` encoding fix, so a post-sync listing restores those
instructions.  The final post-sync comparison should be regenerated with the
included scripts; the pre-sync numbers above are retained only as the
EXTR/ESFR delta measurement.

## 5. Decompiler

`ExportAllDecompiled` succeeded for all 208 functions in the seeded project.
Relevant spot checks:

* `boot_reset` now shows `SYSCON1`/`SYSCON3` after `EXTR #1`.
* `checksum16_be` now contains the two `co_mac_unknown` instructions and
  `mov r4,r1; rets`.
* `calc_limit2` decompiles to the same state/limit structure used by the
  emulator analysis, including `C9D0`, `C0781E`, `C07832`, `E008A2` and
  `E008B0`.

## 6. Remaining unknowns

1. Full C166SV2 MAC-unit encodings and semantics.
2. Code/data discovery beyond the seeded MT05.2 scripts.
3. Correct external RAM contents; the image does not contain them.
4. Whether the XC164CS pspec's remaining C167-sourced SFR aliases should be
   renamed or removed.
5. Decompiler calling-convention selection for every Tasking/Keil function;
   both specs are registered but are not auto-classified per function.

## 7. Reproducible commands

```bash
# Build
cd emulator/ghidra/c166sv2
GHIDRA_INSTALL_DIR=/opt/ghidra_12.1.4_PUBLIC ./build.sh

# Install
GHIDRA_INSTALL_DIR=/opt/ghidra_12.1.4_PUBLIC ./install.sh

# Full seeded analysis
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
