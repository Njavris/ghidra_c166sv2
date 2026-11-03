// Dump two small instruction windows for the extension regression test.
//@category C166
import ghidra.app.script.GhidraScript;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.Instruction;

public class DumpRange extends GhidraScript {
    @Override
    public void run() throws Exception {
        long[] starts = new long[]{0xC00000L, 0xC00010L};
        for (long start : starts) {
            Address a = toAddr(start);
            disassemble(a);
            for (int i = 0; i < 10; i++) {
                Instruction ins = getInstructionAt(a);
                if (ins == null) { break; }
                println(String.format("%06X %s", ins.getAddress().getOffset(), ins.toString()));
                a = ins.getAddress().add(ins.getLength());
            }
        }
    }
}
