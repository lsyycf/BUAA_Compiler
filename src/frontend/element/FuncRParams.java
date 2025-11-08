package frontend.element;

import frontend.config.TokenType;

import java.util.ArrayList;

// FuncRParams → Exp { <COMMA> Exp }
public class FuncRParams {
    private final ArrayList<Exp> exp = new ArrayList<>();

    public FuncRParams(ArrayList<Exp> exp) {
        this.exp.addAll(exp);
    }

    public ArrayList<Exp> getExp() {
        return exp;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < exp.size() - 1; i++) {
            sb.append(exp.get(i)).append("\n");
            sb.append(TokenType.COMMA).append(" ,\n");
        }
        if (!exp.isEmpty()) {
            sb.append(exp.get(exp.size() - 1)).append("\n");
        }
        sb.append("<FuncRParams>");
        return sb.toString();
    }
}
