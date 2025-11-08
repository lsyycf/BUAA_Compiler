package frontend.element;

import java.util.*;

// CompUnit → {Decl} {FuncDef} MainFuncDef
public class CompUnit {
    private final ArrayList<Decl> decl = new ArrayList<>();
    private final ArrayList<FuncDef> funcDef = new ArrayList<>();
    private final MainFuncDef mainFuncDef;

    public CompUnit(ArrayList<Decl> decl, ArrayList<FuncDef> funcDef, MainFuncDef mainFuncDef) {
        this.decl.addAll(decl);
        this.funcDef.addAll(funcDef);
        this.mainFuncDef = mainFuncDef;
    }

    public ArrayList<Decl> getDecl() {
        return decl;
    }

    public ArrayList<FuncDef> getFuncDef() {
        return funcDef;
    }

    public MainFuncDef getMainFuncDef() {
        return mainFuncDef;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Decl decl : decl) {
            sb.append(decl).append("\n");
        }
        for (FuncDef funcDef : funcDef) {
            sb.append(funcDef).append("\n");
        }
        sb.append(mainFuncDef).append("\n");
        sb.append("<CompUnit>");
        return sb.toString();
    }
}
