package frontend.element;

import frontend.utils.*;

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
            sb.append(ToString.formatBrace(constExpArr));
        }
        sb.append("<ConstInitVal>");
        return sb.toString();
    }

    public enum ConstInitValType {ConstExp, ConstExpArr}
}
