package frontend.element;

import frontend.config.*;

// UnaryOp → <PLUS> | <MINU> | <NOT>
public record UnaryOp(frontend.element.UnaryOp.UnaryOpType unaryOpType) {
    @Override
    public String toString() {
        if (unaryOpType == UnaryOpType.Plus) {
            return TokenType.PLUS + " +\n" + "<UnaryOp>";
        } else if (unaryOpType == UnaryOpType.Minu) {
            return TokenType.MINU + " -\n" + "<UnaryOp>";
        }
        return TokenType.NOT + " !\n" + "<UnaryOp>";
    }

    public enum UnaryOpType {Plus, Minu, Not}
}
