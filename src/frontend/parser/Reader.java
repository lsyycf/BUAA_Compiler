package frontend.parser;

import frontend.config.*;
import frontend.data.*;
import frontend.lexer.*;

public class Reader {
    private final TokenList tokenList;
    private int pos = 0;

    public Reader(TokenList tokenList) {
        this.tokenList = tokenList;
    }

    public void consume(TokenType type) {
        if (notEnd() && type == peek(0).type()) {
            pos += 1;
        }
    }

    public Token peek(int steps) {
        if (pos + steps >= tokenList.length() || pos + steps < 0) {
            return null;
        }
        return tokenList.getToken(pos + steps);
    }

    public int getLineIndex() {
        return peek(-1).lineIndex();
    }

    public boolean notEnd() {
        return pos < tokenList.length();
    }
}
