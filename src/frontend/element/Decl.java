package frontend.element;

// Decl → ConstDecl | VarDecl
public class Decl {
    private final DeclType declType;
    private ConstDecl constDecl;
    private VarDecl varDecl;

    public Decl(ConstDecl constDecl) {
        this.constDecl = constDecl;
        this.declType = DeclType.ConstDecl;
    }

    public Decl(VarDecl varDecl) {
        this.varDecl = varDecl;
        this.declType = DeclType.VarDecl;
    }

    public ConstDecl getConstDecl() {
        return constDecl;
    }

    public VarDecl getVarDecl() {
        return varDecl;
    }

    public DeclType getDeclType() {
        return declType;
    }

    @Override
    public String toString() {
        if (declType == DeclType.ConstDecl) {
            return constDecl.toString();
        }
        return varDecl.toString();
    }

    public enum DeclType {ConstDecl, VarDecl}
}
