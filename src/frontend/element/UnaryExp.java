package frontend.element;

import frontend.config.*;
import frontend.lexer.*;

// UnaryExp → PrimaryExp | <IDENFR> <LPARENT> [FuncRParams] <RPARENT> | UnaryOp UnaryExp
public class UnaryExp {
    private final UnaryExpType unaryExpType;
    private PrimaryExp primaryExp;
    private String idenfr;
    private int lineIndex;
    private FuncRParams funcRParams;
    private UnaryOp unaryOp;
    private UnaryExp unaryExp;

    public UnaryExp(PrimaryExp primaryExp) {
        this.primaryExp = primaryExp;
        this.unaryExpType = UnaryExpType.PrimaryExp;
    }

    public UnaryExp(Token token, FuncRParams funcRParams) {
        this.idenfr = token.token();
        this.lineIndex = token.lineIndex();
        this.funcRParams = funcRParams;
        this.unaryExpType = UnaryExpType.FuncRParams;
    }

    public UnaryExp(UnaryOp unaryOp, UnaryExp unaryExp) {
        this.unaryOp = unaryOp;
        this.unaryExp = unaryExp;
        this.unaryExpType = UnaryExpType.UnaryOp;
    }

    public PrimaryExp getPrimaryExp() {
        return primaryExp;
    }

    public String getIdenfr() {
        return idenfr;
    }

    public int getLineIndex() {
        return lineIndex;
    }

    public FuncRParams getFuncRParams() {
        return funcRParams;
    }

    public UnaryOp getUnaryOp() {
        return unaryOp;
    }

    public UnaryExp getUnaryExp() {
        return unaryExp;
    }

    public UnaryExpType getUnaryExpType() {
        return unaryExpType;
    }

    @Override
    public String toString() {
        // UnaryExp → PrimaryExp | <IDENFR> <LPARENT> [FuncRParams] <RPARENT> | UnaryOp UnaryExp
        StringBuilder sb = new StringBuilder();
        if (unaryExpType == UnaryExpType.PrimaryExp) {
            sb.append(primaryExp).append("\n");
        } else if (unaryExpType == UnaryExpType.FuncRParams) {
            sb.append(TokenType.IDENFR).append(" ").append(idenfr).append("\n");
            sb.append(TokenType.LPARENT).append(" (\n");
            if (funcRParams != null) {
                sb.append(funcRParams).append("\n");
            }
            sb.append(TokenType.RPARENT).append(" )\n");
        } else {
            sb.append(unaryOp).append("\n");
            sb.append(unaryExp).append("\n");
        }
        sb.append("<UnaryExp>");
        return sb.toString();
    }

    public enum UnaryExpType {PrimaryExp, FuncRParams, UnaryOp}
}
