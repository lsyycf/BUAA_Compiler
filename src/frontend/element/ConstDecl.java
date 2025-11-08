package frontend.element;

import frontend.config.*;

import java.util.*;

// ConstDecl → <CONSTTK> BType ConstDef { <COMMA> ConstDef } <SEMICN>
public class ConstDecl {
    private final BType bType;
    private final ArrayList<ConstDef> constDef = new ArrayList<>();

    public ConstDecl(BType bType, ArrayList<ConstDef> constDef) {
        this.bType = bType;
        this.constDef.addAll(constDef);
    }

    public ArrayList<ConstDef> getConstDef() {
        return constDef;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(TokenType.CONSTTK).append(" const\n");
        sb.append(bType).append("\n");
        for (int i = 0; i < constDef.size() - 1; i++) {
            sb.append(constDef.get(i)).append("\n");
            sb.append(TokenType.COMMA).append(" ,\n");
        }
        if (!constDef.isEmpty()) {
            sb.append(constDef.get(constDef.size() - 1)).append("\n");
        }
        sb.append(TokenType.SEMICN).append(" ;\n");
        sb.append("<ConstDecl>");
        return sb.toString();
    }
}
