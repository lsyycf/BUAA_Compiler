package frontend.config;

import java.util.regex.Pattern;

public enum TokenType {
    CONSTTK(true, "const"),
    INTTK(true, "int"),
    STATICTK(true, "static"),
    BREAKTK(true, "break"),
    CONTINUETK(true, "continue"),
    IFTK(true, "if"),
    MAINTK(true, "main"),
    ELSETK(true, "else"),
    FORTK(true, "for"),
    RETURNTK(true, "return"),
    VOIDTK(true, "void"),
    PRINTFTK(true, "printf"),
    AND(false, "&&"),
    OR(false, "\\|\\|"),
    LEQ(false, "<="),
    GEQ(false, ">="),
    EQL(false, "=="),
    NEQ(false, "!="),
    PLUS(false, "\\+"),
    MINU(false, "-"),
    MULT(false, "\\*"),
    DIV(false, "/"),
    MOD(false, "%"),
    LSS(false, "<"),
    GRE(false, ">"),
    NOT(false, "!"),
    ASSIGN(false, "="),
    SEMICN(false, ";"),
    COMMA(false, ","),
    LPARENT(false, "\\("),
    RPARENT(false, "\\)"),
    LBRACK(false, "\\["),
    RBRACK(false, "]"),
    LBRACE(false, "\\{"),
    RBRACE(false, "}"),
    INTCON(false, "[0-9]+"),
    IDENFR(false, "[_A-Za-z][_A-Za-z0-9]*"),
    STRCON(false, "\\\"[^\\\"]*\\\"");

    private final Pattern pattern;

    TokenType(boolean fullMatch, String patternString) {
        if (fullMatch) {
            this.pattern = Pattern.compile("^" + patternString + "(?![_A-Za-z0-9])");
        } else {
            this.pattern = Pattern.compile("^" + patternString);
        }
    }

    public Pattern getPattern() {
        return this.pattern;
    }

    @Override
    public String toString() {
        return this.name();
    }
}