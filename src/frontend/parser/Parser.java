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
        ArrayList<Decl> decls = new ArrayList<>();
        ArrayList<FuncDef> funcDefs = new ArrayList<>();
        MainFuncDef mainFuncDef;
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
        mainFuncDef = parseMainFuncDef();
        return new CompUnit(decls, funcDefs, mainFuncDef);
    }

    // Decl → ConstDecl | VarDecl
    public Decl parseDecl() {
        Token t = reader.peek(0);
        if (t.type() == TokenType.CONSTTK) {
            return new Decl(parseConstDecl());
        } else {
            return new Decl(parseVarDecl());
        }
    }

    // ConstDecl → <CONSTTK> BType ConstDef { <COMMA> ConstDef } <SEMICN>
    public ConstDecl parseConstDecl() {
        reader.consume(TokenType.CONSTTK);
        BType bType = parseBType();
        ArrayList<ConstDef> constDefs = new ArrayList<>();
        constDefs.add(parseConstDef());
        while (reader.notEnd() && reader.peek(0).type() == TokenType.COMMA) {
            reader.consume(TokenType.COMMA);
            constDefs.add(parseConstDef());
        }
        if (reader.peek(0).type() != TokenType.SEMICN) {
            errorList.addError(reader.getLineIndex(), ErrorType.MISSING_SEMICOLON);
        } else {
            reader.consume(TokenType.SEMICN);
        }
        return new ConstDecl(bType, constDefs);
    }

    // BType → <INTTK>
    public BType parseBType() {
        reader.consume(TokenType.INTTK);
        return new BType();
    }

    // ConstDef → <IDENFR> [ <LBRACK> ConstExp <RBRACK> ] <ASSIGN> ConstInitVal
    public ConstDef parseConstDef() {
        Token idenfr = reader.peek(0);
        reader.consume(TokenType.IDENFR);
        ConstExp constExp = null;
        if (reader.peek(0).type() == TokenType.LBRACK) {
            reader.consume(TokenType.LBRACK);
            constExp = parseConstExp();
            if (reader.peek(0).type() != TokenType.RBRACK) {
                errorList.addError(reader.getLineIndex(), ErrorType.MISSING_RIGHT_BRACKET);
            } else {
                reader.consume(TokenType.RBRACK);
            }
        }
        reader.consume(TokenType.ASSIGN);
        ConstInitVal constInitVal = parseConstInitVal();
        return new ConstDef(idenfr, constExp, constInitVal);
    }

    // ConstInitVal → ConstExp | <LBRACE> [ ConstExp { <COMMA> ConstExp } ] <RBRACE>
    public ConstInitVal parseConstInitVal() {
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
    public VarDecl parseVarDecl() {
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
        if (reader.peek(0).type() != TokenType.SEMICN) {
            errorList.addError(reader.getLineIndex(), ErrorType.MISSING_SEMICOLON);
        } else {
            reader.consume(TokenType.SEMICN);
        }
        return new VarDecl(varDeclType, bType, varDefs);
    }

    // VarDef → <IDENFR> [ <LBRACK> ConstExp <RBRACK> ] | <IDENFR> [ <LBRACK> ConstExp <RBRACK> ] <ASSIGN> InitVal
    public VarDef parseVarDef() {
        Token idenfr = reader.peek(0);
        reader.consume(TokenType.IDENFR);
        ConstExp constExp = null;
        if (reader.peek(0).type() == TokenType.LBRACK) {
            reader.consume(TokenType.LBRACK);
            constExp = parseConstExp();
            if (reader.peek(0).type() != TokenType.RBRACK) {
                errorList.addError(reader.getLineIndex(), ErrorType.MISSING_RIGHT_BRACKET);
            } else {
                reader.consume(TokenType.RBRACK);
            }
        }
        if (reader.peek(0).type() == TokenType.ASSIGN) {
            reader.consume(TokenType.ASSIGN);
            InitVal initVal = parseInitVal();
            return new VarDef(idenfr, constExp, initVal);
        }
        return new VarDef(idenfr, constExp);
    }

    // InitVal → Exp | <LBRACE> [ Exp { <COMMA> Exp } ] <RBRACE>
    public InitVal parseInitVal() {
        if (reader.peek(0).type() == TokenType.LBRACE) {
            reader.consume(TokenType.LBRACE);
            ArrayList<Exp> exps = new ArrayList<>();
            if (reader.peek(0).type() != TokenType.RBRACE) {
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
    public FuncDef parseFuncDef() {
        FuncType funcType = parseFuncType();
        Token idenfr = reader.peek(0);
        reader.consume(TokenType.IDENFR);
        reader.consume(TokenType.LPARENT);
        FuncFParams funcFParams = null;
        if (reader.peek(0).type() != TokenType.RPARENT) {
            funcFParams = parseFuncFParams();
        }
        if (reader.peek(0).type() != TokenType.RPARENT) {
            errorList.addError(reader.getLineIndex(), ErrorType.MISSING_RIGHT_PARENTHESIS);
        } else {
            reader.consume(TokenType.RPARENT);
        }
        Block block = parseBlock();
        return new FuncDef(funcType, idenfr, funcFParams, block);
    }

    // MainFuncDef → <INTTK> <MAINTK> <LPARENT> <RPARENT> Block
    public MainFuncDef parseMainFuncDef() {
        reader.consume(TokenType.INTTK);
        reader.consume(TokenType.MAINTK);
        reader.consume(TokenType.LPARENT);
        if (reader.peek(0).type() != TokenType.RPARENT) {
            errorList.addError(reader.getLineIndex(), ErrorType.MISSING_RIGHT_PARENTHESIS);
        } else {
            reader.consume(TokenType.RPARENT);
        }
        Block block = parseBlock();
        return new MainFuncDef(block);
    }

    // FuncType → <VOIDTK> | <INTTK>
    public FuncType parseFuncType() {
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
    public FuncFParams parseFuncFParams() {
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
    public FuncFParam parseFuncFParam() {
        BType bType = parseBType();
        Token idenfr = reader.peek(0);
        if (idenfr.type() != TokenType.IDENFR) {
            return null;
        }
        reader.consume(TokenType.IDENFR);
        FuncFParam.FuncFParamType funcFParamType = FuncFParam.FuncFParamType.Int;
        if (reader.peek(0).type() == TokenType.LBRACK) {
            reader.consume(TokenType.LBRACK);
            if (reader.peek(0).type() != TokenType.RBRACK) {
                errorList.addError(reader.getLineIndex(), ErrorType.MISSING_RIGHT_BRACKET);
            } else {
                reader.consume(TokenType.RBRACK);
            }
            funcFParamType = FuncFParam.FuncFParamType.Array;
        }
        return new FuncFParam(bType, idenfr, funcFParamType);
    }

    // Block → <LBRACE> { BlockItem } <RBRACE>
    public Block parseBlock() {
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
    public BlockItem parseBlockItem() {
        TokenType type = reader.peek(0).type();
        if (type == TokenType.CONSTTK || type == TokenType.INTTK || type == TokenType.STATICTK) {
            return new BlockItem(parseDecl());
        } else {
            return new BlockItem(parseStmt());
        }
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
    public Stmt parseStmt() {
        Token t = reader.peek(0);
        if (t.type() == TokenType.LBRACE) {
            return new Stmt(parseBlock());
        } else if (t.type() == TokenType.IFTK) {
            reader.consume(TokenType.IFTK);
            reader.consume(TokenType.LPARENT);
            Cond cond = parseCond();
            if (reader.peek(0).type() != TokenType.RPARENT) {
                errorList.addError(reader.getLineIndex(), ErrorType.MISSING_RIGHT_PARENTHESIS);
            } else {
                reader.consume(TokenType.RPARENT);
            }
            Stmt stmt = parseStmt();
            Stmt stmtElse = null;
            if (reader.notEnd() && reader.peek(0).type() == TokenType.ELSETK) {
                reader.consume(TokenType.ELSETK);
                stmtElse = parseStmt();
            }
            return new Stmt(cond, stmt, stmtElse);
        } else if (t.type() == TokenType.FORTK) {
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
            if (reader.peek(0).type() != TokenType.RPARENT) {
                errorList.addError(reader.getLineIndex(), ErrorType.MISSING_RIGHT_PARENTHESIS);
            } else {
                reader.consume(TokenType.RPARENT);
            }
            Stmt stmt = parseStmt();
            return new Stmt(forStmtLeft, cond, forStmtRight, stmt);
        } else if (t.type() == TokenType.BREAKTK) {
            Token t1 = reader.peek(0);
            reader.consume(TokenType.BREAKTK);
            if (reader.peek(0).type() != TokenType.SEMICN) {
                errorList.addError(reader.getLineIndex(), ErrorType.MISSING_SEMICOLON);
            } else {
                reader.consume(TokenType.SEMICN);
            }
            return new Stmt(t1);
        } else if (t.type() == TokenType.CONTINUETK) {
            Token t1 = reader.peek(0);
            reader.consume(TokenType.CONTINUETK);
            if (reader.peek(0).type() != TokenType.SEMICN) {
                errorList.addError(reader.getLineIndex(), ErrorType.MISSING_SEMICOLON);
            } else {
                reader.consume(TokenType.SEMICN);
            }
            return new Stmt(t1);
        } else if (t.type() == TokenType.RETURNTK) {
            Token t1 = reader.peek(0);
            reader.consume(TokenType.RETURNTK);
            Exp exp = null;
            if (reader.peek(0).type() != TokenType.SEMICN) {
                exp = parseExp();
            }
            if (reader.peek(0).type() != TokenType.SEMICN) {
                errorList.addError(reader.getLineIndex(), ErrorType.MISSING_SEMICOLON);
            } else {
                reader.consume(TokenType.SEMICN);
            }
            return new Stmt(exp, t1);
        } else if (t.type() == TokenType.PRINTFTK) {
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
            if (reader.peek(0).type() != TokenType.RPARENT) {
                errorList.addError(reader.getLineIndex(), ErrorType.MISSING_RIGHT_PARENTHESIS);
            } else {
                reader.consume(TokenType.RPARENT);
            }
            if (reader.peek(0).type() != TokenType.SEMICN) {
                errorList.addError(reader.getLineIndex(), ErrorType.MISSING_SEMICOLON);
            } else {
                reader.consume(TokenType.SEMICN);
            }
            return new Stmt(t1, strCon.getStrCon(), exps);
        } else {
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
            if (stmtType == null) {
                return null;
            } else if (stmtType == Stmt.StmtType.LVal) {
                LVal lVal = parseLVal();
                if (reader.peek(0).type() == TokenType.ASSIGN) {
                    reader.consume(TokenType.ASSIGN);
                    Exp exp = parseExp();
                    if (reader.peek(0).type() != TokenType.SEMICN) {
                        errorList.addError(reader.getLineIndex(), ErrorType.MISSING_SEMICOLON);
                    } else {
                        reader.consume(TokenType.SEMICN);
                    }
                    return new Stmt(lVal, exp);
                }
            } else {
                Exp exp = null;
                if (reader.peek(0).type() != TokenType.SEMICN) {
                    exp = parseExp();
                }
                if (reader.peek(0).type() != TokenType.SEMICN) {
                    errorList.addError(reader.getLineIndex(), ErrorType.MISSING_SEMICOLON);
                } else {
                    reader.consume(TokenType.SEMICN);
                }
                return new Stmt(exp);
            }
            return null;
        }
    }

    // ForStmt → LVal <ASSIGN> Exp { <COMMA> LVal <ASSIGN> Exp }
    public ForStmt parseForStmt() {
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
    public Exp parseExp() {
        AddExp addExp = parseAddExp();
        if (addExp == null) {
            return null;
        }
        return new Exp(addExp);
    }

    // Cond → LOrExp
    public Cond parseCond() {
        return new Cond(parseLOrExp());
    }

    // LVal → <IDENFR> [<LBRACK> Exp <RBRACK>]
    public LVal parseLVal() {
        Token idenfr = reader.peek(0);
        reader.consume(TokenType.IDENFR);
        Exp exp = null;
        if (reader.peek(0).type() == TokenType.LBRACK) {
            reader.consume(TokenType.LBRACK);
            exp = parseExp();
            if (reader.peek(0).type() != TokenType.RBRACK) {
                errorList.addError(reader.getLineIndex(), ErrorType.MISSING_RIGHT_BRACKET);
            } else {
                reader.consume(TokenType.RBRACK);
            }
        }
        return new LVal(idenfr, exp);
    }

    // PrimaryExp → <LPARENT> Exp <RPARENT> | LVal | Number
    public PrimaryExp parsePrimaryExp() {
        Token t = reader.peek(0);
        if (t.type() == TokenType.LPARENT) {
            reader.consume(TokenType.LPARENT);
            Exp exp = parseExp();
            if (exp == null) {
                return null;
            }
            if (reader.peek(0).type() != TokenType.RPARENT) {
                errorList.addError(reader.getLineIndex(), ErrorType.MISSING_RIGHT_PARENTHESIS);
            } else {
                reader.consume(TokenType.RPARENT);
            }
            return new PrimaryExp(exp);
        } else if (t.type() == TokenType.IDENFR) {
            return new PrimaryExp(parseLVal());
        } else if (t.type() == TokenType.INTCON) {
            return new PrimaryExp(parseNumber());
        }
        return null;
    }

    // Number → <INTCON>
    public Number parseNumber() {
        Token t = reader.peek(0);
        reader.consume(TokenType.INTCON);
        return new Number(t.token());
    }

    // UnaryExp → PrimaryExp | <IDENFR> <LPARENT> [FuncRParams] <RPARENT> | UnaryOp UnaryExp
    public UnaryExp parseUnaryExp() {
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
            Token idenfr = reader.peek(0);
            reader.consume(TokenType.IDENFR);
            reader.consume(TokenType.LPARENT);
            FuncRParams funcRParams = null;
            if (reader.peek(0).type() != TokenType.RPARENT) {
                funcRParams = parseFuncRParams();
            }
            if (reader.peek(0).type() != TokenType.RPARENT) {
                errorList.addError(reader.getLineIndex(), ErrorType.MISSING_RIGHT_PARENTHESIS);
            } else {
                reader.consume(TokenType.RPARENT);
            }
            return new UnaryExp(idenfr, funcRParams);
        } else {
            PrimaryExp primaryExp = parsePrimaryExp();
            if (primaryExp == null) {
                return null;
            }
            return new UnaryExp(primaryExp);
        }
    }

    // UnaryOp → <PLUS> | <MINU> | <NOT>
    public UnaryOp parseUnaryOp() {
        Token t = reader.peek(0);
        if (t.type() == TokenType.PLUS) {
            reader.consume(TokenType.PLUS);
            return new UnaryOp(UnaryOp.UnaryOpType.Plus);
        } else if (t.type() == TokenType.MINU) {
            reader.consume(TokenType.MINU);
            return new UnaryOp(UnaryOp.UnaryOpType.Minu);
        } else if (t.type() == TokenType.NOT) {
            reader.consume(TokenType.NOT);
            return new UnaryOp(UnaryOp.UnaryOpType.Not);
        }
        return null;
    }

    // FuncRParams → Exp { <COMMA> Exp }
    public FuncRParams parseFuncRParams() {
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
        return new FuncRParams(exps);
    }

    // MulExp → UnaryExp { ( <MULT> | <DIV> | <MOD> ) UnaryExp }
    public MulExp parseMulExp() {
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
            if (t.type() == TokenType.MULT) {
                reader.consume(TokenType.MULT);
                mulExpTypes.add(MulExp.MulExpType.Mult);
            } else if (t.type() == TokenType.DIV) {
                reader.consume(TokenType.DIV);
                mulExpTypes.add(MulExp.MulExpType.Div);
            } else if (t.type() == TokenType.MOD) {
                reader.consume(TokenType.MOD);
                mulExpTypes.add(MulExp.MulExpType.Mod);
            }
            unaryExps.add(parseUnaryExp());
        }
        return new MulExp(unaryExps, mulExpTypes);
    }

    // AddExp → MulExp { ( <PLUS> | <MINU> ) MulExp }
    public AddExp parseAddExp() {
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
            if (t.type() == TokenType.PLUS) {
                reader.consume(TokenType.PLUS);
                addExpTypes.add(AddExp.AddExpType.Plus);
            } else if (t.type() == TokenType.MINU) {
                reader.consume(TokenType.MINU);
                addExpTypes.add(AddExp.AddExpType.Minu);
            }
            mulExps.add(parseMulExp());
        }
        return new AddExp(mulExps, addExpTypes);
    }

    // RelExp → AddExp { ( <LSS> | <GRE> | <LEQ> | <GEQ> ) AddExp }
    public RelExp parseRelExp() {
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
            if (t.type() == TokenType.LSS) {
                reader.consume(TokenType.LSS);
                relExpTypes.add(RelExp.RelExpType.Lss);
            } else if (t.type() == TokenType.GRE) {
                reader.consume(TokenType.GRE);
                relExpTypes.add(RelExp.RelExpType.Gre);
            } else if (t.type() == TokenType.LEQ) {
                reader.consume(TokenType.LEQ);
                relExpTypes.add(RelExp.RelExpType.Leq);
            } else {
                reader.consume(TokenType.GEQ);
                relExpTypes.add(RelExp.RelExpType.Geq);
            }
            addExps.add(parseAddExp());
        }
        return new RelExp(addExps, relExpTypes);
    }

    // EqExp → RelExp { ( <EQL> | <NEQ> ) RelExp }
    public EqExp parseEqExp() {
        ArrayList<RelExp> relExps = new ArrayList<>();
        ArrayList<EqExp.EqExpType> eqExpTypes = new ArrayList<>();
        relExps.add(parseRelExp());
        TokenType type;
        while (reader.notEnd() && ((type = reader.peek(0).type()) == TokenType.EQL || type == TokenType.NEQ)) {
            Token t = reader.peek(0);
            if (t.type() == TokenType.EQL) {
                reader.consume(TokenType.EQL);
                eqExpTypes.add(EqExp.EqExpType.Eql);
            } else {
                reader.consume(TokenType.NEQ);
                eqExpTypes.add(EqExp.EqExpType.Neq);
            }
            relExps.add(parseRelExp());
        }
        return new EqExp(relExps, eqExpTypes);
    }

    // LAndExp → EqExp { <AND> EqExp }
    public LAndExp parseLAndExp() {
        ArrayList<EqExp> eqExps = new ArrayList<>();
        eqExps.add(parseEqExp());
        while (reader.notEnd() && reader.peek(0).type() == TokenType.AND) {
            reader.consume(TokenType.AND);
            eqExps.add(parseEqExp());
        }
        return new LAndExp(eqExps);
    }

    // LOrExp → LAndExp { <OR> LAndExp }
    public LOrExp parseLOrExp() {
        ArrayList<LAndExp> lAndExps = new ArrayList<>();
        lAndExps.add(parseLAndExp());
        while (reader.notEnd() && reader.peek(0).type() == TokenType.OR) {
            reader.consume(TokenType.OR);
            lAndExps.add(parseLAndExp());
        }
        return new LOrExp(lAndExps);
    }

    // ConstExp → AddExp
    public ConstExp parseConstExp() {
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