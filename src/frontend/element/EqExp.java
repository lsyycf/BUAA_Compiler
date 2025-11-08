package frontend.element;

import frontend.config.*;

import java.util.*;

// EqExp → RelExp { ( <EQL> | <NEQ> ) RelExp }
public class EqExp {
    public final ArrayList<RelExp> relExp = new ArrayList<>();
    public final ArrayList<EqExpType> eqExpType = new ArrayList<>();

    public EqExp(ArrayList<RelExp> relExp, ArrayList<EqExpType> eqExpType) {
        this.relExp.addAll(relExp);
        this.eqExpType.addAll(eqExpType);
    }

    public ArrayList<RelExp> getRelExp() {
        return relExp;
    }

    public ArrayList<EqExpType> getEqExpType() {
        return eqExpType;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < relExp.size() - 1; i++) {
            sb.append(relExp.get(i)).append("\n");
            sb.append("<EqExp>").append("\n");
            if (eqExpType.get(i).equals(EqExpType.Eql)) {
                sb.append(TokenType.EQL).append(" ==\n");
            } else {
                sb.append(TokenType.NEQ).append(" !=\n");
            }
        }
        if (!relExp.isEmpty()) {
            sb.append(relExp.get(relExp.size() - 1)).append("\n");
        }
        sb.append("<EqExp>");
        return sb.toString();
    }

    public enum EqExpType {Eql, Neq}
}
