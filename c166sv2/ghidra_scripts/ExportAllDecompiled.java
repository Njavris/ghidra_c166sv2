// Export every function with decompiled C, assembly, calls and xrefs.
//@category C166
import java.io.*;
import java.util.*;
import ghidra.app.script.GhidraScript;
import ghidra.app.decompiler.*;
import ghidra.program.model.address.*;
import ghidra.program.model.listing.*;
import ghidra.program.model.symbol.*;

public class ExportAllDecompiled extends GhidraScript {
    private static String clean(String s) {
        if (s == null) return "";
        return s.replaceAll("[^A-Za-z0-9_.-]+", "_");
    }
    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\"\"");
    }
    @Override
    public void run() throws Exception {
        String[] args = getScriptArgs();
        File out = new File(args.length > 0 ? args[0] : "/tmp/opencode/ghidra_full");
        out.mkdirs();
        DecompInterface decomp = new DecompInterface();
        decomp.setOptions(new DecompileOptions());
        if (!decomp.openProgram(currentProgram)) {
            printerr("decompiler open failed");
            return;
        }
        FunctionManager fm = currentProgram.getFunctionManager();
        StringBuilder all = new StringBuilder();
        StringBuilder csv = new StringBuilder();
        csv.append("address,name,size,is_thunk,is_external,return_type,param_count,calling_convention,callers,callees,xrefs,file\n");
        int n = 0, ok = 0;
        for (Function f : fm.getFunctions(true)) {
            if (monitor.isCancelled()) break;
            n++;
            Address ep = f.getEntryPoint();
            String addr = ep.toString();
            String name = f.getName();
            long size = f.getBody().getNumAddresses();
            boolean thunk = f.isThunk();
            boolean ext = f.isExternal();
            String ret = f.getReturnType() == null ? "" : f.getReturnType().getDisplayName();
            int pc = f.getParameterCount();
            String cc = f.getCallingConventionName() == null ? "" : f.getCallingConventionName();
            List<String> callers = new ArrayList<>();
            List<String> callees = new ArrayList<>();
            List<String> xrefs = new ArrayList<>();
            try { for (Function c : f.getCallingFunctions(monitor)) callers.add(c.getEntryPoint().toString()+":"+c.getName()); } catch (Exception e) {}
            try { for (Function c : f.getCalledFunctions(monitor)) callees.add(c.getEntryPoint().toString()+":"+c.getName()); } catch (Exception e) {}
            try {
                ReferenceIterator ri = currentProgram.getReferenceManager().getReferencesTo(ep);
                while (ri.hasNext()) {
                    Reference r = ri.next();
                    xrefs.add(r.getFromAddress().toString()+"("+r.getReferenceType()+")");
                }
            } catch (Exception e) {}
            Collections.sort(callers); Collections.sort(callees); Collections.sort(xrefs);
            String base = addr + "__" + clean(name);
            String ctext = "";
            if (!ext) {
                DecompileResults res = decomp.decompileFunction(f, 60, monitor);
                if (res != null && res.decompileCompleted() && res.getDecompiledFunction() != null) {
                    ctext = res.getDecompiledFunction().getC();
                    ok++;
                } else {
                    ctext = "/* DECOMPILE FAILED: " + (res == null ? "null" : res.getErrorMessage()) + " */\n";
                }
                StringBuilder asm = new StringBuilder();
                InstructionIterator ii = currentProgram.getListing().getInstructions(f.getBody(), true);
                while (ii.hasNext()) {
                    Instruction ins = ii.next();
                    asm.append(ins.getAddress()).append("  ").append(ins.toString()).append("\n");
                }
                try (PrintWriter w = new PrintWriter(new File(out, base + ".c"), "UTF-8")) {
                    w.print("// " + addr + " " + name + " size=" + size + " cc=" + cc + " proto=" + f.getSignature().getPrototypeString() + "\n");
                    w.print(ctext);
                }
                try (PrintWriter w = new PrintWriter(new File(out, base + ".asm"), "UTF-8")) {
                    w.print("; " + addr + " " + name + " size=" + size + "\n" + asm);
                }
                all.append("\n/* ===== ").append(addr).append(" ").append(name).append(" size=").append(size).append(" ===== */\n");
                all.append(ctext).append("\n");
            }
            csv.append('"').append(esc(addr)).append("\",\"")
               .append(esc(name)).append("\",").append(size).append(',')
               .append(thunk).append(',').append(ext).append(",\"")
               .append(esc(ret)).append("\",").append(pc).append(",\"")
               .append(esc(cc)).append("\",\"")
               .append(esc(String.join(";", callers))).append("\",\"")
               .append(esc(String.join(";", callees))).append("\",\"")
               .append(esc(String.join(";", xrefs))).append("\",\"")
               .append(ext ? "" : base + ".c").append("\"\n");
            if (n % 25 == 0) println("exported " + n + " functions...");
        }
        try (PrintWriter w = new PrintWriter(new File(out, "functions.csv"), "UTF-8")) { w.print(csv); }
        try (PrintWriter w = new PrintWriter(new File(out, "all_decompiled.c"), "UTF-8")) { w.print(all); }
        try (PrintWriter w = new PrintWriter(new File(out, "summary.txt"), "UTF-8")) {
            w.println("program=" + currentProgram.getName());
            w.println("imageBase=" + currentProgram.getImageBase());
            w.println("language=" + currentProgram.getLanguageID());
            w.println("functions=" + n);
            w.println("decompiled_ok=" + ok);
            w.println("minAddress=" + currentProgram.getMinAddress());
            w.println("maxAddress=" + currentProgram.getMaxAddress());
        }
        println("DONE functions=" + n + " decompiled_ok=" + ok + " out=" + out);
    }
}
