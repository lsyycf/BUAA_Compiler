package frontend.element;

import frontend.config.TokenType;
import frontend.lexer.Token;

import java.util.ArrayList;

// Block → <LBRACE> { BlockItem } <RBRACE>
public class Block {
    private final ArrayList<BlockItem> blockItem = new ArrayList<>();
    private final int lineIndex;

    public Block(ArrayList<BlockItem> blockItem, Token token) {
        this.blockItem.addAll(blockItem);
        this.lineIndex = token.lineIndex();
    }

    public ArrayList<BlockItem> getBlockItem() {
        return blockItem;
    }

    public int getLineIndex() {
        return lineIndex;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(TokenType.LBRACE).append(" {\n");
        for (BlockItem blockItem : blockItem) {
            sb.append(blockItem).append("\n");
        }
        sb.append(TokenType.RBRACE).append(" }\n");
        sb.append("<Block>");
        return sb.toString();
    }
}
