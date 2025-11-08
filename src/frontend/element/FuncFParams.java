package frontend.element;

import frontend.config.TokenType;

import java.util.ArrayList;

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
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < funcFParam.size() - 1; i++) {
            sb.append(funcFParam.get(i)).append("\n");
            sb.append(TokenType.COMMA).append(" ,\n");
        }
        if (!funcFParam.isEmpty()) {
            sb.append(funcFParam.get(funcFParam.size() - 1)).append("\n");
        }
        sb.append("<FuncFParams>");
        return sb.toString();
    }
}
