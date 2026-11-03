// Create the XC164CS/MT05.2 memory windows that are not part of the raw flash.
//@category C166
import ghidra.app.script.GhidraScript;
import ghidra.program.model.address.Address;
import ghidra.program.model.mem.MemoryBlock;

public class CreateMT05Memory extends GhidraScript {
    private void block(String name, long start, long length) {
        Address a = toAddr(start);
        MemoryBlock existing = currentProgram.getMemory().getBlock(a);
        if (existing != null) {
            println("exists " + name + " @ " + a);
            return;
        }
        try {
            createMemoryBlock(name, a, new byte[(int) length], false);
            println("created " + name + " @ " + a + " size " + Long.toHexString(length));
        } catch (Exception e) {
            println("FAILED " + name + ": " + e.getMessage());
        }
    }

    @Override
    public void run() throws Exception {
        // Flash is created by the loader at 0xC00000.
        block("EXTERNAL_RAM", 0x000000L, 0x8000L);
        // 0x008000-0x00BFFF is the remaining data-RAM window.  The processor
        // spec already provides XRAM/ESFR/IRAM at 0x00C000-0x00FFFF.
        block("DATA_RAM", 0x008000L, 0x4000L);
        block("XRAM_SEG_E0", 0xE00000L, 0x10000L);
        block("TWINCAN", 0x200000L, 0x800L);
        println("memory blocks=" + currentProgram.getMemory().getBlocks().length);
    }
}
