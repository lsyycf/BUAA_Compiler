package frontend.element;

import frontend.config.*;
import frontend.lexer.*;
import frontend.utils.*;

// ConstDef → <IDENFR> [ <LBRACK> ConstExp <RBRACK> ] <ASSIGN> ConstInitVal
public class ConstDef implements Def {
    private final String idenfr;
    private final int lineIndex;
    private final ConstExp constExp;
    private final ConstInitVal constInitVal;

    public ConstDef(Token token, ConstExp constExp, ConstInitVal constInitVal) {
        this.idenfr = token.token();
        this.lineIndex = token.lineIndex();
        this.constExp = constExp;
        this.constInitVal = constInitVal;
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

    public ConstInitVal getConstInitVal() {
        return constInitVal;
    }

    @Override
    public String toString() {
        return ToString.formatIdenfr(idenfr, constExp) + TokenType.ASSIGN + " =\n" + constInitVal + "\n" + "<ConstDef>";
    }
}
