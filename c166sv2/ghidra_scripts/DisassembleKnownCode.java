// Disassemble every code address recovered by the listing, then follow code refs.
//@category C166
import java.io.*;
import java.util.*;
import ghidra.app.script.GhidraScript;
import ghidra.program.model.address.*;
import ghidra.program.model.listing.*;
import ghidra.program.model.symbol.*;

public class DisassembleKnownCode extends GhidraScript {
    private boolean dis(long v) {
        Address a = toAddr(v);
        if (a == null || !currentProgram.getMemory().contains(a)) return false;
        if (getInstructionAt(a) != null) return true;
        try { return disassemble(a); } catch (Exception e) { return false; }
    }
    private void make(long v, String name) {
        Address a = toAddr(v);
        if (a == null || getInstructionAt(a) == null) return;
        if (getFunctionContaining(a) != null) return;
        try {
            Function f = createFunction(a, name);
            if (f != null) println("continuation function " + name + " @ " + a);
        } catch (Exception e) { println("function fail " + a + ": " + e); }
    }
    @Override
    public void run() throws Exception {
        String[] args = getScriptArgs();
        String labelsPath = args.length > 0 ? args[0] : "/tmp/opencode/code_labels.txt";
        int total = 0, newly = 0, failed = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(labelsPath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                total++;
                long v = Long.parseLong(line.replace("0x", ""), 16);
                boolean was = getInstructionAt(toAddr(v)) != null;
                if (!dis(v)) { failed++; println("DISASSEMBLE FAIL " + line); }
                else if (!was) newly++;
            }
        }
        // Follow direct code references from everything now disassembled.
        for (int pass = 0; pass < 4; pass++) {
            int added = 0;
            InstructionIterator ii = currentProgram.getListing().getInstructions(true);
            while (ii.hasNext()) {
                Instruction ins = ii.next();
                for (Reference r : ins.getReferencesFrom()) {
                    RefType t = r.getReferenceType();
                    if (!(t.isCall() || t.isJump() || t.isFlow())) continue;
                    Address to = r.getToAddress();
                    if (to == null || !currentProgram.getMemory().contains(to)) continue;
                    if (getInstructionAt(to) == null) {
                        if (dis(to.getOffset())) added++;
                    }
                }
            }
            println("reference pass " + pass + " added=" + added);
            if (added == 0) break;
        }
        String[][] blocks = {
            {"c0117c", "asc0_state_dispatch_default"},
            {"c011d8", "asc0_state2_rx_body"},
            {"c11eb0", "app_idle_wait_tail"},
            {"c12f6e", "kline_transport_fallback"},
            {"c12f9c", "kline_transport_timeout_tail"},
            {"c1305a", "kline_transport_epilogue"}
        };
        for (String[] b : blocks) make(Long.parseLong(b[0], 16), b[1]);
        int insCount = 0;
        InstructionIterator ii = currentProgram.getListing().getInstructions(true);
        while (ii.hasNext()) { ii.next(); insCount++; }
        println("labels=" + total + " newly_disassembled=" + newly + " failed=" + failed +
                " instructions=" + insCount + " functions=" + currentProgram.getFunctionManager().getFunctionCount());
    }
}
