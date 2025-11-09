package frontend.element;

import frontend.utils.*;

import java.util.*;

// FuncFParams → FuncFParam { <COMMA> FuncFParam }
public class FuncFParams {
    private final ArrayList<FuncFParam> funcFParam = new ArrayList<>();

    public FuncFParams(ArrayList<FuncFParam> funcFParam) {
        this.funcFParam.addAll(funcFParam);
    }

    public ArrayList<FuncFParam> getFuncFParam() {
        return funcFParam;
    }

    @Override
    public String toString() {
        return ToString.formatComma(funcFParam) + "<FuncFParams>";
    }
}
