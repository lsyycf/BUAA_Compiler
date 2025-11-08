package frontend.element;

import frontend.config.*;

// Number → <INTCON>
public record Number(String number) {

    @Override
    public String toString() {
        return TokenType.INTCON + " " + number + "\n" + "<Number>";
    }
}
