package frontend.element;

// Exp → AddExp
public record Exp(AddExp addExp) {

    @Override
    public String toString() {
        return addExp + "\n" + "<Exp>";
    }
}
