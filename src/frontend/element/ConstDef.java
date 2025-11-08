package frontend.element;

import frontend.config.TokenType;
import frontend.lexer.Token;

// ConstDef → <IDENFR> [ <LBRACK> ConstExp <RBRACK> ] <ASSIGN> ConstInitVal
public class ConstDef {
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
        StringBuilder sb = new StringBuilder();
        sb.append(TokenType.IDENFR).append(" ").append(idenfr).append("\n");
        if (constExp != null) {
            sb.append(TokenType.LBRACK).append(" [\n");
            sb.append(constExp).append("\n");
            sb.append(TokenType.RBRACK).append(" ]\n");
        }
        sb.append(TokenType.ASSIGN).append(" =\n");
        sb.append(constInitVal).append("\n");
        sb.append("<ConstDef>");
        return sb.toString();
    }
}
