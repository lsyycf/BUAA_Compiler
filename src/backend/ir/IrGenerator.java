package backend.ir;

import backend.data.*;
import backend.utils.*;
import frontend.config.*;
import frontend.data.*;
import frontend.element.*;
import frontend.element.Number;
import frontend.symbol.*;

import java.util.*;

public class IrGenerator {
    private final CompUnit compUnit;
    private final SymbolTree symbolTree;
    private final IrList instrList = new IrList();
    private final IrList globalList = new IrList();
    private final Stack<String> breakLabels = new Stack<>();
    private final Stack<String> continueLabels = new Stack<>();
    private final HashMap<String, String> stringPool = new HashMap<>();
    private int tempCounter = 1;
    private int labelCounter = 1;
    private int stringCounter = 1;
    private int scopeIndex = 1;
    private boolean global = true;
    private boolean isStatic = false;

    public IrGenerator(CompUnit compUnit, SymbolTree symbolTree) {
        this.compUnit = compUnit;
        this.symbolTree = symbolTree;
    }

    private static String getIrName(String idenfr, int useLine, SymbolTree node) {
        return "ir_idenfr_" + idenfr + "_" + node.findSymbolRecursive(idenfr, useLine).getScopeId();
    }

    private String newTemp() {
        String temp = "ir_temp_" + (tempCounter++);
        addQuad("alloc", temp, null, "int");
        return temp;
    }

    private String newLabel() {
        return "ir_label_" + (labelCounter++);
    }

    private String newString() {
        return "ir_string_" + (stringCounter++);
    }

    private void addQuad(String op, String arg1, String arg2, String result) {
        Quadruple quad = new Quadruple(op, arg1, arg2, result);
        if (!global && !isStatic) {
            instrList.add(quad);
        } else {
            globalList.add(quad);
        }
    }

    // CompUnit → {Decl} {FuncDef} MainFuncDef
    public IrList generateCompUnit() {
        for (Decl decl : compUnit.getDecl()) {
            generateDecl(decl, symbolTree);
        }
        global = false;
        addQuad("call", "main", "0", null);
        for (FuncDef funcDef : compUnit.getFuncDef()) {
            generateFuncDef(funcDef);
        }
        generateMainFuncDef(compUnit.getMainFuncDef());
        globalList.addAll(instrList);
        return globalList;
    }

    // Decl → ConstDecl | VarDecl
    private void generateDecl(Decl decl, SymbolTree node) {
        if (decl.getDeclType() == Decl.DeclType.ConstDecl) {
            generateConstDecl(decl.getConstDecl(), node);
        } else {
            generateVarDecl(decl.getVarDecl(), node);
        }
    }

    // ConstDecl → <CONSTTK> BType ConstDef { <COMMA> ConstDef } <SEMICN>
    private void generateConstDecl(ConstDecl constDecl, SymbolTree node) {
        for (ConstDef constDef : constDecl.getConstDef()) {
            generateConstDef(constDef, node);
        }
    }

    // ConstDef → <IDENFR> [ <LBRACK> ConstExp <RBRACK> ] <ASSIGN> ConstInitVal
    private void generateConstDef(ConstDef constDef, SymbolTree node) {
        String name = generateIndex(constDef, node);
        generateConstInitVal(constDef.getConstInitVal(), name, node);
        isStatic = false;
    }

    private String generateIndex(Object defT, SymbolTree node) {
        Def def = (Def) defT;
        String idenfr = def.getIdenfr();
        String name = getIrName(idenfr, def.getLineIndex(), node);
        SymbolType type = node.findSymbolRecursive(idenfr, def.getLineIndex()).getSymbol(idenfr).getType();
        isStatic = (type == SymbolType.StaticInt || type == SymbolType.StaticIntArray);
        if (def.getConstExp() != null) {
            int size = Visitor.evaConstExp(def.getConstExp(), node);
            addQuad("array_alloc", name, String.valueOf(size), isStatic ? "static" : "int");
        } else {
            addQuad("alloc", name, null, isStatic ? "static" : "int");
        }
        return name;
    }

    // ConstInitVal → ConstExp | <LBRACE> [ ConstExp { <COMMA> ConstExp } ] <RBRACE>
    private void generateConstInitVal(ConstInitVal constInitVal, String name, SymbolTree node) {
        if (constInitVal.getConstInitValType() == ConstInitVal.ConstInitValType.ConstExp) {
            int value = Visitor.evaConstExp(constInitVal.getConstExp(), node);
            addQuad("assign", String.valueOf(value), null, name);
        } else {
            ArrayList<ConstExp> constExpArr = constInitVal.getConstExpArr();
            for (int i = 0; i < constExpArr.size(); i++) {
                int value = Visitor.evaConstExp(constExpArr.get(i), node);
                String index = String.valueOf(i);
                addQuad("store", String.valueOf(value), index, name);
            }
        }
    }

    // VarDecl → [ <STATICTK> ] BType VarDef { <COMMA> VarDef } <SEMICN>
    private void generateVarDecl(VarDecl varDecl, SymbolTree node) {
        for (VarDef varDef : varDecl.getVarDef()) {
            generateVarDef(varDef, node);
        }
    }

    // VarDef → <IDENFR> [ <LBRACK> ConstExp <RBRACK> ] | <IDENFR> [ <LBRACK>
    // ConstExp <RBRACK> ] <ASSIGN> InitVal
    private void generateVarDef(VarDef varDef, SymbolTree node) {
        String name = generateIndex(varDef, node);
        if (varDef.getVatDefType() == VarDef.VatDefType.Assign) {
            generateInitVal(varDef.getInitVal(), name, node);
        }
        isStatic = false;
    }

    // InitVal → Exp | <LBRACE> [ Exp { <COMMA> Exp } ] <RBRACE>
    private void generateInitVal(InitVal initVal, String name, SymbolTree node) {
        if (initVal.getInitValType() == InitVal.InitValType.Exp) {
            String value = generateExp(initVal.getExp(), node);
            addQuad("assign", value, null, name);
        } else {
            ArrayList<Exp> expArr = initVal.getExpArr();
            for (int i = 0; i < expArr.size(); i++) {
                String value = generateExp(expArr.get(i), node);
                String index = String.valueOf(i);
                addQuad("store", value, index, name);
            }
        }
    }

    // FuncDef → FuncType <IDENFR> <LPARENT> [FuncFParams] <RPARENT> Block
    private void generateFuncDef(FuncDef funcDef) {
        String funcName = funcDef.getIdenfr();
        String type = generateFuncType(funcDef.getFuncType());
        addQuad("func_begin", funcName, type, null);
        scopeIndex++;
        SymbolTree funcNode = symbolTree.getScope(scopeIndex);
        if (funcDef.getFuncFParams() != null) {
            generateFuncFParams(funcDef.getFuncFParams(), funcNode);
        }
        for (BlockItem blockItem : funcDef.getBlock().getBlockItem()) {
            generateBlockItem(blockItem, funcNode);
        }
        if (type.equals("void")) {
            Quadruple lastQuad = instrList.get(instrList.size() - 1);
            if (lastQuad != null && !lastQuad.op().equals("ret")) {
                addQuad("ret", null, null, null);
            }
        }
        addQuad("func_end", funcName, type, null);
    }

    // MainFuncDef → <INTTK> <MAINTK> <LPARENT> <RPARENT> Block
    private void generateMainFuncDef(MainFuncDef mainFuncDef) {
        addQuad("func_begin", "main", null, null);
        scopeIndex++;
        SymbolTree mainNode = symbolTree.getScope(scopeIndex);
        generateBlock(mainFuncDef.block(), mainNode);
        addQuad("func_end", null, null, null);
    }

    // FuncType → <VOIDTK> | <INTTK>
    private String generateFuncType(FuncType funcType) {
        return funcType.funcTypeType() == FuncType.FuncTypeType.Void ? "void" : "int";
    }

    // FuncFParams → FuncFParam { <COMMA> FuncFParam }
    private void generateFuncFParams(FuncFParams funcFParams, SymbolTree node) {
        ArrayList<FuncFParam> params = funcFParams.getFuncFParam();
        for (int i = 0; i < params.size(); i++) {
            generateFuncFParam(params.get(i), params.size() - i - 1, node);
        }
    }

    // FuncFParam → BType <IDENFR> [<LBRACK> <RBRACK>]
    private void generateFuncFParam(FuncFParam funcFParam, int index, SymbolTree node) {
        String paramName = getIrName(funcFParam.getIdenfr(), funcFParam.getLineIndex(), node);
        String type = funcFParam.getFuncFParamType() == FuncFParam.FuncFParamType.Int ? "int" : "array";
        addQuad("func_param", paramName, String.valueOf(index), type);
    }

    // Block → <LBRACE> { BlockItem } <RBRACE>
    private void generateBlock(Block block, SymbolTree node) {
        for (BlockItem blockItem : block.getBlockItem()) {
            generateBlockItem(blockItem, node);
        }
    }

    // BlockItem → Decl | Stmt
    private void generateBlockItem(BlockItem blockItem, SymbolTree node) {
        if (blockItem.getBlockItemType() == BlockItem.BlockItemType.Decl) {
            generateDecl(blockItem.getDecl(), node);
        } else {
            generateStmt(blockItem.getStmt(), node);
        }
    }

    // Stmt → LVal <ASSIGN> Exp <SEMICN>
    // | [Exp] <SEMICN>
    // | Block
    // | <IFTK> <LPARENT> Cond <RPARENT> Stmt [ <ELSETK> Stmt ]
    // | <FORTK> <LPARENT> [ForStmt] <SEMICN> [Cond] <SEMICN> [ForStmt] <RPARENT>
    // Stmt
    // | <BREAKTK> <SEMICN>
    // | <CONTINUETK> <SEMICN>
    // | <RETURNTK> [Exp] <SEMICN>
    // | <PRINTFTK> <LPARENT> <STRCON> { <COMMA> Exp } <RPARENT> <SEMICN>
    private void generateStmt(Stmt stmt, SymbolTree node) {
        switch (stmt.getStmtType()) {
            case LVal -> generateLvalStmt(stmt, node);
            case Exp -> {
                if (stmt.getExp() != null) {
                    generateExp(stmt.getExp(), node);
                }
            }
            case Block -> {
                scopeIndex++;
                SymbolTree blockNode = symbolTree.getScope(scopeIndex);
                generateBlock(stmt.getBlock(), blockNode);
            }
            case If -> generateIfStmt(stmt, node);
            case For -> generateForStmtPart(stmt, node);
            case Break -> {
                if (!breakLabels.isEmpty()) {
                    addQuad("j", null, null, breakLabels.peek());
                }
            }
            case Continue -> {
                if (!continueLabels.isEmpty()) {
                    addQuad("j", null, null, continueLabels.peek());
                }
            }
            case Return -> {
                if (stmt.getExpReturn() != null) {
                    String value = generateExp(stmt.getExpReturn(), node);
                    addQuad("ret", value, null, null);
                } else {
                    addQuad("ret", null, null, null);
                }
            }
            case Print -> generatePrintfStmt(stmt, node);
        }
    }

    // LVal <ASSIGN> Exp <SEMICN>
    private void generateLvalStmt(Stmt stmt, SymbolTree node) {
        LVal lVal = stmt.getlVal();
        String value = generateExp(stmt.getExpLVal(), node);
        if (lVal.getExp() == null) {
            addQuad("assign", value, null, getIrName(lVal.getIdenfr(), lVal.getLineIndex(), node));
        } else {
            String index = generateExp(lVal.getExp(), node);
            addQuad("store", value, index, getIrName(lVal.getIdenfr(), lVal.getLineIndex(), node));
        }
    }

    // <IFTK> <LPARENT> Cond <RPARENT> Stmt [ <ELSETK> Stmt ]
    private void generateIfStmt(Stmt stmt, SymbolTree node) {
        if (stmt.getStmtElse() == null) {
            String labelThen = newLabel();
            String labelEnd = newLabel();
            generateCond(stmt.getCondIf(), labelThen, labelEnd, node);
            addQuad("label", null, null, labelThen);
            generateStmt(stmt.getStmtIf(), node);
            addQuad("label", null, null, labelEnd);
        } else {
            String labelThen = newLabel();
            String labelElse = newLabel();
            String labelEnd = newLabel();
            generateCond(stmt.getCondIf(), labelThen, labelElse, node);
            addQuad("label", null, null, labelThen);
            generateStmt(stmt.getStmtIf(), node);
            addQuad("j", null, null, labelEnd);
            addQuad("label", null, null, labelElse);
            generateStmt(stmt.getStmtElse(), node);
            addQuad("label", null, null, labelEnd);
        }
    }

    // <FORTK> <LPARENT> [ForStmt] <SEMICN> [Cond] <SEMICN> [ForStmt] <RPARENT> Stmt
    private void generateForStmtPart(Stmt stmt, SymbolTree node) {
        String labelCond = newLabel();
        String labelBody = newLabel();
        String labelUpdate = newLabel();
        String labelEnd = newLabel();
        breakLabels.push(labelEnd);
        continueLabels.push(labelUpdate);
        if (stmt.getForStmtLeft() != null) {
            generateForStmt(stmt.getForStmtLeft(), node);
        }
        addQuad("label", null, null, labelCond);
        if (stmt.getCondFor() != null) {
            generateCond(stmt.getCondFor(), labelBody, labelEnd, node);
            addQuad("label", null, null, labelBody);
        }
        generateStmt(stmt.getStmtFor(), node);
        addQuad("label", null, null, labelUpdate);
        if (stmt.getForStmtRight() != null) {
            generateForStmt(stmt.getForStmtRight(), node);
        }
        addQuad("j", null, null, labelCond);
        addQuad("label", null, null, labelEnd);
        breakLabels.pop();
        continueLabels.pop();
    }

    // ForStmt → LVal <ASSIGN> Exp { <COMMA> LVal <ASSIGN> Exp }
    private void generateForStmt(ForStmt forStmt, SymbolTree node) {
        for (int i = 0; i < forStmt.getlVal().size(); i++) {
            LVal lVal = forStmt.getlVal().get(i);
            Exp exp = forStmt.getExp().get(i);
            String value = generateExp(exp, node);
            if (lVal.getExp() == null) {
                addQuad("assign", value, null, getIrName(lVal.getIdenfr(), lVal.getLineIndex(), node));
            } else {
                String index = generateExp(lVal.getExp(), node);
                addQuad("store", value, index, getIrName(lVal.getIdenfr(), lVal.getLineIndex(), node));
            }
        }
    }

    // <PRINTFTK> <LPARENT> <STRCON> { <COMMA> Exp } <RPARENT> <SEMICN>
    private void generatePrintfStmt(Stmt stmt, SymbolTree node) {
        String format = stmt.getStrCon();
        String[] parts = format.split("((?<=%d)|(?=%d))");
        ArrayList<Exp> expList = stmt.getExpPrint();
        ArrayList<String> values = new ArrayList<>();
        for (Exp exp : expList) {
            values.add(generateExp(exp, node));
        }
        for (int i = 0, j = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.equals("%d")) {
                addQuad("printf", values.get(j), null, null);
                j++;
            } else if (!part.isEmpty()) {
                String str = stringPool.containsKey(part) ? stringPool.get(part) : newString();
                stringPool.put(part, str);
                addQuad("print", part, null, str);
            }
        }
    }

    // Exp → AddExp
    private String generateExp(Exp exp, SymbolTree node) {
        return generateAddExp(exp.addExp(), node);
    }

    // Cond → LOrExp
    private void generateCond(Cond cond, String labelTrue, String labelFalse, SymbolTree node) {
        generateLOrExp(cond.lOrExp(), labelTrue, labelFalse, node);
    }

    // LVal → <IDENFR> [<LBRACK> Exp <RBRACK>]
    private String generateLVal(LVal lVal, SymbolTree node) {
        if (lVal.getExp() == null) {
            return getIrName(lVal.getIdenfr(), lVal.getLineIndex(), node);
        } else {
            String index = generateExp(lVal.getExp(), node);
            String temp = newTemp();
            addQuad("load", getIrName(lVal.getIdenfr(), lVal.getLineIndex(), node), index, temp);
            return temp;
        }
    }

    // PrimaryExp → <LPARENT> Exp <RPARENT> | LVal | Number
    private String generatePrimaryExp(PrimaryExp primaryExp, SymbolTree node) {
        return switch (primaryExp.getPrimaryExpType()) {
            case Exp -> generateExp(primaryExp.getExp(), node);
            case LVal -> generateLVal(primaryExp.getLval(), node);
            case Number -> generateNumber(primaryExp.getNumber());
        };
    }

    // Number → <INTCON>
    private String generateNumber(Number number) {
        return number.number();
    }

    // UnaryExp → PrimaryExp | <IDENFR> <LPARENT> [FuncRParams] <RPARENT> | UnaryOp UnaryExp
    private String generateUnaryExp(UnaryExp unaryExp, SymbolTree node) {
        return switch (unaryExp.getUnaryExpType()) {
            case PrimaryExp -> generatePrimaryExp(unaryExp.getPrimaryExp(), node);
            case FuncRParams -> generateFuncCall(unaryExp, node);
            case UnaryOp -> generateUnaryOp(unaryExp, node);
        };
    }

    // <IDENFR> <LPARENT> [FuncRParams] <RPARENT>
    private String generateFuncCall(UnaryExp unaryExp, SymbolTree node) {
        String funcName = unaryExp.getIdenfr();
        if (funcName.equals("getint")) {
            String result = newTemp();
            addQuad("get_int", null, null, result);
            return result;
        }
        String result = newTemp();
        int paramCount = 0;
        if (unaryExp.getFuncRParams() != null) {
            generateFuncRParams(unaryExp.getFuncRParams(), node);
            paramCount = unaryExp.getFuncRParams().getExp().size();
        }
        addQuad("call", funcName, String.valueOf(paramCount), result);
        return result;
    }

    // UnaryOp → <PLUS> | <MINU> | <NOT>
    private String generateUnaryOp(UnaryExp unaryExp, SymbolTree node) {
        String operand = generateUnaryExp(unaryExp.getUnaryExp(), node);
        UnaryOp.UnaryOpType opType = unaryExp.getUnaryOp().unaryOpType();
        return switch (opType) {
            case Plus -> operand;
            case Minu -> generateSingle("subu", operand);
            case Not -> generateSingle("seq", operand);
        };
    }

    private String generateSingle(String op, String operand) {
        String temp = newTemp();
        if (Calculate.isNumber(operand)) {
            int res = Calculate.getRes(op, "0", operand);
            addQuad("assign", String.valueOf(res), null, temp);
        } else {
            addQuad(op, "0", operand, temp);
        }
        return temp;
    }

    // FuncRParams → Exp { <COMMA> Exp }
    private void generateFuncRParams(FuncRParams funcRParams, SymbolTree node) {
        for (Exp exp : funcRParams.getExp()) {
            String value = generateExp(exp, node);
            SymbolType symbolType = Visitor.getExpType(exp, node);
            String type = (symbolType == SymbolType.IntFunc || symbolType == SymbolType.Int || symbolType == SymbolType.StaticInt || symbolType == SymbolType.ConstInt) ? "int" : "array";
            addQuad("param", value, type, null);
        }
    }

    // MulExp → UnaryExp { ( <MULT> | <DIV> | <MOD> ) UnaryExp }
    private String generateMulExp(MulExp mulExp, SymbolTree node) {
        ArrayList<UnaryExp> unaryExps = mulExp.getUnaryExp();
        ArrayList<MulExp.MulExpType> ops = mulExp.getMulExpType();
        String result = generateUnaryExp(unaryExps.get(0), node);
        for (int i = 1; i < unaryExps.size(); i++) {
            String right = generateUnaryExp(unaryExps.get(i), node);
            String op = switch (ops.get(i - 1)) {
                case Mult -> "mulu";
                case Div -> "div";
                case Mod -> "mod";
            };
            result = generateCalculate(right, result, op);
        }
        return result;
    }

    private String generateCalculate(String right, String result, String op) {
        if (Calculate.isNumber(right) && Calculate.isNumber(result)) {
            String temp = newTemp();
            int res = Calculate.getRes(op, result, right);
            addQuad("assign", String.valueOf(res), null, temp);
            return temp;
        } else if (op.equals("mulu")) {
            return generateMul(right, result);
        } else {
            String temp = newTemp();
            addQuad(op, result, right, temp);
            return temp;
        }
    }

    private String generateMul(String right, String result) {
        String s = generateConst(right, result);
        if (s != null) {
            return s;
        }
        s = generateConst(result, right);
        if (s != null) {
            return s;
        }
        String temp = newTemp();
        int l = Calculate.getPower(result);
        int r = Calculate.getPower(right);
        if (r != -1) {
            addQuad("sllv", result, String.valueOf(r), temp);
        } else if (l != -1) {
            addQuad("sllv", right, String.valueOf(l), temp);
        } else {
            addQuad("mulu", result, right, temp);
        }
        return temp;
    }

    private String generateConst(String result, String other) {
        if (Calculate.isNumber(result)) {
            int num = Integer.parseInt(result);
            if (num == 1) {
                return other;
            } else if (num == 0) {
                return "0";
            }
        }
        return null;
    }

    // AddExp → MulExp { ( <PLUS> | <MINU> ) MulExp }
    private String generateAddExp(AddExp addExp, SymbolTree node) {
        ArrayList<MulExp> mulExps = addExp.getMulExp();
        ArrayList<AddExp.AddExpType> ops = addExp.getAddExpType();
        String result = generateMulExp(mulExps.get(0), node);
        for (int i = 1; i < mulExps.size(); i++) {
            String right = generateMulExp(mulExps.get(i), node);
            String op = ops.get(i - 1) == AddExp.AddExpType.Plus ? "addu" : "subu";
            result = generateCalculate(right, result, op);
        }
        return result;
    }

    // RelExp → AddExp { ( <LSS> | <GRE> | <LEQ> | <GEQ> ) AddExp }
    private String generateRelExp(RelExp relExp, SymbolTree node) {
        ArrayList<AddExp> addExps = relExp.getAddExp();
        ArrayList<RelExp.RelExpType> ops = relExp.getRelExpType();
        String result = generateAddExp(addExps.get(0), node);
        for (int i = 1; i < addExps.size(); i++) {
            String right = generateAddExp(addExps.get(i), node);
            String op = switch (ops.get(i - 1)) {
                case Lss -> "slt";
                case Gre -> "sgt";
                case Leq -> "sle";
                case Geq -> "sge";
            };
            result = generateCalculate(right, result, op);
        }
        return result;
    }

    // EqExp → RelExp { ( <EQL> | <NEQ> ) RelExp }
    private String generateEqExp(EqExp eqExp, SymbolTree node) {
        ArrayList<RelExp> relExps = eqExp.getRelExp();
        ArrayList<EqExp.EqExpType> ops = eqExp.getEqExpType();
        String result = generateRelExp(relExps.get(0), node);
        for (int i = 1; i < relExps.size(); i++) {
            String right = generateRelExp(relExps.get(i), node);
            String op = ops.get(i - 1) == EqExp.EqExpType.Eql ? "seq" : "sne";
            result = generateCalculate(right, result, op);
        }
        return result;
    }

    // LAndExp → EqExp { <AND> EqExp }
    private void generateLAndExp(LAndExp lAndExp, String labelTrue, String labelFalse, SymbolTree node) {
        ArrayList<EqExp> eqExps = lAndExp.getEqExp();
        for (EqExp eqExp : eqExps) {
            String result = generateEqExp(eqExp, node);
            addQuad("beq", result, "0", labelFalse);
        }
        addQuad("j", null, null, labelTrue);
    }

    // LOrExp → LAndExp { <OR> LAndExp }
    private void generateLOrExp(LOrExp lOrExp, String labelTrue, String labelFalse, SymbolTree node) {
        ArrayList<LAndExp> lAndExps = lOrExp.getlAndExp();
        for (int i = 0; i < lAndExps.size() - 1; i++) {
            LAndExp lAndExp = lAndExps.get(i);
            String labelNext = newLabel();
            generateLAndExp(lAndExp, labelTrue, labelNext, node);
            addQuad("label", null, null, labelNext);
        }
        if (!lAndExps.isEmpty()) {
            generateLAndExp(lAndExps.get(lAndExps.size() - 1), labelTrue, labelFalse, node);
        }
    }
}