package frontend.element;

import frontend.utils.*;

import java.util.*;

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
        return ToString.formatComma(exp) + "<FuncRParams>";
    }
}
