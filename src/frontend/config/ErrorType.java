package frontend.config;


public enum ErrorType {
    ILLEGAL_CHAR("a"),
    NAME_REDEFINITION("b"),
    UNDEFINED_NAME("c"),
    PARAM_COUNT_MISMATCH("d"),
    PARAM_TYPE_MISMATCH("e"),
    INVALID_RETURN("f"),
    MISSING_RETURN("g"),
    CONSTANT_MODIFICATION("h"),
    MISSING_SEMICOLON("i"),
    MISSING_RIGHT_PARENTHESIS("j"),
    MISSING_RIGHT_BRACKET("k"),
    PRINTF_FORMAT_MISMATCH("l"),
    BREAK_CONTINUE_OUT_LOOP("m");

    private final String code;
    private final boolean isFatal;

    ErrorType(String code) {
        this.code = code;
        isFatal = !code.equals("a") && !code.equals("f") && !code.equals("g") && !code.equals("i") && !code.equals("j") && !code.equals("k") && !code.equals("m");
    }

    public boolean isFatal() {
        return isFatal;
    }

    @Override
    public String toString() {
        return this.code;
    }
}
