package frontend.data;

import frontend.symbol.Symbol;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class SymbolMap {
    private final int scopeId;
    private final LinkedHashMap<String, Symbol> symbolMap = new LinkedHashMap<>();

    public SymbolMap(int scopeId) {
        this.scopeId = scopeId;
    }

    public void addSymbol(Symbol symbol) {
        symbolMap.put(symbol.getName(), symbol);
    }

    public boolean containsSymbol(String name) {
        return symbolMap.containsKey(name);
    }

    public Symbol getSymbol(String name) {
        return symbolMap.get(name);
    }

    public int getScopeId() {
        return scopeId;
    }

    public boolean isEmpty() {
        return symbolMap.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        ArrayList<Symbol> symbols = new ArrayList<>(symbolMap.values());
        for (int i = 0; i < symbols.size() - 1; i++) {
            sb.append(scopeId).append(" ").append(symbols.get(i)).append("\n");
        }
        if (!isEmpty()) {
            sb.append(scopeId).append(" ").append(symbols.get(symbolMap.size() - 1));
        }
        return sb.toString();
    }
}
