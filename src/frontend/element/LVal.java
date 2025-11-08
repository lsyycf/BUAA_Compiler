package frontend.element;

import frontend.config.*;
import frontend.lexer.*;

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
        StringBuilder sb = new StringBuilder();
        sb.append(TokenType.IDENFR).append(" ").append(idenfr).append("\n");
        if (exp != null) {
            sb.append(TokenType.LBRACK).append(" [\n");
            sb.append(exp).append("\n");
            sb.append(TokenType.RBRACK).append(" ]\n");
        }
        sb.append("<LVal>");
        return sb.toString();
    }
}
