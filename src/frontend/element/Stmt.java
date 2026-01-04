package frontend.element;

import frontend.config.*;
import frontend.lexer.*;

import java.util.*;

// Stmt → LVal <ASSIGN> Exp <SEMICN> // i
// | [Exp] <SEMICN> // i
// | Block
// | <IFTK> <LPARENT> Cond <RPARENT> Stmt [ <ELSETK> Stmt ] // j
// | <FORTK> <LPARENT> [ForStmt] <SEMICN> [Cond] <SEMICN> [ForStmt] <RPARENT> Stmt
// | <BREAKTK> <SEMICN>
// | <CONTINUETK> <SEMICN> // i
// | <RETURNTK> [Exp] <SEMICN> // i
// | <PRINTFTK> <LPARENT> <STRCON> { <COMMA> Exp } <RPARENT> <SEMICN> // i j
// | <WHILETK> <LPARENT> Cond <RPARENT> Stmt
public class Stmt {
    private final ArrayList<Exp> expPrint = new ArrayList<>();
    private LVal lVal;
    private Exp expLVal;
    private Exp exp;
    private Block block;
    private Cond condIf;
    private Stmt stmtIf;
    private Stmt stmtElse;
    private ForStmt forStmtLeft;
    private Cond condFor;
    private ForStmt forStmtRight;
    private Stmt stmtFor;
    private Exp expReturn;
    private String strCon;
    private Cond condWhile;
    private Stmt stmtWhile;
    private int lineIndex;
    private StmtType stmtType;

    public Stmt(LVal lval, Exp exp) {
        this.lVal = lval;
        this.expLVal = exp;
        this.stmtType = StmtType.LVal;
    }

    public Stmt(Exp exp, Token token) {
        this.expReturn = exp;
        this.stmtType = StmtType.Return;
        this.lineIndex = token.lineIndex();
    }

    public Stmt(Exp exp) {
        this.exp = exp;
        this.stmtType = StmtType.Exp;
    }

    public Stmt(Block block) {
        this.block = block;
        this.stmtType = StmtType.Block;
    }

    public Stmt(Cond cond, Stmt stmt, Stmt stmtElse) {
        this.condIf = cond;
        this.stmtIf = stmt;
        this.stmtElse = stmtElse;
        this.stmtType = StmtType.If;
    }

    public Stmt(ForStmt forStmtLeft, Cond cond, ForStmt forStmtRight, Stmt stmt) {
        this.forStmtLeft = forStmtLeft;
        this.condFor = cond;
        this.forStmtRight = forStmtRight;
        this.stmtFor = stmt;
        this.stmtType = StmtType.For;
    }

    public Stmt(Cond cond, Stmt stmt) {
        this.condWhile = cond;
        this.stmtWhile = stmt;
        this.stmtType = StmtType.While;
    }

    public Stmt(Token token) {
        if (token.type() == TokenType.BREAKTK) {
            this.stmtType = StmtType.Break;
        } else {
            this.stmtType = StmtType.Continue;
        }
        this.lineIndex = token.lineIndex();
    }

    public Stmt(Token token, String strCon, ArrayList<Exp> expPrint) {
        this.strCon = strCon;
        this.expPrint.addAll(expPrint);
        this.stmtType = StmtType.Print;
        this.lineIndex = token.lineIndex();
    }

    public LVal getlVal() {
        return lVal;
    }

    public Exp getExpLVal() {
        return expLVal;
    }

    public Exp getExp() {
        return exp;
    }

    public Block getBlock() {
        return block;
    }

    public Cond getCondIf() {
        return condIf;
    }

    public Stmt getStmtIf() {
        return stmtIf;
    }

    public Stmt getStmtElse() {
        return stmtElse;
    }

    public ForStmt getForStmtLeft() {
        return forStmtLeft;
    }

    public Cond getCondFor() {
        return condFor;
    }

    public ForStmt getForStmtRight() {
        return forStmtRight;
    }

    public Stmt getStmtFor() {
        return stmtFor;
    }

    public Exp getExpReturn() {
        return expReturn;
    }

    public String getStrCon() {
        return strCon;
    }

    public ArrayList<Exp> getExpPrint() {
        return expPrint;
    }

    public StmtType getStmtType() {
        return stmtType;
    }

    public int getLineIndex() {
        return lineIndex;
    }

    public void clearExpReturn() {
        this.expReturn = null;
    }

    public Cond getCondWhile() {
        return condWhile;
    }

    public Stmt getStmtWhile() {
        return stmtWhile;
    }

    public void clear() {
        this.stmtType = StmtType.Exp;
        this.exp = null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        switch (stmtType) {
            case Block -> sb.append(block).append("\n");
            case Exp -> {
                if (exp != null) {
                    sb.append(exp).append("\n");
                }
                sb.append(TokenType.SEMICN).append(" ;\n");
            }
            case If -> {
                sb.append(TokenType.IFTK).append(" if\n");
                sb.append(TokenType.LPARENT).append(" (\n");
                sb.append(condIf).append("\n");
                sb.append(TokenType.RPARENT).append(" )\n");
                sb.append(stmtIf).append("\n");
                if (stmtElse != null) {
                    sb.append(TokenType.ELSETK).append(" else\n");
                    sb.append(stmtElse).append("\n");
                }
            }
            case For -> {
                sb.append(TokenType.FORTK).append(" for\n");
                sb.append(TokenType.LPARENT).append(" (\n");
                if (forStmtLeft != null) {
                    sb.append(forStmtLeft).append("\n");
                }
                sb.append(TokenType.SEMICN).append(" ;\n");
                if (condFor != null) {
                    sb.append(condFor).append("\n");
                }
                sb.append(TokenType.SEMICN).append(" ;\n");
                if (forStmtRight != null) {
                    sb.append(forStmtRight).append("\n");
                }
                sb.append(TokenType.RPARENT).append(" )\n");
                sb.append(stmtFor).append("\n");
            }
            case LVal -> {
                sb.append(lVal).append("\n");
                sb.append(TokenType.ASSIGN).append(" =\n");
                sb.append(expLVal).append("\n");
                sb.append(TokenType.SEMICN).append(" ;\n");
            }
            case Break -> {
                sb.append(TokenType.BREAKTK).append(" break\n");
                sb.append(TokenType.SEMICN).append(" ;\n");
            }
            case Continue -> {
                sb.append(TokenType.CONTINUETK).append(" continue\n");
                sb.append(TokenType.SEMICN).append(" ;\n");
            }
            case Print -> {
                sb.append(TokenType.PRINTFTK).append(" printf\n");
                sb.append(TokenType.LPARENT).append(" (\n");
                sb.append(TokenType.STRCON).append(" \"").append(strCon).append("\"\n");
                for (Exp exp : expPrint) {
                    sb.append(TokenType.COMMA).append(" ,\n");
                    sb.append(exp).append("\n");
                }
                sb.append(TokenType.RPARENT).append(" )\n");
                sb.append(TokenType.SEMICN).append(" ;\n");
            }
            case Return -> {
                sb.append(TokenType.RETURNTK).append(" return\n");
                if (expReturn != null) {
                    sb.append(expReturn).append("\n");
                }
                sb.append(TokenType.SEMICN).append(" ;\n");
            }
            case While -> {
                sb.append(TokenType.WHILETK).append(" while\n");
                sb.append(TokenType.LPARENT).append(" (\n");
                sb.append(condWhile).append("\n");
                sb.append(TokenType.RPARENT).append(" )\n");
                sb.append(stmtWhile).append("\n");
            }
        }
        sb.append("<Stmt>");
        return sb.toString();
    }

    public enum StmtType {LVal, Exp, Block, If, For, Break, Continue, Return, Print, While}
}
