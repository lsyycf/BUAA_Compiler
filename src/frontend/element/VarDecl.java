package frontend.element;

import frontend.config.*;
import frontend.utils.*;

import java.util.*;

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
        sb.append(ToString.formatComma(varDef));
        sb.append(TokenType.SEMICN).append(" ;\n");
        sb.append("<VarDecl>");
        return sb.toString();
    }

    public enum VarDeclType {
        Static, Normal
    }
}
