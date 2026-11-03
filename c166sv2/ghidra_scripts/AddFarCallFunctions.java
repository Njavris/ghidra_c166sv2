// Disassemble the 0xC10000 far-call trampoline table and create its targets.
//@category C166
import ghidra.app.script.GhidraScript;
import ghidra.program.model.address.*;
import ghidra.program.model.listing.*;
import ghidra.program.model.mem.*;

public class AddFarCallFunctions extends GhidraScript {
    @Override
    public void run() throws Exception {
        Address table = toAddr(0xC10000L);
        Memory mem = currentProgram.getMemory();
        int entries = 128, entriesMade = 0, funcs = 0;
        long tableStart = table.getOffset();
        long tableEnd = tableStart + entries * 4;
        for (int i = 0; i < entries; i++) {
            Address ea = table.add(i * 4L);
            byte[] b = new byte[4];
            try { mem.getBytes(ea, b); } catch (Exception e) { continue; }
            if (getInstructionAt(ea) == null) {
                try { if (disassemble(ea)) entriesMade++; } catch (Exception e) {}
            }
            int op = b[0] & 0xff;
            int seg = b[1] & 0xff;
            int lo = b[2] & 0xff, hi = b[3] & 0xff;
            if (op != 0xFA && op != 0xEA) continue;
            long target = ((long)seg << 16) | (hi << 8) | lo;
            if (seg != 0xC1) continue;
            if (target >= tableStart && target < tableEnd) continue;
            Address ta = toAddr(target);
            if (ta == null || !mem.contains(ta)) continue;
            if (getInstructionAt(ta) == null) {
                try { disassemble(ta); } catch (Exception e) {}
            }
            if (getFunctionAt(ta) == null) {
                try {
                    Function f = createFunction(ta, String.format("farcall_%02d", i));
                    if (f != null) funcs++;
                } catch (Exception e) { println("target fail " + ta + ": " + e); }
            }
        }
        println("farcall table: entries_disassembled=" + entriesMade + " functions_created=" + funcs +
                " total_functions=" + currentProgram.getFunctionManager().getFunctionCount());
    }
}
