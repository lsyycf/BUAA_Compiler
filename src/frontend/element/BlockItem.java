package frontend.element;

// BlockItem → Decl | Stmt
public class BlockItem {
    private final BlockItemType blockItemType;
    private Decl decl;
    private Stmt stmt;

    public BlockItem(Decl decl) {
        this.decl = decl;
        this.blockItemType = BlockItemType.Decl;
    }

    public BlockItem(Stmt stmt) {
        this.stmt = stmt;
        this.blockItemType = BlockItemType.Stmt;
    }

    public Decl getDecl() {
        return decl;
    }

    public Stmt getStmt() {
        return stmt;
    }

    public BlockItemType getBlockItemType() {
        return blockItemType;
    }

    @Override
    public String toString() {
        if (blockItemType == BlockItemType.Decl) {
            return decl.toString();
        }
        return stmt.toString();
    }

    public enum BlockItemType {Decl, Stmt}
}
