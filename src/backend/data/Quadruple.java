package backend.data;

public record Quadruple(String op, String arg1, String arg2, String result) {
    public Quadruple(String op, String arg1, String arg2, String result) {
        this.op = op;
        this.arg1 = arg1 == null ? "_" : arg1;
        this.arg2 = arg2 == null ? "_" : arg2;
        this.result = result == null ? "_" : result;
    }

    @Override
    public String toString() {
        return "(" + op + "," + arg1 + "," + arg2 + "," + result + ")";
    }
}

