package frontend.element;

import frontend.lexer.*;
import frontend.utils.*;

// LVal → <IDENFR> [<LBRACK> Exp <RBRACK>]
public class LVal {
    private final String idenfr;
    private final int lineindex;
    private final Exp exp;

    public LVal(Token token, Exp exp) {
        this.idenfr = token.token();
        this.lineindex = token.lineIndex();
        this.exp = exp;
    }

    public String getIdenfr() {
        return idenfr;
    }

    public int getLineIndex() {
        return lineindex;
    }

    public Exp getExp() {
        return exp;
    }

    @Override
    public String toString() {
        return ToString.formatIdenfr(idenfr, exp) + "<LVal>";
    }
}
