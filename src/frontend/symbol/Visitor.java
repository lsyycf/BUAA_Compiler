package frontend.symbol;

import frontend.config.*;
import frontend.data.*;
import frontend.element.*;
import frontend.element.Number;
import frontend.lexer.*;

import java.util.*;

public class Visitor {
    private final CompUnit compUnit;
    private final SymbolTree symbolTree = new SymbolTree();
    private final ErrorList errorList = new ErrorList();
    private final HashMap<Integer, SymbolTree> symbolMap = new HashMap<>();
    private int scopeCounter = 1;
    private int loopDeep = 0;

    public Visitor(CompUnit compUnit) {
        this.compUnit = compUnit;
    }

    public static SymbolType getExpType(Exp exp, SymbolTree node) {
        return getAddExpType(exp.addExp(), node);
    }

    private static SymbolType getAddExpType(AddExp addExp, SymbolTree node) {
        return getMulExpType(addExp.getMulExp().get(0), node);
    }

    private static SymbolType getMulExpType(MulExp mulExp, SymbolTree node) {
        return getUnaryExpType(mulExp.getUnaryExp().get(0), node);
    }

    private static SymbolType getLValType(LVal lVal, SymbolTree node) {
        String idenfr = lVal.getIdenfr();
        Symbol symbol = node.findSymbolRecursive(idenfr, lVal.getLineIndex()).getSymbol(idenfr);
        SymbolType baseType = symbol.getType();
        if (lVal.getExp() != null) {
            return SymbolType.Int;
        }
        if (baseType == SymbolType.ConstInt || baseType == SymbolType.Int || baseType == SymbolType.StaticInt) {
            return SymbolType.Int;
        }
        return SymbolType.IntArray;
    }

    private static SymbolType getPrimaryExpType(PrimaryExp primaryExp, SymbolTree node) {
        if (primaryExp.getPrimaryExpType() == PrimaryExp.PrimaryExpType.Exp) {
            return getExpType(primaryExp.getExp(), node);
        }
        if (primaryExp.getPrimaryExpType() == PrimaryExp.PrimaryExpType.Number) {
            return SymbolType.Int;
        }
        return getLValType(primaryExp.getLval(), node);
    }

    private static SymbolType getUnaryExpType(UnaryExp unaryExp, SymbolTree node) {
        if (unaryExp.getUnaryExpType() == UnaryExp.UnaryExpType.PrimaryExp) {
            return getPrimaryExpType(unaryExp.getPrimaryExp(), node);
        }
        return SymbolType.Int;
    }

    public static int evaConstExp(ConstExp constExp, SymbolTree node) {
        return evaAddExp(constExp.addExp(), node);
    }

    private static int evaAddExp(AddExp addExp, SymbolTree node) {
        ArrayList<MulExp> mulExps = addExp.getMulExp();
        ArrayList<AddExp.AddExpType> ops = addExp.getAddExpType();
        int res = evaMulExp(mulExps.get(0), node);
        for (int i = 1; i < mulExps.size(); i++) {
            if (ops.get(i - 1) == AddExp.AddExpType.Plus) {
                res += evaMulExp(mulExps.get(i), node);
            } else {
                res -= evaMulExp(mulExps.get(i), node);
            }
        }
        return res;
    }

    private static int evaMulExp(MulExp mulExp, SymbolTree node) {
        ArrayList<UnaryExp> unaryExps = mulExp.getUnaryExp();
        ArrayList<MulExp.MulExpType> ops = mulExp.getMulExpType();
        int res = evaUnaryExp(unaryExps.get(0), node);
        for (int i = 1; i < unaryExps.size(); i++) {
            switch (ops.get(i - 1)) {
                case Mult:
                    res *= evaUnaryExp(unaryExps.get(i), node);
                    break;
                case Div:
                    res /= evaUnaryExp(unaryExps.get(i), node);
                    break;
                case Mod:
                    res %= evaUnaryExp(unaryExps.get(i), node);
                    break;
            }
        }
        return res;
    }

    private static int evaUnaryExp(UnaryExp unaryExp, SymbolTree node) {
        if (unaryExp.getUnaryExpType() == UnaryExp.UnaryExpType.PrimaryExp) {
            return evaPrimaryExp(unaryExp.getPrimaryExp(), node);
        } else if (unaryExp.getUnaryExpType() == UnaryExp.UnaryExpType.UnaryOp) {
            return evaUnaryOp(unaryExp, node);
        }
        return 0;
    }

    private static int evaPrimaryExp(PrimaryExp primaryExp, SymbolTree node) {
        if (primaryExp.getPrimaryExpType() == PrimaryExp.PrimaryExpType.Exp) {
            return evaExp(primaryExp.getExp(), node);
        } else if (primaryExp.getPrimaryExpType() == PrimaryExp.PrimaryExpType.Number) {
            return evaNumber(primaryExp.getNumber());
        } else {
            return evaLVal(primaryExp.getLval(), node);
        }
    }

    private static int evaExp(Exp exp, SymbolTree node) {
        return evaAddExp(exp.addExp(), node);
    }

    private static int evaNumber(Number number) {
        return Integer.parseInt(number.number());
    }

    private static int evaLVal(LVal lval, SymbolTree node) {
        String idenfr = lval.getIdenfr();
        Symbol symbol = node.findSymbolRecursive(idenfr, lval.getLineIndex()).getSymbol(idenfr);
        if (symbol.getType() == SymbolType.ConstInt) {
            return symbol.getEvaluations().get(0);
        } else if (symbol.getType() == SymbolType.ConstIntArray) {
            ArrayList<Integer> constList = symbol.getEvaluations();
            int index = evaExp(lval.getExp(), node);
            return constList.get(index);
        }
        return 0;
    }

    private static int evaUnaryOp(UnaryExp unaryExp, SymbolTree node) {
        UnaryOp.UnaryOpType opType = unaryExp.getUnaryOp().unaryOpType();
        if (opType == UnaryOp.UnaryOpType.Plus) {
            return evaUnaryExp(unaryExp.getUnaryExp(), node);
        } else if (opType == UnaryOp.UnaryOpType.Minu) {
            return -evaUnaryExp(unaryExp.getUnaryExp(), node);
        } else {
            if (evaUnaryExp(unaryExp.getUnaryExp(), node) == 0) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    // CompUnit → {Decl} {FuncDef} MainFuncDef
    public SymbolTree visitCompUnit() {
        for (Decl decl : compUnit.getDecl()) {
            visitDecl(decl, symbolTree);
        }
        for (FuncDef funcDef : compUnit.getFuncDef()) {
            visitFuncDef(funcDef, symbolTree);
        }
        visitMainFuncDef(compUnit.getMainFuncDef(), symbolTree);
        symbolTree.setSymbolTreeMap(symbolMap);
        return symbolTree;
    }

    // Decl → ConstDecl | VarDecl
    private void visitDecl(Decl decl, SymbolTree node) {
        if (decl.getDeclType() == Decl.DeclType.ConstDecl) {
            visitConstDecl(decl.getConstDecl(), node);
        } else {
            visitVarDecl(decl.getVarDecl(), node);
        }
    }

    // ConstDecl → <CONSTTK> BType ConstDef { <COMMA> ConstDef } <SEMICN>
    private void visitConstDecl(ConstDecl constDecl, SymbolTree node) {
        for (ConstDef constDef : constDecl.getConstDef()) {
            visitConstDef(constDef, node);
        }
    }

    // ConstDef → <IDENFR> [ <LBRACK> ConstExp <RBRACK> ] <ASSIGN> ConstInitVal
    private void visitConstDef(ConstDef constDef, SymbolTree node) {
        SymbolType type;
        if (constDef.getConstExp() != null) {
            type = SymbolType.ConstIntArray;
            visitConstExp(constDef.getConstExp(), node);
        } else {
            type = SymbolType.ConstInt;
        }
        String name = constDef.getIdenfr();
        if (node.findSymbol(name)) {
            errorList.addError(constDef.getLineIndex(), ErrorType.NAME_REDEFINITION);
        }
        Symbol symbol = new Symbol(name, type, constDef.getLineIndex());
        node.getSymbolMap().addSymbol(symbol);
        if (constDef.getConstInitVal() != null) {
            ArrayList<Integer> constList = visitConstInitVal(constDef.getConstInitVal(), node);
            symbol.setEvaluations(constList);
        }
    }

    // ConstInitVal → ConstExp | <LBRACE> [ ConstExp { <COMMA> ConstExp } ] <RBRACE>
    private ArrayList<Integer> visitConstInitVal(ConstInitVal constInitVal, SymbolTree node) {
        ArrayList<Integer> constList = new ArrayList<>();
        if (constInitVal.getConstInitValType() == ConstInitVal.ConstInitValType.ConstExp) {
            visitConstExp(constInitVal.getConstExp(), node);
            int eva = evaConstExp(constInitVal.getConstExp(), node);
            constList.add(eva);
        } else if (constInitVal.getConstInitValType() == ConstInitVal.ConstInitValType.ConstExpArr) {
            for (ConstExp constExp : constInitVal.getConstExpArr()) {
                visitConstExp(constExp, node);
                int eva = evaConstExp(constExp, node);
                constList.add(eva);
            }
        }
        return constList;
    }

    // VarDecl → [ <STATICTK> ] BType VarDef { <COMMA> VarDef } <SEMICN>
    private void visitVarDecl(VarDecl varDecl, SymbolTree node) {
        for (VarDef varDef : varDecl.getVarDef()) {
            visitVarDef(varDef, node, varDecl.getVarDeclType());
        }
    }

    // VarDef → <IDENFR> [ <LBRACK> ConstExp <RBRACK> ] | <IDENFR> [ <LBRACK> ConstExp <RBRACK> ] <ASSIGN> InitVal
    private void visitVarDef(VarDef varDef, SymbolTree node, VarDecl.VarDeclType varDeclType) {
        SymbolType type;
        if (varDeclType == VarDecl.VarDeclType.Static) {
            if (varDef.getConstExp() != null) {
                type = SymbolType.StaticIntArray;
                visitConstExp(varDef.getConstExp(), node);
            } else {
                type = SymbolType.StaticInt;
            }
        } else {
            if (varDef.getConstExp() != null) {
                type = SymbolType.IntArray;
                visitConstExp(varDef.getConstExp(), node);
            } else {
                type = SymbolType.Int;
            }
        }
        String name = varDef.getIdenfr();
        if (node.findSymbol(name)) {
            errorList.addError(varDef.getLineIndex(), ErrorType.NAME_REDEFINITION);
        }
        Symbol symbol = new Symbol(name, type, varDef.getLineIndex());
        node.getSymbolMap().addSymbol(symbol);
        if (varDef.getInitVal() != null) {
            visitInitVal(varDef.getInitVal(), node);
        }
    }

    // InitVal → Exp | <LBRACE> [ Exp { <COMMA> Exp } ] <RBRACE>
    private void visitInitVal(InitVal initVal, SymbolTree node) {
        if (initVal.getInitValType() == InitVal.InitValType.Exp) {
            visitExp(initVal.getExp(), node);
        } else {
            for (Exp exp : initVal.getExpArr()) {
                visitExp(exp, node);
            }
        }
    }

    //  FuncDef → FuncType <IDENFR> <LPARENT> [FuncFParams] <RPARENT> Block
    private void visitFuncDef(FuncDef funcDef, SymbolTree node) {
        SymbolType type;
        if (funcDef.getFuncType().funcTypeType() == FuncType.FuncTypeType.Int) {
            type = SymbolType.IntFunc;
        } else {
            type = SymbolType.VoidFunc;
        }
        String name = funcDef.getIdenfr();
        if (node.findSymbol(name)) {
            errorList.addError(funcDef.getLineIndex(), ErrorType.NAME_REDEFINITION);
        }
        Symbol symbol = new Symbol(name, type, funcDef.getLineIndex());
        node.getSymbolMap().addSymbol(symbol);
        scopeCounter++;
        SymbolTree funcNode = new SymbolTree(scopeCounter, node);
        symbolMap.put(scopeCounter, funcNode);
        if (funcDef.getFuncFParams() != null) {
            visitFuncFParams(funcDef.getFuncFParams(), funcNode);
            for (FuncFParam param : funcDef.getFuncFParams().getFuncFParam()) {
                if (param.getFuncFParamType() == FuncFParam.FuncFParamType.Array) {
                    symbol.addParamType(SymbolType.IntArray);
                } else {
                    symbol.addParamType(SymbolType.Int);
                }
            }
        }
        for (BlockItem blockItem : funcDef.getBlock().getBlockItem()) {
            visitBlockItem(blockItem, funcNode);
        }
        Block block = funcDef.getBlock();

        if (type == SymbolType.IntFunc) {
            boolean error = true;
            if (!block.getBlockItem().isEmpty()) {
                BlockItem blockitem = block.getBlockItem().get(block.getBlockItem().size() - 1);
                error = blockitem.getBlockItemType() != BlockItem.BlockItemType.Stmt || blockitem.getStmt().getStmtType() != Stmt.StmtType.Return || blockitem.getStmt().getExpReturn() == null;
            }
            if (error) {
                errorList.addError(block.getLineIndex(), ErrorType.MISSING_RETURN);
                block.getBlockItem().add(initial());
            }
        } else {
            checkBlock(block);
        }
    }

    // MainFuncDef → <INTTK> <MAINTK> <LPARENT> <RPARENT> Block
    private void visitMainFuncDef(MainFuncDef mainFuncDef, SymbolTree node) {
        visitBlock(mainFuncDef.block(), node);
        Block block = mainFuncDef.block();
        boolean error = true;
        if (!block.getBlockItem().isEmpty()) {
            BlockItem blockitem = block.getBlockItem().get(block.getBlockItem().size() - 1);
            error = blockitem.getBlockItemType() != BlockItem.BlockItemType.Stmt || blockitem.getStmt().getStmtType() != Stmt.StmtType.Return || blockitem.getStmt().getExpReturn() == null;
        }
        if (error) {
            errorList.addError(block.getLineIndex(), ErrorType.MISSING_RETURN);
            block.getBlockItem().add(initial());
        }
    }

    // FuncFParams → FuncFParam { <COMMA> FuncFParam }
    private void visitFuncFParams(FuncFParams funcFParams, SymbolTree node) {
        for (FuncFParam param : funcFParams.getFuncFParam()) {
            visitFuncFParam(param, node);
        }
    }

    // FuncFParam → BType <IDENFR> [<LBRACK> <RBRACK>]
    private void visitFuncFParam(FuncFParam funcFParam, SymbolTree node) {
        SymbolType type;
        if (funcFParam.getFuncFParamType() == FuncFParam.FuncFParamType.Array) {
            type = SymbolType.IntArray;
        } else {
            type = SymbolType.Int;
        }
        String name = funcFParam.getIdenfr();
        if (node.findSymbol(name)) {
            errorList.addError(funcFParam.getLineIndex(), ErrorType.NAME_REDEFINITION);
        }
        Symbol symbol = new Symbol(name, type, funcFParam.getLineIndex());
        node.getSymbolMap().addSymbol(symbol);
    }

    // Block → <LBRACE> { BlockItem } <RBRACE>
    private void visitBlock(Block block, SymbolTree node) {
        scopeCounter++;
        SymbolTree blockNode = new SymbolTree(scopeCounter, node);
        symbolMap.put(scopeCounter, blockNode);
        for (BlockItem blockItem : block.getBlockItem()) {
            visitBlockItem(blockItem, blockNode);
        }
    }

    // BlockItem → Decl | Stmt
    private void visitBlockItem(BlockItem blockItem, SymbolTree node) {
        if (blockItem.getBlockItemType() == BlockItem.BlockItemType.Decl) {
            visitDecl(blockItem.getDecl(), node);
        } else {
            visitStmt(blockItem.getStmt(), node);
        }
    }

    // Stmt → LVal <ASSIGN> Exp <SEMICN>
    // | [Exp] <SEMICN>
    // | Block
    // | <IFTK> <LPARENT> Cond <RPARENT> Stmt [ <ELSETK> Stmt ]
    // | <FORTK> <LPARENT> [ForStmt] <SEMICN> [Cond] <SEMICN> [ForStmt] <RPARENT> Stmt
    // | <BREAKTK> <SEMICN>
    // | <CONTINUETK> <SEMICN>
    // | <RETURNTK> [Exp] <SEMICN>
    // | <PRINTFTK> <LPARENT> <STRCON> { <COMMA> Exp } <RPARENT> <SEMICN>
    private void visitStmt(Stmt stmt, SymbolTree node) {
        if (stmt.getStmtType() == Stmt.StmtType.Block) {
            visitBlock(stmt.getBlock(), node);
        } else if (stmt.getStmtType() == Stmt.StmtType.If) {
            visitCond(stmt.getCondIf(), node);
            visitStmt(stmt.getStmtIf(), node);
            if (stmt.getStmtElse() != null) {
                visitStmt(stmt.getStmtElse(), node);
            }
        } else if (stmt.getStmtType() == Stmt.StmtType.For) {
            loopDeep += 1;
            if (stmt.getForStmtLeft() != null) {
                visitForStmt(stmt.getForStmtLeft(), node);
            }
            if (stmt.getCondFor() != null) {
                visitCond(stmt.getCondFor(), node);
            }
            if (stmt.getForStmtRight() != null) {
                visitForStmt(stmt.getForStmtRight(), node);
            }
            visitStmt(stmt.getStmtFor(), node);
            loopDeep -= 1;
        } else if (stmt.getStmtType() == Stmt.StmtType.LVal) {
            String idenfr = stmt.getlVal().getIdenfr();
            SymbolMap symbolMap = node.findSymbolRecursive(idenfr, -1);
            if (symbolMap != null && (symbolMap.getSymbol(idenfr).getType() == SymbolType.ConstInt || symbolMap.getSymbol(idenfr).getType() == SymbolType.ConstIntArray)) {
                errorList.addError(stmt.getlVal().getLineIndex(), ErrorType.CONSTANT_MODIFICATION);
            }
            visitLVal(stmt.getlVal(), node);
            visitExp(stmt.getExpLVal(), node);
        } else if (stmt.getStmtType() == Stmt.StmtType.Exp) {
            if (stmt.getExp() != null) {
                visitExp(stmt.getExp(), node);
            }
        } else if (stmt.getStmtType() == Stmt.StmtType.Break || stmt.getStmtType() == Stmt.StmtType.Continue) {
            if (loopDeep == 0) {
                errorList.addError(stmt.getLineIndex(), ErrorType.BREAK_CONTINUE_OUT_LOOP);
                stmt.clear();
            }
        } else if (stmt.getStmtType() == Stmt.StmtType.Return) {
            if (stmt.getExpReturn() != null) {
                visitExp(stmt.getExpReturn(), node);
            }
        } else {
            String format = stmt.getStrCon();
            int count = 0;
            for (int i = 0; i < format.length() - 1; i++) {
                if (format.charAt(i) == '%' && format.charAt(i + 1) == 'd') {
                    count++;
                }
            }
            if (count != stmt.getExpPrint().size()) {
                errorList.addError(stmt.getLineIndex(), ErrorType.PRINTF_FORMAT_MISMATCH);
            }
            for (Exp exp : stmt.getExpPrint()) {
                visitExp(exp, node);
            }
        }
    }

    // ForStmt → LVal <ASSIGN> Exp { <COMMA> LVal <ASSIGN> Exp }
    private void visitForStmt(ForStmt forStmt, SymbolTree node) {
        for (LVal lVal : forStmt.getlVal()) {
            String idenfr = lVal.getIdenfr();
            SymbolMap symbolMap = node.findSymbolRecursive(idenfr, -1);
            if (symbolMap != null && (symbolMap.getSymbol(idenfr).getType() == SymbolType.ConstInt || symbolMap.getSymbol(idenfr).getType() == SymbolType.ConstIntArray)) {
                errorList.addError(lVal.getLineIndex(), ErrorType.CONSTANT_MODIFICATION);
            }
        }
        for (int i = 0; i < forStmt.getExp().size(); i++) {
            visitLVal(forStmt.getlVal().get(i), node);
            visitExp(forStmt.getExp().get(i), node);
        }
    }

    // Exp → AddExp
    private void visitExp(Exp exp, SymbolTree node) {
        visitAddExp(exp.addExp(), node);
    }

    // Cond → LOrExp
    private void visitCond(Cond cond, SymbolTree node) {
        visitLOrExp(cond.lOrExp(), node);
    }

    // LVal → <IDENFR> [<LBRACK> Exp <RBRACK>]
    private void visitLVal(LVal lVal, SymbolTree node) {
        String name = lVal.getIdenfr();
        if (node.findSymbolRecursive(name, lVal.getLineIndex()) == null) {
            errorList.addError(lVal.getLineIndex(), ErrorType.UNDEFINED_NAME);
        } else if (lVal.getExp() != null) {
            visitExp(lVal.getExp(), node);
        }
    }

    // PrimaryExp → <LPARENT> Exp <RPARENT> | LVal | Number
    private void visitPrimaryExp(PrimaryExp primaryExp, SymbolTree node) {
        if (primaryExp.getPrimaryExpType() == PrimaryExp.PrimaryExpType.Exp) {
            visitExp(primaryExp.getExp(), node);
        } else if (primaryExp.getPrimaryExpType() == PrimaryExp.PrimaryExpType.LVal) {
            visitLVal(primaryExp.getLval(), node);
        }
    }

    // UnaryExp → PrimaryExp | <IDENFR> <LPARENT> [FuncRParams] <RPARENT> | UnaryOp UnaryExp
    private void visitUnaryExp(UnaryExp unaryExp, SymbolTree node) {
        if (unaryExp.getUnaryExpType() == UnaryExp.UnaryExpType.PrimaryExp) {
            visitPrimaryExp(unaryExp.getPrimaryExp(), node);
        } else if (unaryExp.getUnaryExpType() == UnaryExp.UnaryExpType.FuncRParams) {
            String name = unaryExp.getIdenfr();
            SymbolMap symbolMap = node.findSymbolRecursive(name, unaryExp.getLineIndex());
            if (symbolMap == null) {
                errorList.addError(unaryExp.getLineIndex(), ErrorType.UNDEFINED_NAME);
            } else {
                Symbol symbol = symbolMap.getSymbol(name);
                ArrayList<SymbolType> formalParams = symbol.getParamTypes();
                FuncRParams actualParams = unaryExp.getFuncRParams();
                int formalCount = formalParams.size();
                int actualCount = 0;
                if (actualParams != null) {
                    actualCount = actualParams.getExp().size();
                }
                if (formalCount != actualCount) {
                    errorList.addError(unaryExp.getLineIndex(), ErrorType.PARAM_COUNT_MISMATCH);
                } else {
                    for (int i = 0; i < formalCount; i++) {
                        SymbolType formalType = formalParams.get(i);
                        Exp actualExp = actualParams.getExp().get(i);
                        SymbolType actualType = getExpType(actualExp, node);
                        if (formalType != actualType) {
                            errorList.addError(unaryExp.getLineIndex(), ErrorType.PARAM_TYPE_MISMATCH);
                        }
                    }
                }
            }
            if (unaryExp.getFuncRParams() != null) {
                visitFuncFRarams(unaryExp.getFuncRParams(), node);
            }
        }
    }

    // FuncRParams → Exp { <COMMA> Exp }
    private void visitFuncFRarams(FuncRParams funcRParams, SymbolTree node) {
        for (Exp exp : funcRParams.getExp()) {
            visitExp(exp, node);
        }
    }

    // MulExp → UnaryExp { ( <MULT> | <DIV> | <MOD> ) UnaryExp }
    private void visitMulExp(MulExp mulExp, SymbolTree node) {
        for (UnaryExp unaryExp : mulExp.getUnaryExp()) {
            visitUnaryExp(unaryExp, node);
        }
    }

    // AddExp → MulExp { ( <PLUS> | <MINU> ) MulExp }
    private void visitAddExp(AddExp addExp, SymbolTree node) {
        for (MulExp mulExp : addExp.getMulExp()) {
            visitMulExp(mulExp, node);
        }
    }

    // RelExp → AddExp { ( <LSS> | <GRE> | <LEQ> | <GEQ> ) AddExp }
    private void visitRelExp(RelExp relExp, SymbolTree node) {
        for (AddExp addExp : relExp.getAddExp()) {
            visitAddExp(addExp, node);
        }
    }

    // EqExp → RelExp { ( <EQL> | <NEQ> ) RelExp }
    private void visitEqExp(EqExp eqExp, SymbolTree node) {
        for (RelExp relExp : eqExp.getRelExp()) {
            visitRelExp(relExp, node);
        }
    }

    // LAndExp → EqExp { <AND> EqExp }
    private void visitLAndExp(LAndExp lAndExp, SymbolTree node) {
        for (EqExp eqExp : lAndExp.getEqExp()) {
            visitEqExp(eqExp, node);
        }
    }

    // LOrExp → LAndExp { <OR> LAndExp }
    private void visitLOrExp(LOrExp lOrExp, SymbolTree node) {
        for (LAndExp lAndExp : lOrExp.getlAndExp()) {
            visitLAndExp(lAndExp, node);
        }
    }

    // ConstExp → AddExp
    private void visitConstExp(ConstExp constExp, SymbolTree node) {
        visitAddExp(constExp.addExp(), node);
    }

    private void checkBlock(Block block) {
        for (BlockItem blockItem : block.getBlockItem()) {
            if (blockItem.getBlockItemType() == BlockItem.BlockItemType.Stmt) {
                checkStmt(blockItem.getStmt());
            }
        }
    }

    private void checkStmt(Stmt stmt) {
        if (stmt.getStmtType() == Stmt.StmtType.Return) {
            if (stmt.getExpReturn() != null) {
                errorList.addError(stmt.getLineIndex(), ErrorType.INVALID_RETURN);
                stmt.clearExpReturn();
            }
        } else if (stmt.getStmtType() == Stmt.StmtType.Block) {
            checkBlock(stmt.getBlock());
        } else if (stmt.getStmtType() == Stmt.StmtType.If) {
            checkStmt(stmt.getStmtIf());
            if (stmt.getStmtElse() != null) {
                checkStmt(stmt.getStmtElse());
            }
        } else if (stmt.getStmtType() == Stmt.StmtType.For) {
            checkStmt(stmt.getStmtFor());
        }
    }

    public ErrorList getErrorList() {
        return errorList;
    }

    private BlockItem initial() {
        Number num = new Number("0");
        PrimaryExp primaryExp = new PrimaryExp(num);
        UnaryExp unaryExp = new UnaryExp(primaryExp);
        ArrayList<UnaryExp> unaryExps = new ArrayList<>();
        unaryExps.add(unaryExp);
        MulExp mulExp = new MulExp(unaryExps, new ArrayList<>());
        ArrayList<MulExp> mulExps = new ArrayList<>();
        mulExps.add(mulExp);
        AddExp addExp = new AddExp(mulExps, new ArrayList<>());
        Exp exp = new Exp(addExp);
        Token token = new Token(TokenType.RETURNTK, 0, "return");
        Stmt stmt = new Stmt(exp, token);
        return new BlockItem(stmt);
    }
}