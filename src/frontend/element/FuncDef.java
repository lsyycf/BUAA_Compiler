package frontend.element;

import frontend.config.TokenType;
import frontend.lexer.Token;

// FuncDef → FuncType <IDENFR> <LPARENT> [FuncFParams] <RPARENT> Block
public class FuncDef {
    private final FuncType funcType;
    private final String idenfr;
    private final int lineIndex;
    private final FuncFParams funcFParams;
    private final Block block;

    public FuncDef(FuncType funcType, Token token, FuncFParams funcFParams, Block block) {
        this.funcType = funcType;
        this.idenfr = token.token();
        this.lineIndex = token.lineIndex();
        this.funcFParams = funcFParams;
        this.block = block;
    }

    public FuncType getFuncType() {
        return funcType;
    }

    public String getIdenfr() {
        return idenfr;
    }

    public int getLineIndex() {
        return lineIndex;
    }

    public FuncFParams getFuncFParams() {
        return funcFParams;
    }

    public Block getBlock() {
        return block;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(funcType).append("\n");
        sb.append(TokenType.IDENFR).append(" ").append(idenfr).append("\n");
        sb.append(TokenType.LPARENT).append(" (\n");
        if (funcFParams != null) {
            sb.append(funcFParams).append("\n");
        }
        sb.append(TokenType.RPARENT).append(" )\n");
        sb.append(block).append("\n");
        sb.append("<FuncDef>");
        return sb.toString();
    }
}
