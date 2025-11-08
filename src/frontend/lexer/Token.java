package frontend.lexer;

import frontend.config.TokenType;

public record Token(TokenType type, int lineIndex, String token) {

    public String getStrCon() {
        return this.token.substring(1, token.length() - 1);
    }

    @Override
    public String toString() {
        return type.toString() + " " + token;
    }
}
