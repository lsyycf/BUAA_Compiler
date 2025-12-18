package optimize;

import backend.data.*;

import java.util.*;

public class Optimize {
    private final IrList irList;
    private final ArrayList<Func> funcList = new ArrayList<>();
    private IrList global = new IrList();

    public Optimize(IrList irList) {
        this.irList = irList;
    }

    public IrList optimize() {
        String old;
        do {
            old = this.irList.toString();
            splitFunc();
            for (Func func : funcList) {
                func.splitBlock();
                func.buildGraph();
                func.removeDeadBlocks();
                func.globalConstantPropagation();
                func.deadCodeElimination();
                func.lcse();
                func.deadCodeElimination();
                func.loopInvariantCodeMotion();
                func.deadCodeElimination();
                func.strengthReduction();
                func.deadCodeElimination();
                func.peephole();
                func.deadCodeElimination();
            }
            this.irList.getIRList().clear();
            this.irList.addAll(global);
            for (Func func : funcList) {
                this.irList.addAll(func.getIrList());
            }
            global = new IrList();
            funcList.clear();
        } while (!irList.toString().equals(old));
        return irList;
    }

    private void splitFunc() {
        TreeSet<Integer> index = new TreeSet<>();
        for (int i = 0; i < irList.size(); i++) {
            if (irList.getIRList().get(i).op().equals("func_begin")) {
                index.add(i);
            }
        }
        index.add(irList.size());
        ArrayList<Integer> list = new ArrayList<>(index);

        if (!list.isEmpty()) {
            global.getIRList().addAll(irList.getIRList().subList(0, list.get(0)));
        }
        for (int i = 0; i < list.size() - 1; i++) {
            int l = list.get(i);
            int r = list.get(i + 1);
            IrList temp = new IrList();
            temp.getIRList().addAll(irList.getIRList().subList(l, r));
            funcList.add(new Func(temp));
        }
    }
}