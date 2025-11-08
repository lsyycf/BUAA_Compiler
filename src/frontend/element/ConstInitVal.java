package frontend.element;

import frontend.config.*;

import java.util.*;

// ConstInitVal → ConstExp | <LBRACE> [ ConstExp { <COMMA> ConstExp } ] <RBRACE>
public class ConstInitVal {
    private final ArrayList<ConstExp> constExpArr = new ArrayList<>();
    private final ConstInitValType constInitValType;
    private ConstExp constExp;

    public ConstInitVal(ConstExp constExp) {
        this.constExp = constExp;
        this.constInitValType = ConstInitValType.ConstExp;
    }


    public ConstInitVal(ArrayList<ConstExp> constExpArr) {
        this.constExpArr.addAll(constExpArr);
        this.constInitValType = ConstInitValType.ConstExpArr;
    }

    public ConstExp getConstExp() {
        return constExp;
    }

    public ArrayList<ConstExp> getConstExpArr() {
        return constExpArr;
    }

    public ConstInitValType getConstInitValType() {
        return constInitValType;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (constInitValType == ConstInitValType.ConstExp) {
            sb.append(constExp).append("\n");
        } else {
            sb.append(TokenType.LBRACE).append(" {\n");
            for (int i = 0; i < constExpArr.size() - 1; i++) {
                sb.append(constExpArr.get(i)).append("\n");
                sb.append(TokenType.COMMA).append(" ,\n");
            }
            if (!constExpArr.isEmpty()) {
                sb.append(constExpArr.get(constExpArr.size() - 1)).append("\n");
            }
            sb.append(TokenType.RBRACE).append(" }\n");
        }
        sb.append("<ConstInitVal>");
        return sb.toString();
    }

    public enum ConstInitValType {ConstExp, ConstExpArr}
}
