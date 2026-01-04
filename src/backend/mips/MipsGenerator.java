package backend.mips;

import backend.data.*;
import backend.utils.*;

import java.util.*;

public class MipsGenerator {
    private final IrList irList;
    private final HashSet<String> vars = new HashSet<>();
    private final HashMap<String, Integer> arrays = new HashMap<>();
    private final HashMap<String, String> strings = new HashMap<>();
    private final HashMap<String, FuncStack> functions = new HashMap<>();
    private final StringBuilder dataSection = new StringBuilder();
    private final StringBuilder textSection = new StringBuilder();
    private FuncStack currentFunc;
    private int currentStack = 0;

    public MipsGenerator(IrList irList) {
        this.irList = irList;
    }

    public String generate() {
        collectData();
        generateDataSection();
        generateTextSection();
        return dataSection + "\n" + textSection;
    }

    private void collectData() {
        for (Quadruple quad : irList.getIRList()) {
            String op = quad.op();
            String arg1 = quad.arg1();
            String arg2 = quad.arg2();
            String result = quad.result();
            switch (op) {
                case "func_begin" -> {
                    currentFunc = new FuncStack(arg1);
                    currentStack = -8;
                }
                case "func_end" -> {
                    if (currentFunc != null) {
                        currentFunc.setStackSize(currentStack);
                        functions.put(currentFunc.getName(), currentFunc);
                    }
                    currentFunc = null;
                }
                case "func_param" -> {
                    if (currentFunc != null) {
                        currentFunc.putParam(arg1, Integer.parseInt(arg2) * 4);
                    }
                }
                case "alloc" -> {
                    if (currentFunc != null && !result.equals("static")) {
                        currentStack -= 4;
                        currentFunc.putLocal(arg1, currentStack);
                    } else {
                        vars.add(arg1);
                    }
                }
                case "array_alloc" -> {
                    if (currentFunc != null && !result.equals("static")) {
                        currentStack -= Integer.parseInt(arg2) * 4;
                        currentFunc.putLocal(arg1, currentStack);
                    } else {
                        arrays.put(arg1, Integer.parseInt(arg2));
                    }
                }
                case "print" -> strings.put(result, arg1);
            }
        }
    }

    private void generateDataSection() {
        dataSection.append(".data\n");
        for (String var : vars) {
            dataSection.append(var).append(": .space 4\n");
        }
        dataSection.append("\n");
        for (HashMap.Entry<String, Integer> entry : arrays.entrySet()) {
            String arrayName = entry.getKey();
            int size = entry.getValue();
            dataSection.append(arrayName).append(": .space ").append(size * 4).append("\n");
        }
        dataSection.append("\n");
        for (HashMap.Entry<String, String> entry : strings.entrySet()) {
            String label = entry.getKey();
            String content = entry.getValue();
            dataSection.append(label).append(": .asciiz \"").append(content.replace("\"", "\\\"")).append("\"\n");
        }
        dataSection.append("\n");
    }

    private void generateTextSection() {
        textSection.append(".text\n");
        textSection.append(".globl main\n\n");
        for (Quadruple quad : irList.getIRList()) {
            textSection.append("# ").append(quad.toString()).append("\n");
            String op = quad.op();
            String arg1 = quad.arg1();
            String arg2 = quad.arg2();
            String result = quad.result();
            switch (op) {
                case "func_begin" -> generateFuncBegin(arg1);
                case "func_end" -> generateFuncEnd();
                case "ret" -> generateReturn(arg1);
                case "assign" -> {
                    load(arg1, "$t0");
                    store(result, "$t0");
                }
                case "store" -> generateArrayStore(arg1, arg2, result);
                case "load" -> generateArrayLoad(arg1, arg2, result);
                case "label" -> textSection.append(result).append(":\n");
                case "j" -> textSection.append("j ").append(result).append("\n");
                case "beq" -> {
                    load(arg1, "$t0");
                    load(arg2, "$t1");
                    textSection.append("beq $t0, $t1, ").append(result).append("\n");
                }
                case "param" -> generateParam(arg1, arg2);
                case "call" -> generateCall(arg1, arg2, result);
                case "get_int" -> {
                    textSection.append("li $v0, 5\n");
                    textSection.append("syscall\n");
                    store(result, "$v0");
                }
                case "print" -> {
                    textSection.append("la $a0, ").append(result).append("\n");
                    textSection.append("li $v0, 4\n");
                    textSection.append("syscall\n");
                }
                case "printf" -> {
                    load(arg1, "$a0");
                    textSection.append("li $v0, 1\n");
                    textSection.append("syscall\n");
                }
                default -> {
                    if (Calculate.canCalculate(op)) {
                        generateBinaryOp(op, arg1, arg2, result);
                    }
                }
            }
        }
        textSection.append("li $v0, 10\n");
        textSection.append("syscall\n");
    }

    private void load(String var, String reg) {
        if (Calculate.isNumber(var)) {
            textSection.append("li ").append(reg).append(", ").append(var).append("\n");
        } else if (currentFunc != null && currentFunc.containsLocal(var)) {
            int offset = currentFunc.getLocalOffsets().get(var);
            textSection.append("lw ").append(reg).append(", ").append(offset).append("($fp)\n");
        } else if (currentFunc != null && currentFunc.containsParam(var)) {
            int offset = currentFunc.getParamOffsets().get(var);
            textSection.append("lw ").append(reg).append(", ").append(offset).append("($fp)\n");
        } else {
            textSection.append("lw ").append(reg).append(", ").append(var).append("\n");
        }
    }

    private void store(String var, String reg) {
        if (currentFunc != null && currentFunc.containsLocal(var)) {
            int offset = currentFunc.getLocalOffsets().get(var);
            textSection.append("sw ").append(reg).append(", ").append(offset).append("($fp)\n");
        } else if (currentFunc != null && currentFunc.containsParam(var)) {
            int offset = currentFunc.getParamOffsets().get(var);
            textSection.append("sw ").append(reg).append(", ").append(offset).append("($fp)\n");
        } else {
            textSection.append("sw ").append(reg).append(", ").append(var).append("\n");
        }
    }

    private void loadAddr(String var) {
        String reg = "$t0";
        if (currentFunc != null && currentFunc.containsLocal(var)) {
            int offset = currentFunc.getLocalOffsets().get(var);
            textSection.append("addu ").append(reg).append(", $fp, ").append(offset).append("\n");
        } else if (currentFunc != null && currentFunc.containsParam(var)) {
            int offset = currentFunc.getParamOffsets().get(var);
            textSection.append("lw ").append(reg).append(", ").append(offset).append("($fp)\n");
        } else {
            textSection.append("la ").append(reg).append(", ").append(var).append("\n");
        }
    }

    private void generateFuncBegin(String funcName) {
        currentFunc = functions.get(funcName);
        textSection.append("\n").append(funcName).append(":\n");
        textSection.append("sw $ra, -4($sp)\n");
        textSection.append("sw $fp, -8($sp)\n");
        textSection.append("move $fp, $sp\n");
        int stackSize = Math.abs(currentFunc.getStackSize());
        textSection.append("subu $sp, $sp, ").append(stackSize).append("\n");
    }

    private void generateFuncEnd() {
        String endLabel = currentFunc.getName() + "_end";
        textSection.append(endLabel).append(":\n");
        int stackSize = Math.abs(currentFunc.getStackSize());
        textSection.append("addu $sp, $sp, ").append(stackSize).append("\n");
        textSection.append("lw $ra, -4($fp)\n");
        textSection.append("lw $fp, -8($fp)\n");
        if (!currentFunc.getName().equals("main")) {
            textSection.append("jr $ra\n");
        }
        textSection.append("\n");
        currentFunc = null;
    }

    private void generateReturn(String val) {
        if (!val.equals("_")) {
            load(val, "$v0");
        }
        textSection.append("j ").append(currentFunc.getName()).append("_end\n");
    }

    private void generateBinaryOp(String op, String arg1, String arg2, String result) {
        load(arg1, "$t0");
        load(arg2, "$t1");
        if (op.equals("mod")) {
            textSection.append("div $t0, $t1\n");
            textSection.append("mfhi $t0\n");
        } else {
            textSection.append(op).append(" $t0, $t0, $t1\n");
        }
        store(result, "$t0");
    }

    private void generateArrayStore(String value, String index, String arrayName) {
        loadAddr(arrayName);
        load(index, "$t1");
        textSection.append("sll $t1, $t1, 2\n");
        textSection.append("addu $t0, $t0, $t1\n");
        load(value, "$t1");
        textSection.append("sw $t1, 0($t0)\n");
    }

    private void generateArrayLoad(String arrayName, String index, String dst) {
        loadAddr(arrayName);
        load(index, "$t1");
        textSection.append("sll $t1, $t1, 2\n");
        textSection.append("addu $t0, $t0, $t1\n");
        textSection.append("lw $t0, 0($t0)\n");
        store(dst, "$t0");
    }

    private void generateParam(String value, String type) {
        if (type.equals("array")) {
            loadAddr(value);
        } else {
            load(value, "$t0");
        }
        textSection.append("subu $sp, $sp, 4\n");
        textSection.append("sw $t0, 0($sp)\n");
    }

    private void generateCall(String funcName, String paramCountStr, String result) {
        textSection.append("jal ").append(funcName).append("\n");
        int paramCount = Integer.parseInt(paramCountStr);
        if (paramCount > 0) {
            textSection.append("addu $sp, $sp, ").append(paramCount * 4).append("\n");
        }
        if (!result.equals("_")) {
            store(result, "$v0");
        }
    }
}
