package frontend.element;

import frontend.config.*;

import java.util.*;

// AddExp → MulExp { ( <PLUS> | <MINU> ) MulExp }
public class AddExp {
    private final ArrayList<MulExp> mulExp = new ArrayList<>();
    private final ArrayList<AddExpType> addExpType = new ArrayList<>();

    public AddExp(ArrayList<MulExp> mulExp, ArrayList<AddExpType> addExpType) {
        this.mulExp.addAll(mulExp);
        this.addExpType.addAll(addExpType);
    }

    public ArrayList<MulExp> getMulExp() {
        return mulExp;
    }

    public ArrayList<AddExpType> getAddExpType() {
        return addExpType;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mulExp.size() - 1; i++) {
            sb.append(mulExp.get(i)).append("\n");
            sb.append("<AddExp>").append("\n");
            if (addExpType.get(i).equals(AddExpType.Plus)) {
                sb.append(TokenType.PLUS).append(" +\n");
            } else {
                sb.append(TokenType.MINU).append(" -\n");
            }
        }
        if (!mulExp.isEmpty()) {
            sb.append(mulExp.get(mulExp.size() - 1)).append("\n");
        }
        sb.append("<AddExp>");
        return sb.toString();
    }

    public enum AddExpType {Plus, Minu}
}
