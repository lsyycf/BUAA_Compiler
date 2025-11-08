package frontend.config;

public enum SymbolType {
    ConstInt, Int, VoidFunc, ConstIntArray, IntArray, IntFunc, StaticInt, StaticIntArray;

    @Override
    public String toString() {
        return this.name();
    }
}
