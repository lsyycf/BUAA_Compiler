package frontend.lexer;

import frontend.config.*;
import frontend.data.*;

import java.util.*;
import java.util.regex.*;

public class Lexer {
    private final Reader reader;
    private final TokenList tokenList = new TokenList();
    private final ErrorList errorList = new ErrorList();

    public Lexer(ArrayList<String> lines) {
        this.reader = new Reader(lines);
    }

    public TokenList tokenizer() {
        while (!this.reader.endOfFile()) {
            reader.skip();
            if (isComment()) {
                continue;
            }
            addToken();
        }
        return tokenList;
    }

    private boolean isComment() {
        if (this.reader.peekStr(2).equals("//")) {
            this.reader.nextLine();
            return true;
        } else if (this.reader.peekStr(2).equals("/*")) {
            reader.consume(2);
            while (!this.reader.endOfFile() && !this.reader.peekStr(2).equals("*/")) {
                this.reader.consume(1);
            }
            if (this.reader.peekStr(2).equals("*/")) {
                this.reader.consume(2);
                return true;
            }
        }
        return false;
    }

    private void addToken() {
        for (TokenType tokenType : TokenType.values()) {
            Pattern pattern = tokenType.getPattern();
            String tokenString = this.reader.matchStr(pattern);
            if (tokenString != null) {
                Token token = new Token(tokenType, reader.getLineIndex(), tokenString);
                this.reader.consume(tokenString.length());
                this.tokenList.addToken(token);
                return;
            }
        }
        if (!reader.endOfFile()) {
            errorList.addError(reader.getLineIndex(), ErrorType.ILLEGAL_CHAR);
            if (reader.peekStr(1).equals("|")) {
                tokenList.addToken(new Token(TokenType.OR, reader.getLineIndex(), "||"));
            } else if (reader.peekStr(1).equals("&")) {
                tokenList.addToken(new Token(TokenType.AND, reader.getLineIndex(), "&&"));
            }
            reader.consume(1);
        }
    }

    public TokenList getTokenList() {
        return tokenList;
    }

    public ErrorList getErrorList() {
        return errorList;
    }
}
