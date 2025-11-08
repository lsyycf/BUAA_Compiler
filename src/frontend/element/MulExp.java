package frontend.element;

import frontend.config.TokenType;

import java.util.ArrayList;

// MulExp → UnaryExp { ( <MULT> | <DIV> | <MOD> ) UnaryExp }
public class MulExp {
    private final ArrayList<UnaryExp> unaryExp = new ArrayList<>();
    private final ArrayList<MulExpType> mulExpType = new ArrayList<>();

    public MulExp(ArrayList<UnaryExp> unaryExp, ArrayList<MulExpType> mulExpType) {
        this.unaryExp.addAll(unaryExp);
        this.mulExpType.addAll(mulExpType);
    }

    public ArrayList<UnaryExp> getUnaryExp() {
        return unaryExp;
    }

    public ArrayList<MulExpType> getMulExpType() {
        return mulExpType;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < unaryExp.size() - 1; i++) {
            sb.append(unaryExp.get(i)).append("\n");
            sb.append("<MulExp>").append("\n");
            if (mulExpType.get(i).equals(MulExpType.Mult)) {
                sb.append(TokenType.MULT).append(" *\n");
            } else if (mulExpType.get(i).equals(MulExpType.Div)) {
                sb.append(TokenType.DIV).append(" /\n");
            } else {
                sb.append(TokenType.MOD).append(" %\n");
            }
        }
        if (!unaryExp.isEmpty()) {
            sb.append(unaryExp.get(unaryExp.size() - 1)).append("\n");
        }
        sb.append("<MulExp>");
        return sb.toString();
    }

    public enum MulExpType {Mult, Div, Mod}
}
