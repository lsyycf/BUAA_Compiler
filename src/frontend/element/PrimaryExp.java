package frontend.element;

import frontend.config.TokenType;

// PrimaryExp → <LPARENT> Exp <RPARENT> | LVal | Number
public class PrimaryExp {
    private final PrimaryExpType primaryExpType;
    private Exp exp;
    private LVal lval;
    private Number number;

    public PrimaryExp(Exp exp) {
        this.exp = exp;
        this.primaryExpType = PrimaryExpType.Exp;
    }

    public PrimaryExp(LVal lval) {
        this.lval = lval;
        this.primaryExpType = PrimaryExpType.LVal;
    }

    public PrimaryExp(Number number) {
        this.number = number;
        this.primaryExpType = PrimaryExpType.Number;
    }

    public Exp getExp() {
        return exp;
    }

    public LVal getLval() {
        return lval;
    }

    public Number getNumber() {
        return number;
    }

    public PrimaryExpType getPrimaryExpType() {
        return primaryExpType;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (primaryExpType == PrimaryExpType.Exp) {
            sb.append(TokenType.LPARENT).append(" (\n");
            sb.append(exp).append("\n");
            sb.append(TokenType.RPARENT).append(" )\n");
        } else if (primaryExpType == PrimaryExpType.LVal) {
            sb.append(lval).append("\n");
        } else {
            sb.append(number).append("\n");
        }
        sb.append("<PrimaryExp>");
        return sb.toString();
    }

    public enum PrimaryExpType {Exp, LVal, Number}
}
