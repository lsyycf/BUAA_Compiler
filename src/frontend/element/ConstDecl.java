package frontend.element;

import frontend.config.*;
import frontend.utils.*;

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
        return TokenType.CONSTTK + " const\n" + bType + "\n" + ToString.formatComma(constDef) + TokenType.SEMICN + " ;\n" + "<ConstDecl>";
    }
}
