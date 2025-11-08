package frontend.element;

import frontend.config.*;

// FuncType → <VOIDTK> | <INTTK>
public record FuncType(frontend.element.FuncType.FuncTypeType funcTypeType) {
    @Override
    public String toString() {
        if (funcTypeType == FuncTypeType.Void) {
            return TokenType.VOIDTK + " void\n" + "<FuncType>";
        }
        return TokenType.INTTK + " int\n" + "<FuncType>";
    }

    public enum FuncTypeType {Void, Int}
}
