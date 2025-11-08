package frontend.element;

import frontend.config.TokenType;

import java.util.ArrayList;

// RelExp → AddExp { ( <LSS> | <GRE> | <LEQ> | <GEQ> ) AddExp }
public class RelExp {
    private final ArrayList<AddExp> addExp = new ArrayList<>();
    private final ArrayList<RelExpType> relExpType = new ArrayList<>();

    public RelExp(ArrayList<AddExp> addExp, ArrayList<RelExpType> relExpType) {
        this.addExp.addAll(addExp);
        this.relExpType.addAll(relExpType);
    }

    public ArrayList<AddExp> getAddExp() {
        return addExp;
    }

    public ArrayList<RelExpType> getRelExpType() {
        return relExpType;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < addExp.size() - 1; i++) {
            sb.append(addExp.get(i)).append("\n");
            sb.append("<RelExp>").append("\n");
            if (relExpType.get(i).equals(RelExpType.Lss)) {
                sb.append(TokenType.LSS).append(" <\n");
            } else if (relExpType.get(i).equals(RelExpType.Gre)) {
                sb.append(TokenType.GRE).append(" >\n");
            } else if (relExpType.get(i).equals(RelExpType.Leq)) {
                sb.append(TokenType.LEQ).append(" <=\n");
            } else {
                sb.append(TokenType.GEQ).append(" >=\n");
            }
        }
        if (!addExp.isEmpty()) {
            sb.append(addExp.get(addExp.size() - 1)).append("\n");
        }
        sb.append("<RelExp>");
        return sb.toString();
    }

    public enum RelExpType {Lss, Gre, Leq, Geq}
}
