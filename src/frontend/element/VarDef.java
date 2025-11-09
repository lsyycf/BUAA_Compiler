package frontend.element;

import frontend.config.*;
import frontend.lexer.*;
import frontend.utils.*;

// VarDef → <IDENFR> [ <LBRACK> ConstExp <RBRACK> ] | <IDENFR> [ <LBRACK> ConstExp <RBRACK> ] <ASSIGN> InitVal
public class VarDef implements Def {
    private final String idenfr;
    private final int lineIndex;
    private final ConstExp constExp;
    private final VatDefType vatDefType;
    private InitVal initVal;

    public VarDef(Token token, ConstExp constExp) {
        this.idenfr = token.token();
        this.lineIndex = token.lineIndex();
        this.constExp = constExp;
        this.vatDefType = VatDefType.Idenfr;
    }

    public VarDef(Token token, ConstExp constExp, InitVal initVal) {
        this.idenfr = token.token();
        this.lineIndex = token.lineIndex();
        this.constExp = constExp;
        this.initVal = initVal;
        this.vatDefType = VatDefType.Assign;
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

    public VatDefType getVatDefType() {
        return vatDefType;
    }

    @Override
    public String toString() {
        // <IDENFR> [ <LBRACK> ConstExp <RBRACK> ] | <IDENFR> [ <LBRACK> ConstExp
        // <RBRACK> ] <ASSIGN> InitVal
        StringBuilder sb = new StringBuilder();
        sb.append(ToString.formatIdenfr(idenfr, constExp));
        if (vatDefType == VatDefType.Assign) {
            sb.append(TokenType.ASSIGN).append(" =\n");
            sb.append(initVal).append("\n");
        }
        sb.append("<VarDef>");
        return sb.toString();
    }

    public enum VatDefType {
        Idenfr, Assign
    }
}
