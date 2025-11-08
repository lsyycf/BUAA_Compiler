package frontend.element;

// Cond → LOrExp
public record Cond(LOrExp lOrExp) {

    @Override
    public String toString() {
        return lOrExp.toString() + "\n" + "<Cond>";
    }
}
