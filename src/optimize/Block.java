package optimize;

import backend.data.*;
import backend.utils.*;

import java.util.*;


public class Block {
    private final IrList irList;
    private final ArrayList<Block> nexts = new ArrayList<>();
    private final ArrayList<Block> prevs = new ArrayList<>();

    public Block(IrList irList) {
        this.irList = irList;
    }

    public IrList getIrList() {
        return irList;
    }

    public ArrayList<Block> getNexts() {
        return nexts;
    }

    public ArrayList<Block> getPrevs() {
        return prevs;
    }

    public void addNext(Block block) {
        nexts.add(block);
    }

    public void addPrev(Block block) {
        prevs.add(block);
    }

    public Quadruple getLastQuad() {
        if (irList.size() == 0) return null;
        return irList.get(irList.size() - 1);
    }

    public String getLabel() {
        if (irList.size() > 0 && irList.get(0).op().equals("label")) {
            return irList.get(0).result();
        }
        return null;
    }

    private DAGNode getNode(String name, HashMap<String, DAGNode> varToNode, HashMap<String, DAGNode> constNodes) {
        if (Calculate.isNumber(name)) {
            if (!constNodes.containsKey(name)) {
                constNodes.put(name, new DAGNode("leaf", name, null, null));
            }
            return constNodes.get(name);
        }
        if (varToNode.containsKey(name)) {
            return varToNode.get(name);
        }
        DAGNode node = new DAGNode("leaf", name, null, null);
        node.validVars.add(name);
        varToNode.put(name, node);
        return node;
    }

    private void removeFromValid(String var, HashMap<String, DAGNode> varToNode) {
        if (varToNode.containsKey(var)) {
            DAGNode node = varToNode.get(var);
            node.validVars.remove(var);
            varToNode.remove(var);
        }
    }

    public void lcse() {
        DAGNode.ID_COUNTER = 0;
        HashMap<String, DAGNode> varToNode = new HashMap<>();
        HashMap<String, DAGNode> constNodes = new HashMap<>();
        HashMap<String, DAGNode> computedNodes = new HashMap<>();
        ArrayList<Quadruple> newIr = new ArrayList<>();
        for (Quadruple q : irList.getIRList()) {
            String op = q.op();
            String arg1 = q.arg1();
            String arg2 = q.arg2();
            String result = q.result();
            if (op.equals("assign") || Calculate.canCalculate(op)) {
                DAGNode left = getNode(arg1, varToNode, constNodes);
                DAGNode right = null;
                if (!op.equals("assign") && arg2 != null && !arg2.equals("_")) {
                    right = getNode(arg2, varToNode, constNodes);
                }
                if (op.equals("assign")) {
                    removeFromValid(result, varToNode);
                    varToNode.put(result, left);
                    left.validVars.add(result);
                    String src = left.getRepresentVar();
                    if (src == null) src = arg1;
                    newIr.add(new Quadruple("assign", src, "_", result));

                } else {
                    int leftId = left.id;
                    int rightId = (right == null) ? 0 : right.id;
                    if (Calculate.canSwap(op) && leftId > rightId) {
                        int temp = leftId;
                        leftId = rightId;
                        rightId = temp;
                    }
                    String key = op + "," + leftId + "," + rightId;
                    DAGNode exist = computedNodes.get(key);
                    if (exist != null && exist.getRepresentVar() != null) {
                        String src = exist.getRepresentVar();
                        removeFromValid(result, varToNode);
                        varToNode.put(result, exist);
                        exist.validVars.add(result);
                        newIr.add(new Quadruple("assign", src, "_", result));
                    } else {
                        DAGNode newNode = new DAGNode(op, null, left, right);
                        computedNodes.put(key, newNode);
                        removeFromValid(result, varToNode);
                        varToNode.put(result, newNode);
                        newNode.validVars.add(result);
                        String s1 = left.getRepresentVar();
                        if (s1 == null) s1 = arg1;
                        String s2 = (right != null) ? right.getRepresentVar() : null;
                        if (s2 == null) s2 = arg2;

                        newIr.add(new Quadruple(op, s1, s2, result));
                    }
                }
            } else {
                String newArg1 = arg1;
                String newArg2 = arg2;
                if (arg1 != null && !arg1.equals("_") && varToNode.containsKey(arg1)) {
                    String r = varToNode.get(arg1).getRepresentVar();
                    if (r != null) newArg1 = r;
                }
                if (arg2 != null && !arg2.equals("_") && varToNode.containsKey(arg2)) {
                    String r = varToNode.get(arg2).getRepresentVar();
                    if (r != null) newArg2 = r;
                }
                if (op.equals("call")) {
                    Iterator<Map.Entry<String, DAGNode>> it = varToNode.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<String, DAGNode> entry = it.next();
                        String varName = entry.getKey();
                        if (varName.startsWith("ir_idenfr_") && varName.endsWith("_1")) {
                            entry.getValue().validVars.remove(varName);
                            it.remove();
                        }
                    }
                }
                boolean definesResult = result != null && !result.equals("_")
                        && !op.equals("store") && !op.equals("beq") && !op.equals("j")
                        && !op.equals("label") && !op.startsWith("func") && !op.startsWith("array")
                        && !op.equals("alloc") && !op.startsWith("print") && !op.equals("param")
                        && !op.equals("ret");

                if (definesResult) {
                    DAGNode leaf = new DAGNode("leaf", null, null, null);
                    removeFromValid(result, varToNode);
                    varToNode.put(result, leaf);
                    leaf.validVars.add(result);
                }

                newIr.add(new Quadruple(op, newArg1, newArg2, result));
            }
        }
        irList.getIRList().clear();
        irList.getIRList().addAll(newIr);
    }

    @Override
    public String toString() {
        return irList.toString();
    }

    private static class DAGNode {
        static int ID_COUNTER = 0;
        final int id;
        final String op;
        final String val;
        final DAGNode left;
        final DAGNode right;
        final List<String> validVars = new ArrayList<>();

        public DAGNode(String op, String val, DAGNode left, DAGNode right) {
            this.id = ++ID_COUNTER;
            this.op = op;
            this.val = val;
            this.left = left;
            this.right = right;
        }

        public String getRepresentVar() {
            if ("leaf".equals(op) && val != null && Calculate.isNumber(val)) {
                return val;
            }
            if (!validVars.isEmpty()) {
                return validVars.get(0);
            }
            return null;
        }

        @Override
        public String toString() {
            String l = (left == null) ? "null" : String.valueOf(left.id);
            String r = (right == null) ? "null" : String.valueOf(right.id);
            return String.format("Node%d: %s (L:%s, R:%s)", id, op, l, r);
        }
    }
}