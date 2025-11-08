package frontend.element;

import frontend.config.TokenType;

import java.util.ArrayList;

// VarDecl → [ <STATICTK> ] BType VarDef { <COMMA> VarDef } <SEMICN>
public class VarDecl {
    private final BType bType;
    private final ArrayList<VarDef> varDef = new ArrayList<>();
    private final VarDeclType varDeclType;

    public VarDecl(VarDeclType varDeclType, BType bType, ArrayList<VarDef> varDef) {
        this.varDeclType = varDeclType;
        this.bType = bType;
        this.varDef.addAll(varDef);
    }

    public ArrayList<VarDef> getVarDef() {
        return varDef;
    }

    public VarDeclType getVarDeclType() {
        return varDeclType;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (varDeclType == VarDeclType.Static) {
            sb.append(TokenType.STATICTK).append(" static\n");
        }
        sb.append(bType).append("\n");
        for (int i = 0; i < varDef.size() - 1; i++) {
            sb.append(varDef.get(i)).append("\n");
            sb.append(TokenType.COMMA).append(" ,\n");
        }
        if (!varDef.isEmpty()) {
            sb.append(varDef.get(varDef.size() - 1)).append("\n");
        }
        sb.append(TokenType.SEMICN).append(" ;\n");
        sb.append("<VarDecl>");
        return sb.toString();
    }

    public enum VarDeclType {Static, Normal}
}
