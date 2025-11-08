package frontend.element;

import frontend.config.TokenType;
import frontend.lexer.Token;

//  FuncFParam → BType <IDENFR> [<LBRACK> <RBRACK>]
public class FuncFParam {
    private final BType bType;
    private final String idenfr;
    private final int lineIndex;
    private final FuncFParamType funcFParamType;

    public FuncFParam(BType bType, Token token, FuncFParamType funcFParamType) {
        this.bType = bType;
        this.idenfr = token.token();
        this.lineIndex = token.lineIndex();
        this.funcFParamType = funcFParamType;
    }

    public String getIdenfr() {
        return idenfr;
    }

    public int getLineIndex() {
        return lineIndex;
    }

    public FuncFParamType getFuncFParamType() {
        return funcFParamType;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(bType).append("\n");
        sb.append(TokenType.IDENFR).append(" ").append(idenfr).append("\n");
        if (funcFParamType == FuncFParamType.Array) {
            sb.append(TokenType.LBRACK).append(" [\n");
            sb.append(TokenType.RBRACK).append(" ]\n");
        }
        sb.append("<FuncFParam>");
        return sb.toString();
    }

    public enum FuncFParamType {Array, Int}
}
