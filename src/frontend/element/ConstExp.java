package frontend.element;

// ConstExp → AddExp
public record ConstExp(AddExp addExp) {

    @Override
    public String toString() {
        return addExp + "\n" + "<ConstExp>";
    }
}
