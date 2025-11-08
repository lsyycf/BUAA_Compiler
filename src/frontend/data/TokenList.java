package frontend.data;

import frontend.lexer.Token;

import java.util.ArrayList;

public class TokenList {
    private final ArrayList<Token> tokenList = new ArrayList<>();

    public void addToken(Token token) {
        this.tokenList.add(token);
    }

    public Token getToken(int pos) {
        return tokenList.get(pos);
    }

    public int length() {
        return tokenList.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokenList.size() - 1; i++) {
            sb.append(tokenList.get(i)).append("\n");
        }
        sb.append(tokenList.get(tokenList.size() - 1));
        return sb.toString();
    }
}
