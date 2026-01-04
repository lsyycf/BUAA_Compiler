package frontend.parser;

import frontend.config.*;
import frontend.data.*;
import frontend.element.*;
import frontend.element.Number;
import frontend.lexer.*;

import java.util.*;

public class Parser {
    private final Reader reader;
    private final ErrorList errorList = new ErrorList();

    public Parser(TokenList tokenList) {
        this.reader = new Reader(tokenList);
    }

    // CompUnit → {Decl} {FuncDef} MainFuncDef
    public CompUnit parseCompUnit() {
        ArrayList<Decl> decls = parseDecls();
        ArrayList<FuncDef> funcDefs = parseFuncDefs();
        MainFuncDef mainFuncDef;
        mainFuncDef = parseMainFuncDef();
        return new CompUnit(decls, funcDefs, mainFuncDef);
    }

    private ArrayList<Decl> parseDecls() {
        ArrayList<Decl> decls = new ArrayList<>();
        while (true) {
            Token t1 = reader.peek(0);
            Token t2 = reader.peek(1);
            Token t3 = reader.peek(2);
            if (t1 == null || t2 == null || t3 == null) break;
            if (t1.type() == TokenType.CONSTTK) {
                decls.add(parseDecl());
            } else if (t1.type() == TokenType.INTTK && t2.type() == TokenType.IDENFR && t3.type() != TokenType.LPARENT) {
                decls.add(parseDecl());
            } else if (t1.type() == TokenType.STATICTK) {
                decls.add(parseDecl());
            } else {
                break;
            }
        }
        return decls;
    }

    private ArrayList<FuncDef> parseFuncDefs() {
        ArrayList<FuncDef> funcDefs = new ArrayList<>();
        while (true) {
            Token t1 = reader.peek(0);
            Token t2 = reader.peek(1);
            if (t1 == null || t2 == null) break;
            if ((t1.type() == TokenType.INTTK || t1.type() == TokenType.VOIDTK) && t2.type() == TokenType.IDENFR) {
                funcDefs.add(parseFuncDef());
            } else {
                break;
            }
        }
        return funcDefs;
    }

    private void consumeError(TokenType tokenType) {
        ErrorType errorType = switch (tokenType) {
            case SEMICN -> ErrorType.MISSING_SEMICOLON;
            case RPARENT -> ErrorType.MISSING_RIGHT_PARENTHESIS;
            case RBRACK -> ErrorType.MISSING_RIGHT_BRACKET;
            default -> null;
        };
        if (reader.peek(0).type() != tokenType) {
            errorList.addError(reader.getLineIndex(), errorType);
        } else {
            reader.consume(tokenType);
        }
    }

    // Decl → ConstDecl | VarDecl
    private Decl parseDecl() {
        Token t = reader.peek(0);
        if (t.type() == TokenType.CONSTTK) {
            return new Decl(parseConstDecl());
        } else {
            return new Decl(parseVarDecl());
        }
    }

    // ConstDecl → <CONSTTK> BType ConstDef { <COMMA> ConstDef } <SEMICN>
    private ConstDecl parseConstDecl() {
        reader.consume(TokenType.CONSTTK);
        BType bType = parseBType();
        ArrayList<ConstDef> constDefs = new ArrayList<>();
        constDefs.add(parseConstDef());
        while (reader.notEnd() && reader.peek(0).type() == TokenType.COMMA) {
            reader.consume(TokenType.COMMA);
            constDefs.add(parseConstDef());
        }
        consumeError(TokenType.SEMICN);
        return new ConstDecl(bType, constDefs);
    }

    // BType → <INTTK>
    private BType parseBType() {
        reader.consume(TokenType.INTTK);
        return new BType();
    }

    // ConstDef → <IDENFR> [ <LBRACK> ConstExp <RBRACK> ] <ASSIGN> ConstInitVal
    private ConstDef parseConstDef() {
        Token idenfr = reader.peek(0);
        reader.consume(TokenType.IDENFR);
        ConstExp constExp = parseIndex();
        reader.consume(TokenType.ASSIGN);
        ConstInitVal constInitVal = parseConstInitVal();
        return new ConstDef(idenfr, constExp, constInitVal);
    }

    // ConstInitVal → ConstExp | <LBRACE> [ ConstExp { <COMMA> ConstExp } ] <RBRACE>
    private ConstInitVal parseConstInitVal() {
        if (reader.peek(0).type() == TokenType.LBRACE) {
            reader.consume(TokenType.LBRACE);
            ArrayList<ConstExp> constExps = new ArrayList<>();
            if (reader.peek(0).type() != TokenType.RBRACE) {
                constExps.add(parseConstExp());
                while (reader.peek(0).type() == TokenType.COMMA) {
                    reader.consume(TokenType.COMMA);
                    constExps.add(parseConstExp());
                }
            }
            reader.consume(TokenType.RBRACE);
            return new ConstInitVal(constExps);
        } else {
            return new ConstInitVal(parseConstExp());
        }
    }

    // VarDecl → [ <STATICTK> ] BType VarDef { <COMMA> VarDef } <SEMICN>
    private VarDecl parseVarDecl() {
        VarDecl.VarDeclType varDeclType = VarDecl.VarDeclType.Normal;
        if (reader.peek(0).type() == TokenType.STATICTK) {
            reader.consume(TokenType.STATICTK);
            varDeclType = VarDecl.VarDeclType.Static;
        }
        BType bType = parseBType();
        ArrayList<VarDef> varDefs = new ArrayList<>();
        varDefs.add(parseVarDef());
        while (reader.notEnd() && reader.peek(0).type() == TokenType.COMMA) {
            reader.consume(TokenType.COMMA);
            varDefs.add(parseVarDef());
        }
        consumeError(TokenType.SEMICN);
        return new VarDecl(varDeclType, bType, varDefs);
    }

    // VarDef → <IDENFR> [ <LBRACK> ConstExp <RBRACK> ] | <IDENFR> [ <LBRACK> ConstExp <RBRACK> ] <ASSIGN> InitVal
    private VarDef parseVarDef() {
        Token idenfr = reader.peek(0);
        reader.consume(TokenType.IDENFR);
        ConstExp constExp = parseIndex();
        if (reader.peek(0).type() == TokenType.ASSIGN) {
            reader.consume(TokenType.ASSIGN);
            InitVal initVal = parseInitVal();
            return new VarDef(idenfr, constExp, initVal);
        }
        return new VarDef(idenfr, constExp);
    }

    private ConstExp parseIndex() {
        ConstExp constExp = null;
        if (reader.peek(0).type() == TokenType.LBRACK) {
            reader.consume(TokenType.LBRACK);
            constExp = parseConstExp();
            consumeError(TokenType.RBRACK);
        }
        return constExp;
    }

    // InitVal → Exp | <LBRACE> [ Exp { <COMMA> Exp } ] <RBRACE>
    private InitVal parseInitVal() {
        if (reader.peek(0).type() == TokenType.LBRACE) {
            reader.consume(TokenType.LBRACE);
            ArrayList<Exp> exps = new ArrayList<>();
            if (reader.peek(0).type() != TokenType.RBRACE) {
                exps = parseExps();
            }
            reader.consume(TokenType.RBRACE);
            return new InitVal(exps);
        } else {
            Exp exp = parseExp();
            if (exp == null) {
                return null;
            }
            return new InitVal(exp);
        }
    }

    // FuncDef → FuncType <IDENFR> <LPARENT> [FuncFParams] <RPARENT> Block
    private FuncDef parseFuncDef() {
        FuncType funcType = parseFuncType();
        Token idenfr = reader.peek(0);
        reader.consume(TokenType.IDENFR);
        reader.consume(TokenType.LPARENT);
        FuncFParams funcFParams = null;
        if (reader.peek(0).type() != TokenType.RPARENT) {
            funcFParams = parseFuncFParams();
        }
        consumeError(TokenType.RPARENT);
        Block block = parseBlock();
        return new FuncDef(funcType, idenfr, funcFParams, block);
    }

    // MainFuncDef → <INTTK> <MAINTK> <LPARENT> <RPARENT> Block
    private MainFuncDef parseMainFuncDef() {
        reader.consume(TokenType.INTTK);
        reader.consume(TokenType.MAINTK);
        reader.consume(TokenType.LPARENT);
        consumeError(TokenType.RPARENT);
        Block block = parseBlock();
        return new MainFuncDef(block);
    }

    // FuncType → <VOIDTK> | <INTTK>
    private FuncType parseFuncType() {
        Token t = reader.peek(0);
        if (t.type() == TokenType.VOIDTK) {
            reader.consume(TokenType.VOIDTK);
            return new FuncType(FuncType.FuncTypeType.Void);
        } else {
            reader.consume(TokenType.INTTK);
            return new FuncType(FuncType.FuncTypeType.Int);
        }
    }

    // FuncFParams → FuncFParam { <COMMA> FuncFParam }
    private FuncFParams parseFuncFParams() {
        ArrayList<FuncFParam> funcFParams = new ArrayList<>();
        FuncFParam funcFParam = parseFuncFParam();
        if (funcFParam != null) {
            funcFParams.add(funcFParam);
        }
        while (reader.peek(0).type() == TokenType.COMMA) {
            reader.consume(TokenType.COMMA);
            FuncFParam param = parseFuncFParam();
            if (funcFParam != null) {
                funcFParams.add(param);
            }
        }
        return new FuncFParams(funcFParams);
    }

    // FuncFParam → BType <IDENFR> [<LBRACK> <RBRACK>]
    private FuncFParam parseFuncFParam() {
        BType bType = parseBType();
        Token idenfr = reader.peek(0);
        if (idenfr.type() != TokenType.IDENFR) {
            return null;
        }
        reader.consume(TokenType.IDENFR);
        FuncFParam.FuncFParamType funcFParamType = FuncFParam.FuncFParamType.Int;
        if (reader.peek(0).type() == TokenType.LBRACK) {
            reader.consume(TokenType.LBRACK);
            consumeError(TokenType.RBRACK);
            funcFParamType = FuncFParam.FuncFParamType.Array;
        }
        return new FuncFParam(bType, idenfr, funcFParamType);
    }

    // Block → <LBRACE> { BlockItem } <RBRACE>
    private Block parseBlock() {
        reader.consume(TokenType.LBRACE);
        ArrayList<BlockItem> blockItems = new ArrayList<>();
        while (reader.peek(0).type() != TokenType.RBRACE) {
            blockItems.add(parseBlockItem());
        }
        Token t = reader.peek(0);
        reader.consume(TokenType.RBRACE);
        return new Block(blockItems, t);
    }

    // BlockItem → Decl | Stmt
    private BlockItem parseBlockItem() {
        TokenType type = reader.peek(0).type();
        if (type == TokenType.CONSTTK || type == TokenType.INTTK || type == TokenType.STATICTK) {
            return new BlockItem(parseDecl());
        } else {
            return new BlockItem(parseStmt());
        }
    }

    private Stmt parseWhileStmt() {
        reader.consume(TokenType.WHILETK);
        reader.consume(TokenType.LPARENT);
        Cond cond = parseCond();
        consumeError(TokenType.RPARENT);
        Stmt stmt = parseStmt();
        return new Stmt(cond, stmt);
    }

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
    private Stmt parseStmt() {
        Token t = reader.peek(0);
        return switch (t.type()) {
            case LBRACE -> new Stmt(parseBlock());
            case IFTK -> parseIfStmt();
            case FORTK -> parseForStmtPart();
            case PRINTFTK -> parsePrintStmt();
            case WHILETK -> parseWhileStmt();
            case RETURNTK -> {
                Token t1 = reader.peek(0);
                reader.consume(TokenType.RETURNTK);
                yield new Stmt(parseExpEnd(), t1);
            }
            case BREAKTK, CONTINUETK -> {
                Token t1 = reader.peek(0);
                reader.consume(t.type());
                consumeError(TokenType.SEMICN);
                yield new Stmt(t1);
            }
            default -> {
                Stmt.StmtType stmtType = getStmtType();
                if (stmtType == null) {
                    yield null;
                } else if (stmtType == Stmt.StmtType.LVal) {
                    yield parseLvalStmt();
                } else {
                    yield new Stmt(parseExpEnd());
                }
            }
        };
    }

    private Stmt parseLvalStmt() {
        LVal lVal = parseLVal();
        if (reader.peek(0).type() == TokenType.ASSIGN) {
            reader.consume(TokenType.ASSIGN);
            Exp exp = parseExp();
            consumeError(TokenType.SEMICN);
            return new Stmt(lVal, exp);
        }
        return null;
    }

    private Stmt.StmtType getStmtType() {
        int id = 0;
        Stmt.StmtType stmtType = null;
        while (reader.peek(id) != null) {
            if (reader.peek(id).type() == TokenType.ASSIGN) {
                stmtType = Stmt.StmtType.LVal;
                break;
            } else if (reader.peek(id).type() == TokenType.SEMICN) {
                stmtType = Stmt.StmtType.Exp;
                break;
            }
            id += 1;
        }
        return stmtType;
    }

    private Stmt parseIfStmt() {
        reader.consume(TokenType.IFTK);
        reader.consume(TokenType.LPARENT);
        Cond cond = parseCond();
        consumeError(TokenType.RPARENT);
        Stmt stmt = parseStmt();
        Stmt stmtElse = null;
        if (reader.notEnd() && reader.peek(0).type() == TokenType.ELSETK) {
            reader.consume(TokenType.ELSETK);
            stmtElse = parseStmt();
        }
        return new Stmt(cond, stmt, stmtElse);
    }

    private Stmt parseForStmtPart() {
        reader.consume(TokenType.FORTK);
        reader.consume(TokenType.LPARENT);
        ForStmt forStmtLeft = null;
        if (reader.peek(0).type() != TokenType.SEMICN) {
            forStmtLeft = parseForStmt();
        }
        reader.consume(TokenType.SEMICN);
        Cond cond = null;
        if (reader.peek(0).type() != TokenType.SEMICN) {
            cond = parseCond();
        }
        reader.consume(TokenType.SEMICN);
        ForStmt forStmtRight = null;
        if (reader.peek(0).type() != TokenType.RPARENT) {
            forStmtRight = parseForStmt();
        }
        consumeError(TokenType.RPARENT);
        Stmt stmt = parseStmt();
        return new Stmt(forStmtLeft, cond, forStmtRight, stmt);
    }

    private Stmt parsePrintStmt() {
        Token t1 = reader.peek(0);
        reader.consume(TokenType.PRINTFTK);
        reader.consume(TokenType.LPARENT);
        Token strCon = reader.peek(0);
        reader.consume(TokenType.STRCON);
        ArrayList<Exp> exps = new ArrayList<>();
        while (reader.peek(0).type() == TokenType.COMMA) {
            reader.consume(TokenType.COMMA);
            Exp exp = parseExp();
            if (exp != null) {
                exps.add(exp);
            }
        }
        consumeError(TokenType.RPARENT);
        consumeError(TokenType.SEMICN);
        return new Stmt(t1, strCon.getStrCon(), exps);
    }

    private Exp parseExpEnd() {
        Exp exp = null;
        if (reader.peek(0).type() != TokenType.SEMICN) {
            exp = parseExp();
        }
        consumeError(TokenType.SEMICN);
        return exp;
    }

    // ForStmt → LVal <ASSIGN> Exp { <COMMA> LVal <ASSIGN> Exp }
    private ForStmt parseForStmt() {
        ArrayList<LVal> lVals = new ArrayList<>();
        ArrayList<Exp> exps = new ArrayList<>();
        LVal lVal = parseLVal();
        reader.consume(TokenType.ASSIGN);
        Exp exp = parseExp();
        lVals.add(lVal);
        if (exp != null) {
            exps.add(exp);
        }
        while (reader.peek(0).type() == TokenType.COMMA) {
            reader.consume(TokenType.COMMA);
            lVal = parseLVal();
            reader.consume(TokenType.ASSIGN);
            exp = parseExp();
            lVals.add(lVal);
            if (exp != null) {
                exps.add(exp);
            }
        }
        return new ForStmt(lVals, exps);
    }

    // Exp → AddExp
    private Exp parseExp() {
        AddExp addExp = parseAddExp();
        if (addExp == null) {
            return null;
        }
        return new Exp(addExp);
    }

    // Cond → LOrExp
    private Cond parseCond() {
        return new Cond(parseLOrExp());
    }

    // LVal → <IDENFR> [<LBRACK> Exp <RBRACK>]
    private LVal parseLVal() {
        Token idenfr = reader.peek(0);
        reader.consume(TokenType.IDENFR);
        Exp exp = null;
        if (reader.peek(0).type() == TokenType.LBRACK) {
            reader.consume(TokenType.LBRACK);
            exp = parseExp();
            consumeError(TokenType.RBRACK);
        }
        return new LVal(idenfr, exp);
    }

    // PrimaryExp → <LPARENT> Exp <RPARENT> | LVal | Number
    private PrimaryExp parsePrimaryExp() {
        Token t = reader.peek(0);
        if (t.type() == TokenType.LPARENT) {
            reader.consume(TokenType.LPARENT);
            Exp exp = parseExp();
            if (exp == null) {
                return null;
            }
            consumeError(TokenType.RPARENT);
            return new PrimaryExp(exp);
        } else if (t.type() == TokenType.IDENFR) {
            return new PrimaryExp(parseLVal());
        } else if (t.type() == TokenType.INTCON) {
            return new PrimaryExp(parseNumber());
        }
        return null;
    }

    // Number → <INTCON>
    private Number parseNumber() {
        Token t = reader.peek(0);
        reader.consume(TokenType.INTCON);
        return new Number(t.token());
    }

    // UnaryExp → PrimaryExp | <IDENFR> <LPARENT> [FuncRParams] <RPARENT> | UnaryOp UnaryExp
    private UnaryExp parseUnaryExp() {
        Token t1 = reader.peek(0);
        Token t2 = reader.peek(1);
        if (t1.type() == TokenType.PLUS || t1.type() == TokenType.MINU || t1.type() == TokenType.NOT) {
            UnaryOp unaryOp = parseUnaryOp();
            UnaryExp unaryExp = parseUnaryExp();
            if (unaryExp == null) {
                return null;
            }
            return new UnaryExp(unaryOp, unaryExp);
        } else if (t1.type() == TokenType.IDENFR && t2.type() == TokenType.LPARENT) {
            return parseFuncCall();
        } else {
            PrimaryExp primaryExp = parsePrimaryExp();
            if (primaryExp == null) {
                return null;
            }
            return new UnaryExp(primaryExp);
        }
    }

    private UnaryExp parseFuncCall() {
        Token idenfr = reader.peek(0);
        reader.consume(TokenType.IDENFR);
        reader.consume(TokenType.LPARENT);
        FuncRParams funcRParams = null;
        if (reader.peek(0).type() != TokenType.RPARENT) {
            funcRParams = parseFuncRParams();
        }
        consumeError(TokenType.RPARENT);
        return new UnaryExp(idenfr, funcRParams);
    }


    // UnaryOp → <PLUS> | <MINU> | <NOT>
    private UnaryOp parseUnaryOp() {
        Token t = reader.peek(0);
        reader.consume(t.type());
        return switch (t.type()) {
            case PLUS -> new UnaryOp(UnaryOp.UnaryOpType.Plus);
            case MINU -> new UnaryOp(UnaryOp.UnaryOpType.Minu);
            case NOT -> new UnaryOp(UnaryOp.UnaryOpType.Not);
            default -> null;
        };
    }

    // FuncRParams → Exp { <COMMA> Exp }
    private FuncRParams parseFuncRParams() {
        return new FuncRParams(parseExps());
    }

    private ArrayList<Exp> parseExps() {
        ArrayList<Exp> exps = new ArrayList<>();
        Exp exp = parseExp();
        if (exp != null) {
            exps.add(exp);
        }
        while (reader.peek(0).type() == TokenType.COMMA) {
            reader.consume(TokenType.COMMA);
            exp = parseExp();
            if (exp != null) {
                exps.add(exp);
            }
        }
        return exps;
    }

    // MulExp → UnaryExp { ( <MULT> | <DIV> | <MOD> ) UnaryExp }
    private MulExp parseMulExp() {
        ArrayList<UnaryExp> unaryExps = new ArrayList<>();
        ArrayList<MulExp.MulExpType> mulExpTypes = new ArrayList<>();
        UnaryExp unaryExp = parseUnaryExp();
        if (unaryExp == null) {
            return null;
        }
        unaryExps.add(unaryExp);
        TokenType type;
        while (reader.notEnd() && ((type = reader.peek(0).type()) == TokenType.MULT || type == TokenType.DIV || type == TokenType.MOD)) {
            Token t = reader.peek(0);
            reader.consume(t.type());
            switch (t.type()) {
                case MULT -> mulExpTypes.add(MulExp.MulExpType.Mult);
                case DIV -> mulExpTypes.add(MulExp.MulExpType.Div);
                case MOD -> mulExpTypes.add(MulExp.MulExpType.Mod);
            }
            unaryExps.add(parseUnaryExp());
        }
        return new MulExp(unaryExps, mulExpTypes);
    }

    // AddExp → MulExp { ( <PLUS> | <MINU> ) MulExp }
    private AddExp parseAddExp() {
        ArrayList<MulExp> mulExps = new ArrayList<>();
        ArrayList<AddExp.AddExpType> addExpTypes = new ArrayList<>();
        MulExp mulExp = parseMulExp();
        if (mulExp == null) {
            return null;
        }
        mulExps.add(mulExp);
        TokenType type;
        while (reader.notEnd() && ((type = reader.peek(0).type()) == TokenType.PLUS || type == TokenType.MINU)) {
            Token t = reader.peek(0);
            reader.consume(t.type());
            switch (t.type()) {
                case PLUS -> addExpTypes.add(AddExp.AddExpType.Plus);
                case MINU -> addExpTypes.add(AddExp.AddExpType.Minu);
            }
            mulExps.add(parseMulExp());
        }
        return new AddExp(mulExps, addExpTypes);
    }

    // RelExp → AddExp { ( <LSS> | <GRE> | <LEQ> | <GEQ> ) AddExp }
    private RelExp parseRelExp() {
        ArrayList<AddExp> addExps = new ArrayList<>();
        ArrayList<RelExp.RelExpType> relExpTypes = new ArrayList<>();
        AddExp addExp = parseAddExp();
        if (addExp == null) {
            return null;
        }
        addExps.add(addExp);
        TokenType type;
        while (reader.notEnd() && ((type = reader.peek(0).type()) == TokenType.LSS || type == TokenType.GRE || type == TokenType.LEQ || type == TokenType.GEQ)) {
            Token t = reader.peek(0);
            reader.consume(t.type());
            switch (t.type()) {
                case LSS -> relExpTypes.add(RelExp.RelExpType.Lss);
                case GRE -> relExpTypes.add(RelExp.RelExpType.Gre);
                case LEQ -> relExpTypes.add(RelExp.RelExpType.Leq);
                case GEQ -> relExpTypes.add(RelExp.RelExpType.Geq);
            }
            addExps.add(parseAddExp());
        }
        return new RelExp(addExps, relExpTypes);
    }

    // EqExp → RelExp { ( <EQL> | <NEQ> ) RelExp }
    private EqExp parseEqExp() {
        ArrayList<RelExp> relExps = new ArrayList<>();
        ArrayList<EqExp.EqExpType> eqExpTypes = new ArrayList<>();
        relExps.add(parseRelExp());
        TokenType type;
        while (reader.notEnd() && ((type = reader.peek(0).type()) == TokenType.EQL || type == TokenType.NEQ)) {
            Token t = reader.peek(0);
            reader.consume(t.type());
            switch (t.type()) {
                case EQL -> eqExpTypes.add(EqExp.EqExpType.Eql);
                case NEQ -> eqExpTypes.add(EqExp.EqExpType.Neq);
            }
            relExps.add(parseRelExp());
        }
        return new EqExp(relExps, eqExpTypes);
    }

    // LAndExp → EqExp { <AND> EqExp }
    private LAndExp parseLAndExp() {
        ArrayList<EqExp> eqExps = new ArrayList<>();
        eqExps.add(parseEqExp());
        while (reader.notEnd() && reader.peek(0).type() == TokenType.AND) {
            reader.consume(TokenType.AND);
            eqExps.add(parseEqExp());
        }
        return new LAndExp(eqExps);
    }

    // LOrExp → LAndExp { <OR> LAndExp }
    private LOrExp parseLOrExp() {
        ArrayList<LAndExp> lAndExps = new ArrayList<>();
        lAndExps.add(parseLAndExp());
        while (reader.notEnd() && reader.peek(0).type() == TokenType.OR) {
            reader.consume(TokenType.OR);
            lAndExps.add(parseLAndExp());
        }
        return new LOrExp(lAndExps);
    }

    // ConstExp → AddExp
    private ConstExp parseConstExp() {
        AddExp addExp = parseAddExp();
        if (addExp == null) {
            return null;
        }
        return new ConstExp(addExp);
    }

    public ErrorList getErrorList() {
        return errorList;
    }
}