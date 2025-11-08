package frontend.data;

import frontend.config.*;
import frontend.symbol.*;

import java.util.*;

public class SymbolTree {
    private final SymbolMap symbolMap;
    private final ArrayList<SymbolTree> subTree = new ArrayList<>();
    private final SymbolTree parentTree;
    private final HashMap<Integer, SymbolTree> symbolTreeMap = new HashMap<>();

    public SymbolTree(int scopeId, SymbolTree parentTree) {
        this.symbolMap = new SymbolMap(scopeId);
        this.parentTree = parentTree;
        this.parentTree.subTree.add(this);
    }

    public SymbolTree() {
        this.symbolMap = new SymbolMap(1);
        this.parentTree = null;
    }

    public SymbolMap getSymbolMap() {
        return symbolMap;
    }

    public SymbolTree getParentTree() {
        return parentTree;
    }

    public boolean findSymbol(String name) {
        return this.symbolMap.containsSymbol(name);
    }

    public SymbolTree getScope(int symbolId) {
        return symbolTreeMap.get(symbolId);
    }

    public void setSymbolTreeMap(HashMap<Integer, SymbolTree> symbolTreeMap) {
        symbolTreeMap.put(1, this);
        this.symbolTreeMap.putAll(symbolTreeMap);
    }

    public SymbolMap findSymbolRecursive(String name, int useLine) {
        if (name.equals("getint")) {
            Symbol symbol = new Symbol("getint", SymbolType.IntFunc, 0);
            SymbolMap symbolMap = new SymbolMap(1);
            symbolMap.addSymbol(symbol);
            return symbolMap;
        }
        SymbolTree node = this;
        while (node != null) {
            if (node.symbolMap.containsSymbol(name)) {
                Symbol symbol = node.symbolMap.getSymbol(name);
                if (useLine == -1 || symbol.getLineNumber() <= useLine) {
                    return node.symbolMap;
                }
            }
            node = node.getParentTree();
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < subTree.size() - 1; i++) {
            if (!subTree.get(i).toString().isEmpty()) {
                sb.append(subTree.get(i)).append("\n");
            }
        }
        if (!subTree.isEmpty() && !subTree.get(subTree.size() - 1).toString().isEmpty()) {
            sb.append(subTree.get(subTree.size() - 1));
        }
        if (symbolMap.toString().isEmpty() || sb.toString().isEmpty()) {
            return symbolMap + sb.toString();
        }
        return symbolMap + "\n" + sb;
    }
}
