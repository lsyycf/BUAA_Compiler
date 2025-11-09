package frontend.element;

import frontend.lexer.*;
import frontend.utils.*;

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
        if (unaryExpType == UnaryExpType.PrimaryExp) {
            return primaryExp + "\n<UnaryExp>";
        } else if (unaryExpType == UnaryExpType.FuncRParams) {
            return ToString.formatFuncCall(idenfr, funcRParams) + "<UnaryExp>";
        } else {
            return unaryOp + "\n" + unaryExp + "\n<UnaryExp>";
        }
    }

    public enum UnaryExpType {
        PrimaryExp, FuncRParams, UnaryOp
    }
}
