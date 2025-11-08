package frontend.element;

import frontend.config.TokenType;

import java.util.ArrayList;

// InitVal → Exp | <LBRACE> [ Exp { <COMMA> Exp } ] <RBRACE>
public class InitVal {
    private final ArrayList<Exp> expArr = new ArrayList<>();
    private final InitValType initValType;
    private Exp exp;

    public InitVal(Exp exp) {
        this.exp = exp;
        this.initValType = InitValType.Exp;
    }

    public InitVal(ArrayList<Exp> expArr) {
        this.expArr.addAll(expArr);
        this.initValType = InitValType.ExpArr;
    }

    public Exp getExp() {
        return exp;
    }

    public ArrayList<Exp> getExpArr() {
        return expArr;
    }

    public InitValType getInitValType() {
        return initValType;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (initValType == InitValType.Exp) {
            sb.append(exp).append("\n");
        } else {
            sb.append(TokenType.LBRACE).append(" {\n");
            for (int i = 0; i < expArr.size() - 1; i++) {
                sb.append(expArr.get(i)).append("\n");
                sb.append(TokenType.COMMA).append(" ,\n");
            }
            if (!expArr.isEmpty()) {
                sb.append(expArr.get(expArr.size() - 1)).append("\n");
            }
            sb.append(TokenType.RBRACE).append(" }\n");
        }
        sb.append("<InitVal>");
        return sb.toString();
    }

    public enum InitValType {Exp, ExpArr}
}
