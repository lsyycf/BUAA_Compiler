package frontend.element;

import frontend.config.TokenType;

// MainFuncDef → <INTTK> <MAINTK> <LPARENT> <RPARENT> Block
public record MainFuncDef(Block block) {

    @Override
    public String toString() {
        return TokenType.INTTK + " int\n" + TokenType.MAINTK + " main\n" + TokenType.LPARENT + " (\n" + TokenType.RPARENT + " )\n" + block + "\n" + "<MainFuncDef>";
    }
}
