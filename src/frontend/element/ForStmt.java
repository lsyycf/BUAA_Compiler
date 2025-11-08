package frontend.element;

import frontend.config.*;

import java.util.*;

// ForStmt → LVal <ASSIGN> Exp { <COMMA> LVal <ASSIGN> Exp }
public class ForStmt {
    private final ArrayList<LVal> lVal = new ArrayList<>();
    private final ArrayList<Exp> exp = new ArrayList<>();

    public ForStmt(ArrayList<LVal> lVal, ArrayList<Exp> exp) {
        this.lVal.addAll(lVal);
        this.exp.addAll(exp);
    }

    public ArrayList<LVal> getlVal() {
        return lVal;
    }

    public ArrayList<Exp> getExp() {
        return exp;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lVal.size() - 1; i++) {
            sb.append(lVal.get(i)).append("\n");
            sb.append(TokenType.ASSIGN).append(" =\n");
            sb.append(exp.get(i)).append("\n");
            sb.append(TokenType.COMMA).append(" ,\n");
        }
        if (!lVal.isEmpty()) {
            sb.append(lVal.get(lVal.size() - 1)).append("\n");
        }
        sb.append(TokenType.ASSIGN).append(" =\n");
        if (!exp.isEmpty()) {
            sb.append(exp.get(exp.size() - 1)).append("\n");
        }
        sb.append("<ForStmt>");
        return sb.toString();
    }
}
