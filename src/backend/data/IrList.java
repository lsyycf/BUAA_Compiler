package backend.data;

import java.util.ArrayList;

public class IrList {
    private final ArrayList<Quadruple> irList = new ArrayList<>();

    public void add(Quadruple quad) {
        this.irList.add(quad);
    }

    public ArrayList<Quadruple> getIRList() {
        return irList;
    }

    public int size() {
        return irList.size();
    }

    public Quadruple get(int i) {
        return irList.get(i);
    }

    public void addAll(IrList irList) {
        this.irList.addAll(irList.irList);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < irList.size() - 1; i++) {
            sb.append(irList.get(i)).append("\n");
        }
        if (!irList.isEmpty()) {
            sb.append(irList.get(irList.size() - 1));
        }
        return sb.toString();
    }
}
