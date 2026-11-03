// Report disassembly coverage and code-referenced targets that remain undefined.
//@category C166
import java.util.*;
import ghidra.app.script.GhidraScript;
import ghidra.program.model.address.*;
import ghidra.program.model.listing.*;
import ghidra.program.model.symbol.*;

public class ReportUndefinedCode extends GhidraScript {
    @Override
    public void run() throws Exception {
        int ins = 0;
        Set<Address> unresolved = new TreeSet<>();
        InstructionIterator ii = currentProgram.getListing().getInstructions(true);
        while (ii.hasNext()) {
            Instruction i = ii.next(); ins++;
            for (Reference r : i.getReferencesFrom()) {
                RefType t = r.getReferenceType();
                if (!(t.isCall() || t.isJump() || t.isFlow())) continue;
                Address to = r.getToAddress();
                if (to == null || !currentProgram.getMemory().contains(to)) continue;
                if (getInstructionAt(to) == null) unresolved.add(to);
            }
        }
        println("instructions=" + ins + " functions=" + currentProgram.getFunctionManager().getFunctionCount());
        for (Address a : unresolved) println("UNDEFINED_CODE_TARGET " + a);
        println("undefined_code_targets=" + unresolved.size());
    }
}
