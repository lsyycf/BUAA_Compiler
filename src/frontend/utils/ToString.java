package frontend.utils;

import frontend.config.*;

import java.util.*;

public class ToString {
    public static <T> String formatComma(ArrayList<T> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size() - 1; i++) {
            sb.append(list.get(i)).append("\n");
            sb.append(TokenType.COMMA).append(" ,\n");
        }
        if (!list.isEmpty()) {
            sb.append(list.get(list.size() - 1)).append("\n");
        }
        return sb.toString();
    }

    public static String formatIdenfr(String idenfr, Object exp) {
        StringBuilder sb = new StringBuilder();
        sb.append(TokenType.IDENFR).append(" ").append(idenfr).append("\n");
        if (exp != null) {
            sb.append(TokenType.LBRACK).append(" [\n");
            sb.append(exp).append("\n");
            sb.append(TokenType.RBRACK).append(" ]\n");
        }
        return sb.toString();
    }

    public static <T> String formatBrace(ArrayList<T> list) {
        return TokenType.LBRACE + " {\n" + formatComma(list) + TokenType.RBRACE + " }\n";
    }

    public static String formatFuncCall(String idenfr, Object params) {
        StringBuilder sb = new StringBuilder();
        sb.append(TokenType.IDENFR).append(" ").append(idenfr).append("\n");
        sb.append(TokenType.LPARENT).append(" (\n");
        if (params != null) {
            sb.append(params).append("\n");
        }
        sb.append(TokenType.RPARENT).append(" )\n");
        return sb.toString();
    }
}
