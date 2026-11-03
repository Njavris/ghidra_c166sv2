// Export a complete linear disassembly of the program.
//@category C166
import java.io.*;
import ghidra.app.script.GhidraScript;
import ghidra.program.model.listing.*;

public class ExportFullListing extends GhidraScript {
    @Override
    public void run() throws Exception {
        String[] args = getScriptArgs();
        File out = new File(args.length > 0 ? args[0] : "/tmp/opencode/ghidra_full");
        out.mkdirs();
        int n = 0;
        try (PrintWriter w = new PrintWriter(new File(out, "disassembly.txt"), "UTF-8")) {
            InstructionIterator ii = currentProgram.getListing().getInstructions(true);
            while (ii.hasNext()) {
                Instruction ins = ii.next();
                StringBuilder bytes = new StringBuilder();
                try { for (byte b : ins.getBytes()) bytes.append(String.format("%02X", b)); } catch (Exception e) {}
                w.println(ins.getAddress() + "  " + bytes + "  " + ins.toString());
                n++;
            }
        }
        println("ExportFullListing instructions=" + n);
    }
}
