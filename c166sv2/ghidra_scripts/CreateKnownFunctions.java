// Create and name every function from SEMANTIC_INDEX.csv plus C166 vector targets.
//@category C166
import java.io.*;
import java.util.*;
import ghidra.app.script.GhidraScript;
import ghidra.program.model.address.*;
import ghidra.program.model.listing.*;
import ghidra.program.model.symbol.SourceType;

public class CreateKnownFunctions extends GhidraScript {
    private int make(String hex, String name) {
        try {
            long v = Long.parseLong(hex.replace("0x", ""), 16);
            Address a = toAddr(v);
            if (a == null) return 0;
            if (getFunctionAt(a) == null) {
                disassemble(a);
                Function f = createFunction(a, name);
                if (f == null) return 0;
            }
            Function f = getFunctionAt(a);
            if (f != null && name != null && !name.isEmpty()) {
                try { f.setName(name, SourceType.USER_DEFINED); } catch (Exception e) {}
            }
            return 1;
        } catch (Exception e) {
            println("FAIL " + hex + " " + name + ": " + e);
            return 0;
        }
    }
    @Override
    public void run() throws Exception {
        String[] args = getScriptArgs();
        String csv = args.length > 0 ? args[0] : "/home/user/mash/firmware/SEMANTIC_INDEX.csv";
        int made = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(csv))) {
            String line;
            boolean header = true;
            while ((line = br.readLine()) != null) {
                if (header) { header = false; continue; }
                String[] c = line.split(",", -1);
                if (c.length < 4) continue;
                if (!c[0].equals("function")) continue;
                made += make(c[2], c[3]);
            }
        }
        // C166 vector table at the image base: 4-byte far jumps.
        Address base = currentProgram.getMinAddress();
        for (long off = 0; off < 0x200; off += 4) {
            Address va = base.add(off);
            try {
                byte[] b = new byte[4];
                currentProgram.getMemory().getBytes(va, b);
                int op = b[0] & 0xff;
                if (op == 0xFA || op == 0xEA) {
                    int seg = b[1] & 0xff;
                    int lo = b[2] & 0xff, hi = b[3] & 0xff;
                    long target = ((long)seg << 16) | (hi << 8) | lo;
                    if ((target & 0xFFFF) != 0 && (target >> 16) == 0xC0) {
                        made += make(Long.toHexString(target), "vector_" + Long.toHexString(off));
                    }
                }
            } catch (Exception e) {}
        }
        println("CreateKnownFunctions made/named=" + made + " total=" + currentProgram.getFunctionManager().getFunctionCount());
    }
}
