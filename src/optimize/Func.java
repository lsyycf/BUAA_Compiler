package optimize;

import backend.data.*;
import backend.utils.*;

import java.util.*;

public class Func {
    private static int licmLabelCounter = 0;
    private static int srTempCounter = 0;
    private final IrList irList;
    private final ArrayList<Block> blockList = new ArrayList<>();

    public Func(IrList irList) {
        this.irList = irList;
    }

    public IrList getIrList() {
        return irList;
    }

    @Override
    public String toString() {
        return irList.toString();
    }

    public void splitBlock() {
        TreeSet<Integer> index = new TreeSet<>();
        HashSet<String> labels = new HashSet<>();
        index.add(0);
        for (int i = 0; i < irList.size(); i++) {
            String op = irList.get(i).op();
            if (op.equals("beq") || op.equals("j") || op.equals("ret")) {
                index.add(i + 1);
            }
            if (op.equals("label")) {
                labels.add(irList.get(i).result());
            }
        }
        for (int i = 0; i < irList.size(); i++) {
            if (irList.get(i).op().equals("label") && labels.contains(irList.get(i).result())) {
                index.add(i);
            }
        }
        index.add(irList.size());
        ArrayList<Integer> list = new ArrayList<>(index);
        for (int i = 0; i < list.size() - 1; i++) {
            int l = list.get(i);
            int r = list.get(i + 1);
            if (l >= r) continue;
            IrList temp = new IrList();
            temp.getIRList().addAll(irList.getIRList().subList(l, r));
            blockList.add(new Block(temp));
        }
    }

    public void buildGraph() {
        for (Block b : blockList) {
            b.getNexts().clear();
            b.getPrevs().clear();
        }
        HashMap<String, Block> labelMap = new HashMap<>();
        for (Block block : blockList) {
            String label = block.getLabel();
            if (label != null) {
                labelMap.put(label, block);
            }
        }
        for (int i = 0; i < blockList.size(); i++) {
            Block currentBlock = blockList.get(i);
            Quadruple lastQuad = currentBlock.getLastQuad();
            if (lastQuad == null) continue;
            String op = lastQuad.op();
            String targetLabel = lastQuad.result();

            switch (op) {
                case "j" -> {
                    if (labelMap.containsKey(targetLabel)) {
                        Block target = labelMap.get(targetLabel);
                        currentBlock.addNext(target);
                        target.addPrev(currentBlock);
                    }
                }
                case "beq" -> {
                    if (labelMap.containsKey(targetLabel)) {
                        Block target = labelMap.get(targetLabel);
                        currentBlock.addNext(target);
                        target.addPrev(currentBlock);
                    }
                    if (i + 1 < blockList.size()) {
                        Block next = blockList.get(i + 1);
                        currentBlock.addNext(next);
                        next.addPrev(currentBlock);
                    }
                }
                case "ret" -> {
                }
                default -> {
                    if (i + 1 < blockList.size()) {
                        Block next = blockList.get(i + 1);
                        currentBlock.addNext(next);
                        next.addPrev(currentBlock);
                    }
                }
            }
        }
    }

    private HashSet<String> collectLocalVars() {
        HashSet<String> localVars = new HashSet<>();
        for (Block b : blockList) {
            for (Quadruple q : b.getIrList().getIRList()) {
                if (q.op().equals("alloc") || q.op().equals("func_param") || q.op().equals("array_alloc")) {
                    localVars.add(q.arg1());
                }
            }
        }
        return localVars;
    }

    private void handleCallSideEffects(String res, HashMap<String, Value> currentVars, HashSet<String> localVars) {
        if (res != null && !res.equals("_")) {
            currentVars.put(res, new Value(ValType.BOTTOM));
        }
        List<String> vars = new ArrayList<>(currentVars.keySet());
        for (String var : vars) {
            if (!localVars.contains(var)) {
                currentVars.put(var, new Value(ValType.BOTTOM));
            }
        }
    }

    private String generateSrAdjustment(ArrayList<Quadruple> newIr, String arg1, int k) {
        String t1 = "opt_sr_temp_" + (srTempCounter++);
        newIr.add(new Quadruple("alloc", t1, null, "int"));
        newIr.add(new Quadruple("srav", arg1, "31", t1));

        String t2 = "opt_sr_temp_" + (srTempCounter++);
        newIr.add(new Quadruple("alloc", t2, null, "int"));
        newIr.add(new Quadruple("srlv", t1, String.valueOf(32 - k), t2));

        String t3 = "opt_sr_temp_" + (srTempCounter++);
        newIr.add(new Quadruple("alloc", t3, null, "int"));
        newIr.add(new Quadruple("addu", arg1, t2, t3));

        return t3;
    }

    public void removeDeadBlocks() {
        if (blockList.isEmpty()) return;
        HashSet<Block> visited = new HashSet<>();
        dfs(blockList.get(0), visited);
        ArrayList<Block> aliveBlocks = new ArrayList<>();
        for (Block block : blockList) {
            boolean hasFuncEnd = false;
            for (Quadruple quad : block.getIrList().getIRList()) {
                if (quad.op().equals("func_end")) {
                    hasFuncEnd = true;
                    break;
                }
            }
            if (visited.contains(block) || hasFuncEnd) {
                aliveBlocks.add(block);
            }
        }
        blockList.clear();
        blockList.addAll(aliveBlocks);
        irList.getIRList().clear();
        for (Block block : blockList) {
            irList.addAll(block.getIrList());
        }
    }

    public void lcse() {
        for (Block block : blockList) {
            block.lcse();
        }
    }

    private void dfs(Block block, HashSet<Block> visited) {
        if (visited.contains(block)) return;
        visited.add(block);
        for (Block next : block.getNexts()) {
            dfs(next, visited);
        }
    }

    public void loopInvariantCodeMotion() {
        HashMap<Block, HashSet<Block>> doms = computeDominators();
        HashMap<Block, HashSet<Block>> loops = findNaturalLoops(doms);
        for (Map.Entry<Block, HashSet<Block>> entry : loops.entrySet()) {
            processLoop(entry.getKey(), entry.getValue());
        }
        irList.getIRList().clear();
        for (Block block : blockList) {
            irList.addAll(block.getIrList());
        }
    }

    private HashMap<Block, HashSet<Block>> computeDominators() {
        HashMap<Block, HashSet<Block>> doms = new HashMap<>();
        if (blockList.isEmpty()) return doms;
        Block startNode = blockList.get(0);
        HashSet<Block> allBlocks = new HashSet<>(blockList);
        for (Block block : blockList) {
            if (block == startNode) {
                HashSet<Block> s = new HashSet<>();
                s.add(startNode);
                doms.put(block, s);
            } else {
                doms.put(block, new HashSet<>(allBlocks));
            }
        }
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Block block : blockList) {
                if (block == startNode) continue;
                HashSet<Block> newDom = null;
                for (Block pred : block.getPrevs()) {
                    if (doms.containsKey(pred)) {
                        if (newDom == null) {
                            newDom = new HashSet<>(doms.get(pred));
                        } else {
                            newDom.retainAll(doms.get(pred));
                        }
                    }
                }
                if (newDom == null) newDom = new HashSet<>();
                newDom.add(block);
                if (!newDom.equals(doms.get(block))) {
                    doms.put(block, newDom);
                    changed = true;
                }
            }
        }
        return doms;
    }

    private HashMap<Block, HashSet<Block>> findNaturalLoops(HashMap<Block, HashSet<Block>> doms) {
        HashMap<Block, HashSet<Block>> loops = new HashMap<>();
        for (Block block : blockList) {
            for (Block next : block.getNexts()) {
                if (doms.containsKey(block) && doms.get(block).contains(next)) {
                    HashSet<Block> loopBody = loops.getOrDefault(next, new HashSet<>());
                    loopBody.add(next);
                    loopBody.add(block);
                    Stack<Block> stack = new Stack<>();
                    stack.push(block);
                    while (!stack.isEmpty()) {
                        Block curr = stack.pop();
                        for (Block pred : curr.getPrevs()) {
                            if (!loopBody.contains(pred)) {
                                loopBody.add(pred);
                                stack.push(pred);
                            }
                        }
                    }
                    loops.put(next, loopBody);
                }
            }
        }
        return loops;
    }

    private void processLoop(Block header, HashSet<Block> loopBody) {
        HashSet<String> definedInLoop = new HashSet<>();
        HashMap<String, Integer> defCounts = new HashMap<>();
        for (Block b : loopBody) {
            for (Quadruple q : b.getIrList().getIRList()) {
                String res = q.result();
                if (res != null && !res.equals("_")) {
                    definedInLoop.add(res);
                    defCounts.put(res, defCounts.getOrDefault(res, 0) + 1);
                }
            }
        }
        ArrayList<Quadruple> invariants = new ArrayList<>();
        HashMap<Quadruple, Block> quadToBlock = new HashMap<>();
        boolean changed = true;
        HashSet<Quadruple> invariantSet = new HashSet<>();
        while (changed) {
            changed = false;
            for (Block b : loopBody) {
                for (Quadruple q : b.getIrList().getIRList()) {
                    if (invariantSet.contains(q)) continue;
                    if (!canBeMoved(q)) continue;
                    String arg1 = q.arg1();
                    String arg2 = q.arg2();
                    String res = q.result();
                    boolean arg1Ok = isOperandInvariant(arg1, definedInLoop, invariantSet);
                    boolean arg2Ok = isOperandInvariant(arg2, definedInLoop, invariantSet);
                    boolean resOk = defCounts.getOrDefault(res, 0) == 1;
                    if (arg1Ok && arg2Ok && resOk) {
                        invariantSet.add(q);
                        invariants.add(q);
                        quadToBlock.put(q, b);
                        changed = true;
                    }
                }
            }
        }
        if (invariants.isEmpty()) return;
        insertPreHeaderAndMove(header, loopBody, invariants, quadToBlock);
    }

    private boolean canBeMoved(Quadruple q) {
        String op = q.op();
        return Calculate.canCalculate(op) || op.equals("assign");
    }

    private boolean isOperandInvariant(String arg, HashSet<String> definedInLoop, HashSet<Quadruple> invariantSet) {
        if (arg == null || arg.equals("_")) return true;
        if (Calculate.isNumber(arg)) return true;
        if (!definedInLoop.contains(arg)) return true;
        for (Quadruple inv : invariantSet) {
            if (arg.equals(inv.result())) return true;
        }
        return false;
    }

    private void insertPreHeaderAndMove(Block header, HashSet<Block> loopBody, ArrayList<Quadruple> invariants, HashMap<Quadruple, Block> quadToBlock) {
        String preHeaderLabel = "licm_pre_" + (licmLabelCounter++);
        IrList preIr = new IrList();
        preIr.add(new Quadruple("label", "_", "_", preHeaderLabel));
        for (Quadruple q : invariants) {
            preIr.add(q);
            Block origin = quadToBlock.get(q);
            origin.getIrList().getIRList().remove(q);
        }
        String headerLabel = header.getLabel();
        if (headerLabel == null) {
            headerLabel = "licm_head_" + (licmLabelCounter++);
            header.getIrList().getIRList().add(0, new Quadruple("label", "_", "_", headerLabel));
        }
        preIr.add(new Quadruple("j", "_", "_", headerLabel));
        Block preHeader = new Block(preIr);
        ArrayList<Block> predsToUpdate = new ArrayList<>();
        for (Block pred : header.getPrevs()) {
            if (!loopBody.contains(pred)) {
                predsToUpdate.add(pred);
            }
        }
        for (Block pred : predsToUpdate) {
            IrList list = pred.getIrList();
            for (int i = 0; i < list.size(); i++) {
                Quadruple q = list.get(i);
                if ((q.op().equals("j") || q.op().equals("beq")) && q.result().equals(headerLabel)) {
                    list.getIRList().set(i, new Quadruple(q.op(), q.arg1(), q.arg2(), preHeaderLabel));
                }
            }
            pred.getNexts().remove(header);
            pred.addNext(preHeader);
            preHeader.addPrev(pred);
            header.getPrevs().remove(pred);
        }
        preHeader.addNext(header);
        header.addPrev(preHeader);
        int headerIndex = blockList.indexOf(header);
        if (headerIndex != -1) {
            blockList.add(headerIndex, preHeader);
        } else {
            blockList.add(preHeader);
        }
    }

    private Value meet(Value v1, Value v2) {
        if (v1.type == ValType.TOP) return v2;
        if (v2.type == ValType.TOP) return v1;
        if (v1.type == ValType.CONST && v2.type == ValType.CONST) {
            if (v1.val == v2.val) return v1;
        }
        return new Value(ValType.BOTTOM);
    }

    private Value getValue(String arg, HashMap<String, Value> state) {
        if (Calculate.isNumber(arg)) return new Value(Integer.parseInt(arg));
        return state.getOrDefault(arg, new Value(ValType.TOP));
    }

    private HashMap<String, Value> mergePredecessors(Block block, HashMap<Block, HashMap<String, Value>> outStates) {
        HashMap<String, Value> inState = new HashMap<>();
        if (!block.getPrevs().isEmpty()) {
            boolean first = true;
            for (Block prev : block.getPrevs()) {
                if (!outStates.containsKey(prev)) continue;
                HashMap<String, Value> prevOut = outStates.get(prev);
                if (first) {
                    inState.putAll(prevOut);
                    first = false;
                } else {
                    HashSet<String> allVars = new HashSet<>();
                    allVars.addAll(inState.keySet());
                    allVars.addAll(prevOut.keySet());
                    for (String var : allVars) {
                        Value v1 = inState.getOrDefault(var, new Value(ValType.TOP));
                        Value v2 = prevOut.getOrDefault(var, new Value(ValType.TOP));
                        inState.put(var, meet(v1, v2));
                    }
                }
            }
        }
        return inState;
    }

    private Value calculateConstValue(String op, Value v1, Value v2) {
        if (v1.type == ValType.CONST && v2.type == ValType.CONST) {
            if ((op.equals("div") || op.equals("mod")) && v2.val == 0) {
                return null;
            }
            int cal = Calculate.getRes(op, String.valueOf(v1.val), String.valueOf(v2.val));
            return new Value(cal);
        } else if (v1.type == ValType.BOTTOM || v2.type == ValType.BOTTOM) {
            return new Value(ValType.BOTTOM);
        } else {
            return new Value(ValType.TOP);
        }
    }

    public void globalConstantPropagation() {
        HashSet<String> localVars = collectLocalVars();
        HashMap<Block, HashMap<String, Value>> outStates = new HashMap<>();
        for (Block b : blockList) {
            outStates.put(b, new HashMap<>());
        }
        Queue<Block> worklist = new LinkedList<>(blockList);

        while (!worklist.isEmpty()) {
            Block block = worklist.poll();
            HashMap<String, Value> currentVars = mergePredecessors(block, outStates);

            for (Quadruple q : block.getIrList().getIRList()) {
                String op = q.op();
                String res = q.result();
                String arg1 = q.arg1();
                String arg2 = q.arg2();

                if (op.equals("func_param")) {
                    currentVars.put(arg1, new Value(ValType.BOTTOM));
                } else if (op.equals("call")) {
                    handleCallSideEffects(res, currentVars, localVars);
                } else if (res != null && res.startsWith("ir_")) {
                    if (op.equals("assign")) {
                        Value val = getValue(arg1, currentVars);
                        currentVars.put(res, val);
                    } else if (Calculate.canCalculate(op)) {
                        Value v1 = getValue(arg1, currentVars);
                        Value v2 = getValue(arg2, currentVars);
                        Value calculated = calculateConstValue(op, v1, v2);
                        currentVars.put(res, Objects.requireNonNullElseGet(calculated, () -> new Value(ValType.BOTTOM)));
                    } else {
                        currentVars.put(res, new Value(ValType.BOTTOM));
                    }
                }
            }
            if (!currentVars.equals(outStates.get(block))) {
                outStates.put(block, currentVars);
                for (Block next : block.getNexts()) {
                    if (outStates.containsKey(next) && !worklist.contains(next)) {
                        worklist.add(next);
                    }
                }
            }
        }
        for (Block block : blockList) {
            HashMap<String, Value> currentVars = mergePredecessors(block, outStates);
            ArrayList<Quadruple> newIr = new ArrayList<>();

            for (Quadruple q : block.getIrList().getIRList()) {
                String op = q.op();
                String arg1 = q.arg1();
                String arg2 = q.arg2();
                String res = q.result();
                String newArg1 = arg1;
                String newArg2 = arg2;
                boolean isDecl = op.equals("alloc") || op.equals("array_alloc") || op.equals("func_param");
                if (!isDecl && arg1 != null && currentVars.containsKey(arg1) && currentVars.get(arg1).type == ValType.CONST) {
                    newArg1 = String.valueOf(currentVars.get(arg1).val);
                }
                if (arg2 != null && currentVars.containsKey(arg2) && currentVars.get(arg2).type == ValType.CONST) {
                    newArg2 = String.valueOf(currentVars.get(arg2).val);
                }
                Quadruple newQ = new Quadruple(op, newArg1, newArg2, res);

                if (op.equals("beq")) {
                    if (newArg1 != null && newArg2 != null && Calculate.isNumber(newArg1) && Calculate.isNumber(newArg2)) {
                        if (Integer.parseInt(newArg1) == Integer.parseInt(newArg2)) {
                            newIr.add(new Quadruple("j", "_", "_", res));
                        }
                    } else {
                        newIr.add(newQ);
                    }
                    continue;
                }

                if (op.equals("func_param")) {
                    currentVars.put(arg1, new Value(ValType.BOTTOM));
                    newIr.add(newQ);
                } else if (op.equals("call")) {
                    newIr.add(newQ);
                    handleCallSideEffects(res, currentVars, localVars);
                } else if (res != null && res.startsWith("ir_")) {
                    if (op.equals("assign")) {
                        Value val = getValue(newArg1, currentVars);
                        currentVars.put(res, val);
                        newIr.add(newQ);
                    } else if (Calculate.canCalculate(op)) {
                        Value v1 = getValue(newArg1, currentVars);
                        Value v2 = getValue(newArg2, currentVars);
                        Value calculated = calculateConstValue(op, v1, v2);

                        if (calculated != null && calculated.type == ValType.CONST) {
                            currentVars.put(res, calculated);
                            newIr.add(new Quadruple("assign", String.valueOf(calculated.val), "_", res));
                        } else {
                            currentVars.put(res, Objects.requireNonNullElseGet(calculated, () -> new Value(ValType.BOTTOM)));
                            newIr.add(newQ);
                        }
                    } else {
                        currentVars.put(res, new Value(ValType.BOTTOM));
                        newIr.add(newQ);
                    }
                } else {
                    newIr.add(newQ);
                }
            }
            block.getIrList().getIRList().clear();
            block.getIrList().getIRList().addAll(newIr);
        }
    }

    public void peephole() {
        for (Block block : blockList) {
            ArrayList<Quadruple> newIr = new ArrayList<>();
            for (Quadruple q : block.getIrList().getIRList()) {
                if (q.op().equals("assign") && q.arg1().equals(q.result())) {
                    continue;
                }
                newIr.add(q);
            }
            block.getIrList().getIRList().clear();
            block.getIrList().getIRList().addAll(newIr);
        }

        for (int i = 0; i < blockList.size() - 1; i++) {
            Block curr = blockList.get(i);
            Block next = blockList.get(i + 1);
            Quadruple last = curr.getLastQuad();
            String nextLabel = next.getLabel();
            if (last != null && nextLabel != null) {
                if ((last.op().equals("j") || last.op().equals("beq")) && last.result().equals(nextLabel)) {
                    curr.getIrList().getIRList().remove(curr.getIrList().size() - 1);
                }
            }
        }

        irList.getIRList().clear();
        for (Block block : blockList) {
            irList.addAll(block.getIrList());
        }
    }

    public void deadCodeElimination() {
        HashSet<String> localVars = collectLocalVars();
        boolean changed = true;
        while (changed) {
            changed = false;
            HashMap<Block, HashSet<String>> useMap = new HashMap<>();
            HashMap<Block, HashSet<String>> defMap = new HashMap<>();
            for (Block block : blockList) {
                computeUseDef(block, useMap, defMap);
            }
            HashMap<Block, HashSet<String>> liveIn = new HashMap<>();
            HashMap<Block, HashSet<String>> liveOut = new HashMap<>();
            for (Block block : blockList) {
                liveIn.put(block, new HashSet<>());
                liveOut.put(block, new HashSet<>());
            }
            boolean livedChanged = true;
            while (livedChanged) {
                livedChanged = false;
                for (int i = blockList.size() - 1; i >= 0; i--) {
                    Block block = blockList.get(i);
                    HashSet<String> newLiveOut = new HashSet<>();
                    for (Block succ : block.getNexts()) {
                        newLiveOut.addAll(liveIn.get(succ));
                    }
                    HashSet<String> newLiveIn = new HashSet<>(newLiveOut);
                    newLiveIn.removeAll(defMap.get(block));
                    newLiveIn.addAll(useMap.get(block));
                    if (!newLiveIn.equals(liveIn.get(block)) || !newLiveOut.equals(liveOut.get(block))) {
                        liveIn.put(block, newLiveIn);
                        liveOut.put(block, newLiveOut);
                        livedChanged = true;
                    }
                }
            }
            for (Block block : blockList) {
                HashSet<String> currentLive = new HashSet<>(liveOut.get(block));
                ArrayList<Quadruple> instructions = block.getIrList().getIRList();
                ListIterator<Quadruple> it = instructions.listIterator(instructions.size());
                while (it.hasPrevious()) {
                    Quadruple q = it.previous();
                    if (hasSideEffect(q)) {
                        updateLiveSet(q, currentLive);
                        continue;
                    }
                    String result = q.result();
                    boolean definesVar = result != null && !result.equals("_");
                    boolean isGlobalWrite = definesVar && !localVars.contains(result) && !result.startsWith("ir_temp");
                    if (isGlobalWrite) {
                        updateLiveSet(q, currentLive);
                        continue;
                    }
                    if (definesVar && !currentLive.contains(result)) {
                        it.remove();
                        changed = true;
                    } else {
                        updateLiveSet(q, currentLive);
                    }
                }
            }
        }
        irList.getIRList().clear();
        for (Block block : blockList) {
            irList.addAll(block.getIrList());
        }
    }

    private void computeUseDef(Block block, HashMap<Block, HashSet<String>> useMap, HashMap<Block, HashSet<String>> defMap) {
        HashSet<String> use = new HashSet<>();
        HashSet<String> def = new HashSet<>();
        for (Quadruple q : block.getIrList().getIRList()) {
            String arg1 = q.arg1();
            String arg2 = q.arg2();
            String result = q.result();
            if (isValidVar(arg1) && !def.contains(arg1)) use.add(arg1);
            if (isValidVar(arg2) && !def.contains(arg2)) use.add(arg2);
            if (q.op().equals("store")) {
                if (isValidVar(result) && !def.contains(result)) use.add(result);
            }
            if (result != null && !result.equals("_") && !q.op().equals("store") && !hasSideEffect(q)) {
                def.add(result);
            } else if ((q.op().equals("call") || q.op().equals("alloc") || q.op().equals("array_alloc")) && result != null && !result.equals("_")) {
                def.add(result);
            }
        }
        useMap.put(block, use);
        defMap.put(block, def);
    }

    private void updateLiveSet(Quadruple q, HashSet<String> currentLive) {
        String result = q.result();
        String arg1 = q.arg1();
        String arg2 = q.arg2();
        if (result != null && !result.equals("_") && !q.op().equals("store") && !q.op().startsWith("beq") && !q.op().equals("j")) {
            currentLive.remove(result);
        }
        if (isValidVar(arg1)) currentLive.add(arg1);
        if (isValidVar(arg2)) currentLive.add(arg2);
        if (q.op().equals("store") && isValidVar(result)) currentLive.add(result);
    }

    private boolean isValidVar(String s) {
        return s != null && !s.equals("_") && !Calculate.isNumber(s);
    }

    private boolean hasSideEffect(Quadruple q) {
        String op = q.op();
        return switch (op) {
            case "beq", "j", "ret", "label", "func_begin", "func_end", "alloc", "array_alloc", "store", "print",
                 "printf", "get_int", "call", "param", "func_param" -> true;
            default -> false;
        };
    }

    public void strengthReduction() {
        for (Block block : blockList) {
            ArrayList<Quadruple> newIr = new ArrayList<>();
            for (Quadruple q : block.getIrList().getIRList()) {
                String op = q.op();
                String arg1 = q.arg1();
                String arg2 = q.arg2();
                String result = q.result();
                int k = -1;
                if (Calculate.isNumber(arg2)) {
                    k = Calculate.getPower(arg2);
                }

                boolean processed = false;
                switch (op) {
                    case "addu" -> {
                        if (Calculate.isNumber(arg1) && Integer.parseInt(arg1) == 0) {
                            newIr.add(new Quadruple("assign", arg2, "_", result));
                            processed = true;
                        } else if (Calculate.isNumber(arg2) && Integer.parseInt(arg2) == 0) {
                            newIr.add(new Quadruple("assign", arg1, "_", result));
                            processed = true;
                        }
                    }
                    case "subu" -> {
                        if (Calculate.isNumber(arg2) && Integer.parseInt(arg2) == 0) {
                            newIr.add(new Quadruple("assign", arg1, "_", result));
                            processed = true;
                        }
                    }
                    case "mulu" -> {
                        if ((Calculate.isNumber(arg1) && Integer.parseInt(arg1) == 0) || (Calculate.isNumber(arg2) && Integer.parseInt(arg2) == 0)) {
                            newIr.add(new Quadruple("assign", "0", "_", result));
                            processed = true;
                        } else if (Calculate.isNumber(arg1) && Integer.parseInt(arg1) == 1) {
                            newIr.add(new Quadruple("assign", arg2, "_", result));
                            processed = true;
                        } else if (Calculate.isNumber(arg2) && Integer.parseInt(arg2) == 1) {
                            newIr.add(new Quadruple("assign", arg1, "_", result));
                            processed = true;
                        }
                    }
                    case "div" -> {
                        if (Calculate.isNumber(arg1) && Integer.parseInt(arg1) == 0) {
                            newIr.add(new Quadruple("assign", "0", "_", result));
                            processed = true;
                        } else if (Calculate.isNumber(arg2) && Integer.parseInt(arg2) == 1) {
                            newIr.add(new Quadruple("assign", arg1, "_", result));
                            processed = true;
                        }
                    }
                    case "mod" -> {
                        if (Calculate.isNumber(arg1) && Integer.parseInt(arg1) == 0) {
                            newIr.add(new Quadruple("assign", "0", "_", result));
                            processed = true;
                        } else if (Calculate.isNumber(arg2) && Integer.parseInt(arg2) == 1) {
                            newIr.add(new Quadruple("assign", "0", "_", result));
                            processed = true;
                        }
                    }
                }
                if (processed) continue;

                if (k != -1) {
                    switch (op) {
                        case "mulu" -> newIr.add(new Quadruple("sllv", arg1, String.valueOf(k), result));
                        case "div" -> {
                            if (k == 0) {
                                newIr.add(new Quadruple("assign", arg1, "_", result));
                            } else {
                                String t3 = generateSrAdjustment(newIr, arg1, k);
                                newIr.add(new Quadruple("srav", t3, String.valueOf(k), result));
                            }
                        }
                        case "mod" -> {
                            if (k == 0) {
                                newIr.add(new Quadruple("assign", "0", "_", result));
                            } else {
                                String t3 = generateSrAdjustment(newIr, arg1, k);
                                String quotient = "opt_sr_temp_" + (srTempCounter++);
                                newIr.add(new Quadruple("alloc", quotient, null, "int"));
                                newIr.add(new Quadruple("srav", t3, String.valueOf(k), quotient));
                                String p = "opt_sr_temp_" + (srTempCounter++);
                                newIr.add(new Quadruple("alloc", p, null, "int"));
                                newIr.add(new Quadruple("sllv", quotient, String.valueOf(k), p));
                                newIr.add(new Quadruple("subu", arg1, p, result));
                            }
                        }
                        default -> newIr.add(q);
                    }
                } else if (op.equals("mulu") && Calculate.isNumber(arg1) && Calculate.getPower(arg1) != -1) {
                    k = Calculate.getPower(arg1);
                    newIr.add(new Quadruple("sllv", arg2, String.valueOf(k), result));
                } else {
                    newIr.add(q);
                }
            }
            block.getIrList().getIRList().clear();
            block.getIrList().addAll(new IrList());
            block.getIrList().getIRList().addAll(newIr);
        }
        irList.getIRList().clear();
        for (Block block : blockList) {
            irList.addAll(block.getIrList());
        }
    }

    private enum ValType {TOP, CONST, BOTTOM}

    private static class Value {
        final ValType type;
        int val;

        Value(ValType type) {
            this.type = type;
        }

        Value(int val) {
            this.type = ValType.CONST;
            this.val = val;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Value value1 = (Value) o;
            if (type != value1.type) return false;
            return type != ValType.CONST || val == value1.val;
        }

        @Override
        public String toString() {
            return type == ValType.CONST ? String.valueOf(val) : type.toString();
        }
    }
}