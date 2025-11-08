package frontend.element;

import frontend.config.TokenType;

import java.util.ArrayList;

// LAndExp → EqExp { <AND> EqExp }
public class LAndExp {
    private final ArrayList<EqExp> eqExp = new ArrayList<>();

    public LAndExp(ArrayList<EqExp> eqExp) {
        this.eqExp.addAll(eqExp);
    }

    public ArrayList<EqExp> getEqExp() {
        return eqExp;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < eqExp.size() - 1; i++) {
            sb.append(eqExp.get(i)).append("\n");
            sb.append("<LAndExp>").append("\n");
            sb.append(TokenType.AND).append(" &&\n");
        }
        if (!eqExp.isEmpty()) {
            sb.append(eqExp.get(eqExp.size() - 1)).append("\n");
        }
        sb.append("<LAndExp>");
        return sb.toString();
    }
}
