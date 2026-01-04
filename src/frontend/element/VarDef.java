package frontend.element;

import frontend.config.*;
import frontend.lexer.*;
import frontend.utils.*;

// VarDef → <IDENFR> [ <LBRACK> ConstExp <RBRACK> ] | <IDENFR> [ <LBRACK> ConstExp <RBRACK> ] <ASSIGN> InitVal
public class VarDef implements Def {
    private final String idenfr;
    private final int lineIndex;
    private final ConstExp constExp;
    private final VarDefType varDefType;
    private InitVal initVal;

    public VarDef(Token token, ConstExp constExp) {
        this.idenfr = token.token();
        this.lineIndex = token.lineIndex();
        this.constExp = constExp;
        this.varDefType = VarDefType.Idenfr;
    }

    public VarDef(Token token, ConstExp constExp, InitVal initVal) {
        this.idenfr = token.token();
        this.lineIndex = token.lineIndex();
        this.constExp = constExp;
        this.initVal = initVal;
        this.varDefType = VarDefType.Assign;
    }

    public String getIdenfr() {
        return idenfr;
    }

    public int getLineIndex() {
        return lineIndex;
    }

    public ConstExp getConstExp() {
        return constExp;
    }

    public InitVal getInitVal() {
        return initVal;
    }

    public VarDefType getVarDefType() {
        return varDefType;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ToString.formatIdenfr(idenfr, constExp));
        if (varDefType == VarDefType.Assign) {
            sb.append(TokenType.ASSIGN).append(" =\n");
            sb.append(initVal).append("\n");
        }
        sb.append("<VarDef>");
        return sb.toString();
    }

    public enum VarDefType {
        Idenfr, Assign
    }
}
