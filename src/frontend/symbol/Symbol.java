package frontend.symbol;

import frontend.config.SymbolType;

import java.util.ArrayList;

public class Symbol {
    private final String name;
    private final SymbolType type;
    private final ArrayList<SymbolType> paramTypes = new ArrayList<>();
    private final ArrayList<Integer> evaluations = new ArrayList<>();
    private final int lineNumber;

    public Symbol(String name, SymbolType type, int lineNumber) {
        this.name = name;
        this.type = type;
        this.lineNumber = lineNumber;
    }

    public ArrayList<SymbolType> getParamTypes() {
        return paramTypes;
    }

    public void addParamType(SymbolType type) {
        if (type == SymbolType.IntFunc || type == SymbolType.Int || type == SymbolType.ConstInt || type == SymbolType.StaticInt) {
            this.paramTypes.add(SymbolType.Int);
        } else {
            this.paramTypes.add(SymbolType.IntArray);
        }
    }

    public String getName() {
        return name;
    }

    public SymbolType getType() {
        return type;
    }

    public ArrayList<Integer> getEvaluations() {
        return evaluations;
    }

    public void setEvaluations(ArrayList<Integer> evaluations) {
        this.evaluations.addAll(evaluations);
    }

    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public String toString() {
        return name + " " + type;
    }
}
