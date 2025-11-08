package backend.ir;

import backend.data.IrList;
import backend.data.Quadruple;
import frontend.config.SymbolType;
import frontend.data.SymbolTree;
import frontend.element.AddExp;
import frontend.element.Block;
import frontend.element.BlockItem;
import frontend.element.CompUnit;
import frontend.element.Cond;
import frontend.element.ConstDecl;
import frontend.element.ConstDef;
import frontend.element.ConstExp;
import frontend.element.ConstInitVal;
import frontend.element.Decl;
import frontend.element.EqExp;
import frontend.element.Exp;
import frontend.element.ForStmt;
import frontend.element.FuncDef;
import frontend.element.FuncFParam;
import frontend.element.FuncFParams;
import frontend.element.FuncRParams;
import frontend.element.FuncType;
import frontend.element.InitVal;
import frontend.element.LAndExp;
import frontend.element.LOrExp;
import frontend.element.LVal;
import frontend.element.MainFuncDef;
import frontend.element.MulExp;
import frontend.element.Number;
import frontend.element.PrimaryExp;
import frontend.element.RelExp;
import frontend.element.Stmt;
import frontend.element.UnaryExp;
import frontend.element.UnaryOp;
import frontend.element.VarDecl;
import frontend.element.VarDef;
import frontend.symbol.Visitor;

import java.util.ArrayList;
import java.util.Stack;

public class IrGenerator {
    private final CompUnit compUnit;
    private final SymbolTree symbolTree;
    private final IrList instrList = new IrList();
    private final IrList globalList = new IrList();
    private final Stack<String> breakLabels = new Stack<>();
    private final Stack<String> continueLabels = new Stack<>();
    private SymbolTree currentScope;
    private int tempCounter = 1;
    private int labelCounter = 1;
    private int stringCounter = 1;
    private int scopeIndex = 1;
    private boolean global = true;
    private boolean isStatic = false;

    public IrGenerator(CompUnit compUnit, SymbolTree symbolTree) {
        this.compUnit = compUnit;
        this.currentScope = symbolTree;
        this.symbolTree = symbolTree;
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
            generateDecl(decl);
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
    private void generateDecl(Decl decl) {
        if (decl.getDeclType() == Decl.DeclType.ConstDecl) {
            generateConstDecl(decl.getConstDecl());
        } else {
            generateVarDecl(decl.getVarDecl());
        }
    }

    // ConstDecl → <CONSTTK> BType ConstDef { <COMMA> ConstDef } <SEMICN>
    private void generateConstDecl(ConstDecl constDecl) {
        for (ConstDef constDef : constDecl.getConstDef()) {
            generateConstDef(constDef);
        }
    }

    // ConstDef → <IDENFR> [ <LBRACK> ConstExp <RBRACK> ] <ASSIGN> ConstInitVal
    private void generateConstDef(ConstDef constDef) {
        String idenfr = constDef.getIdenfr();
        String name = getIrName(idenfr, constDef.getLineIndex());
        SymbolType type = currentScope.findSymbolRecursive(idenfr, constDef.getLineIndex()).getSymbol(idenfr).getType();
        isStatic = (type == SymbolType.StaticInt || type == SymbolType.StaticIntArray);
        if (constDef.getConstExp() != null) {
            int size = Visitor.evaConstExp(constDef.getConstExp(), currentScope);
            addQuad("array_alloc", name, String.valueOf(size), getFlag());
        } else {
            addQuad("alloc", name, null, getFlag());
        }
        generateConstInitVal(constDef.getConstInitVal(), name);
        isStatic = false;
    }

    // ConstInitVal → ConstExp | <LBRACE> [ ConstExp { <COMMA> ConstExp } ] <RBRACE>
    private void generateConstInitVal(ConstInitVal constInitVal, String name) {
        if (constInitVal.getConstInitValType() == ConstInitVal.ConstInitValType.ConstExp) {
            int value = Visitor.evaConstExp(constInitVal.getConstExp(), currentScope);
            addQuad("assign", String.valueOf(value), null, name);
        } else {
            ArrayList<ConstExp> constExpArr = constInitVal.getConstExpArr();
            for (int i = 0; i < constExpArr.size(); i++) {
                int value = Visitor.evaConstExp(constExpArr.get(i), currentScope);
                String index = String.valueOf(i);
                addQuad("store", String.valueOf(value), index, name);
            }
        }
    }

    // VarDecl → [ <STATICTK> ] BType VarDef { <COMMA> VarDef } <SEMICN>
    private void generateVarDecl(VarDecl varDecl) {
        for (VarDef varDef : varDecl.getVarDef()) {
            generateVarDef(varDef);
        }
    }

    // VarDef → <IDENFR> [ <LBRACK> ConstExp <RBRACK> ] | <IDENFR> [ <LBRACK> ConstExp <RBRACK> ] <ASSIGN> InitVal
    private void generateVarDef(VarDef varDef) {
        String idenfr = varDef.getIdenfr();
        String name = getIrName(idenfr, varDef.getLineIndex());
        SymbolType type = currentScope.findSymbolRecursive(idenfr, varDef.getLineIndex()).getSymbol(idenfr).getType();
        isStatic = (type == SymbolType.StaticInt || type == SymbolType.StaticIntArray);
        if (varDef.getConstExp() != null) {
            int size = Visitor.evaConstExp(varDef.getConstExp(), currentScope);
            addQuad("array_alloc", name, String.valueOf(size), getFlag());
        } else {
            addQuad("alloc", name, null, getFlag());
        }
        if (varDef.getVatDefType() == VarDef.VatDefType.Assign) {
            generateInitVal(varDef.getInitVal(), name);
        }
        isStatic = false;
    }

    // InitVal → Exp | <LBRACE> [ Exp { <COMMA> Exp } ] <RBRACE>
    private void generateInitVal(InitVal initVal, String name) {
        if (initVal.getInitValType() == InitVal.InitValType.Exp) {
            String value = generateExp(initVal.getExp());
            addQuad("assign", value, null, name);
        } else {
            ArrayList<Exp> expArr = initVal.getExpArr();
            for (int i = 0; i < expArr.size(); i++) {
                String value = generateExp(expArr.get(i));
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
        currentScope = symbolTree.getScope(scopeIndex);
        if (funcDef.getFuncFParams() != null) {
            generateFuncFParams(funcDef.getFuncFParams());
        }
        for (BlockItem blockItem : funcDef.getBlock().getBlockItem()) {
            generateBlockItem(blockItem);
        }
        if (type.equals("void")) {
            Quadruple lastQuad = instrList.get(instrList.size() - 1);
            if (lastQuad != null && !lastQuad.op().equals("ret")) {
                addQuad("ret", null, null, null);
            }
        }
        addQuad("func_end", funcName, type, null);
        currentScope = currentScope.getParentTree();
    }

    // MainFuncDef → <INTTK> <MAINTK> <LPARENT> <RPARENT> Block
    private void generateMainFuncDef(MainFuncDef mainFuncDef) {
        addQuad("main", null, null, null);
        generateBlock(mainFuncDef.block());
        addQuad("exit", null, null, null);
    }

    // FuncType → <VOIDTK> | <INTTK>
    private String generateFuncType(FuncType funcType) {
        return funcType.funcTypeType() == FuncType.FuncTypeType.Void ? "void" : "int";
    }

    // FuncFParams → FuncFParam { <COMMA> FuncFParam }
    private void generateFuncFParams(FuncFParams funcFParams) {
        ArrayList<FuncFParam> params = funcFParams.getFuncFParam();
        for (int i = 0; i < params.size(); i++) {
            generateFuncFParam(params.get(i), params.size() - i - 1);
        }
    }

    // FuncFParam → BType <IDENFR> [<LBRACK> <RBRACK>]
    private void generateFuncFParam(FuncFParam funcFParam, int index) {
        String paramName = getIrName(funcFParam.getIdenfr(), funcFParam.getLineIndex());
        String type = funcFParam.getFuncFParamType() == FuncFParam.FuncFParamType.Int ? "int" : "array";
        addQuad("func_param", paramName, String.valueOf(index), type);
    }

    // Block → <LBRACE> { BlockItem } <RBRACE>
    private void generateBlock(Block block) {
        scopeIndex++;
        currentScope = symbolTree.getScope(scopeIndex);
        for (BlockItem blockItem : block.getBlockItem()) {
            generateBlockItem(blockItem);
        }
        currentScope = currentScope.getParentTree();
    }

    // BlockItem → Decl | Stmt
    private void generateBlockItem(BlockItem blockItem) {
        if (blockItem.getBlockItemType() == BlockItem.BlockItemType.Decl) {
            generateDecl(blockItem.getDecl());
        } else {
            generateStmt(blockItem.getStmt());
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
    private void generateStmt(Stmt stmt) {
        switch (stmt.getStmtType()) {
            case LVal:
                generateAssignStmt(stmt);
                break;
            case Exp:
                if (stmt.getExp() != null) {
                    generateExp(stmt.getExp());
                }
                break;
            case Block:
                generateBlock(stmt.getBlock());
                break;
            case If:
                generateIfStmt(stmt);
                break;
            case For:
                generateForStmtPart(stmt);
                break;
            case Break:
                if (!breakLabels.isEmpty()) {
                    addQuad("j", null, null, breakLabels.peek());
                }
                break;
            case Continue:
                if (!continueLabels.isEmpty()) {
                    addQuad("j", null, null, continueLabels.peek());
                }
                break;
            case Return:
                if (stmt.getExpReturn() != null) {
                    String value = generateExp(stmt.getExpReturn());
                    addQuad("ret", value, null, null);
                } else {
                    addQuad("ret", null, null, null);
                }
                break;
            case Print:
                generatePrintfStmt(stmt);
                break;
        }
    }

    // LVal <ASSIGN> Exp <SEMICN>
    private void generateAssignStmt(Stmt stmt) {
        LVal lVal = stmt.getlVal();
        String value = generateExp(stmt.getExpLVal());
        if (lVal.getExp() == null) {
            addQuad("assign", value, null, getIrName(lVal.getIdenfr(), lVal.getLineIndex()));
        } else {
            String index = generateExp(lVal.getExp());
            addQuad("store", value, index, getIrName(lVal.getIdenfr(), lVal.getLineIndex()));
        }
    }

    // <IFTK> <LPARENT> Cond <RPARENT> Stmt [ <ELSETK> Stmt ]
    private void generateIfStmt(Stmt stmt) {
        if (stmt.getStmtElse() == null) {
            String labelThen = newLabel();
            String labelEnd = newLabel();
            generateCond(stmt.getCondIf(), labelThen, labelEnd);
            addQuad("label", null, null, labelThen);
            generateStmt(stmt.getStmtIf());
            addQuad("label", null, null, labelEnd);
        } else {
            String labelThen = newLabel();
            String labelElse = newLabel();
            String labelEnd = newLabel();
            generateCond(stmt.getCondIf(), labelThen, labelElse);
            addQuad("label", null, null, labelThen);
            generateStmt(stmt.getStmtIf());
            addQuad("j", null, null, labelEnd);
            addQuad("label", null, null, labelElse);
            generateStmt(stmt.getStmtElse());
            addQuad("label", null, null, labelEnd);
        }
    }

    // <FORTK> <LPARENT> [ForStmt] <SEMICN> [Cond] <SEMICN> [ForStmt] <RPARENT> Stmt
    private void generateForStmtPart(Stmt stmt) {
        String labelCond = newLabel();
        String labelBody = newLabel();
        String labelUpdate = newLabel();
        String labelEnd = newLabel();
        breakLabels.push(labelEnd);
        continueLabels.push(labelUpdate);
        if (stmt.getForStmtLeft() != null) {
            generateForStmt(stmt.getForStmtLeft());
        }
        addQuad("label", null, null, labelCond);
        if (stmt.getCondFor() != null) {
            generateCond(stmt.getCondFor(), labelBody, labelEnd);
            addQuad("label", null, null, labelBody);
        }
        generateStmt(stmt.getStmtFor());
        addQuad("label", null, null, labelUpdate);
        if (stmt.getForStmtRight() != null) {
            generateForStmt(stmt.getForStmtRight());
        }
        addQuad("j", null, null, labelCond);
        addQuad("label", null, null, labelEnd);
        breakLabels.pop();
        continueLabels.pop();
    }

    // ForStmt → LVal <ASSIGN> Exp { <COMMA> LVal <ASSIGN> Exp }
    private void generateForStmt(ForStmt forStmt) {
        for (int i = 0; i < forStmt.getlVal().size(); i++) {
            LVal lVal = forStmt.getlVal().get(i);
            Exp exp = forStmt.getExp().get(i);
            String value = generateExp(exp);
            if (lVal.getExp() == null) {
                addQuad("assign", value, null, getIrName(lVal.getIdenfr(), lVal.getLineIndex()));
            } else {
                String index = generateExp(lVal.getExp());
                addQuad("store", value, index, getIrName(lVal.getIdenfr(), lVal.getLineIndex()));
            }
        }
    }

    // <PRINTFTK> <LPARENT> <STRCON> { <COMMA> Exp } <RPARENT> <SEMICN>
    private void generatePrintfStmt(Stmt stmt) {
        String format = stmt.getStrCon();
        String[] parts = format.split("((?<=%d)|(?=%d))");
        ArrayList<Exp> expList = stmt.getExpPrint();
        ArrayList<String> values = new ArrayList<>();
        for (Exp exp : expList) {
            values.add(generateExp(exp));
        }
        for (int i = 0, j = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.equals("%d")) {
                addQuad("printf", values.get(j), null, null);
                j++;
            } else if (!part.isEmpty()) {
                String str = newString();
                addQuad("print", part, null, str);
            }
        }
    }

    // Exp → AddExp
    private String generateExp(Exp exp) {
        return generateAddExp(exp.addExp());
    }

    // Cond → LOrExp
    private void generateCond(Cond cond, String labelTrue, String labelFalse) {
        generateLOrExp(cond.lOrExp(), labelTrue, labelFalse);
    }

    // LVal → <IDENFR> [<LBRACK> Exp <RBRACK>]
    private String generateLVal(LVal lVal) {
        if (lVal.getExp() == null) {
            return getIrName(lVal.getIdenfr(), lVal.getLineIndex());
        } else {
            String index = generateExp(lVal.getExp());
            String temp = newTemp();
            addQuad("load", getIrName(lVal.getIdenfr(), lVal.getLineIndex()), index, temp);
            return temp;
        }
    }

    // PrimaryExp → <LPARENT> Exp <RPARENT> | LVal | Number
    private String generatePrimaryExp(PrimaryExp primaryExp) {
        return switch (primaryExp.getPrimaryExpType()) {
            case Exp -> generateExp(primaryExp.getExp());
            case LVal -> generateLVal(primaryExp.getLval());
            case Number -> generateNumber(primaryExp.getNumber());
        };
    }

    // Number → <INTCON>
    private String generateNumber(Number number) {
        return number.number();
    }

    // UnaryExp → PrimaryExp | <IDENFR> <LPARENT> [FuncRParams] <RPARENT> | UnaryOp UnaryExp
    private String generateUnaryExp(UnaryExp unaryExp) {
        return switch (unaryExp.getUnaryExpType()) {
            case PrimaryExp -> generatePrimaryExp(unaryExp.getPrimaryExp());
            case FuncRParams -> generateFuncCall(unaryExp);
            case UnaryOp -> generateUnaryOp(unaryExp);
        };
    }

    // <IDENFR> <LPARENT> [FuncRParams] <RPARENT>
    private String generateFuncCall(UnaryExp unaryExp) {
        String funcName = unaryExp.getIdenfr();
        if (funcName.equals("getint")) {
            String result = newTemp();
            addQuad("get_int", null, null, result);
            return result;
        }
        String result = newTemp();
        int paramCount = 0;
        if (unaryExp.getFuncRParams() != null) {
            generateFuncRParams(unaryExp.getFuncRParams());
            paramCount = unaryExp.getFuncRParams().getExp().size();
        }
        addQuad("call", funcName, String.valueOf(paramCount), result);
        return result;
    }

    // UnaryOp → <PLUS> | <MINU> | <NOT>
    private String generateUnaryOp(UnaryExp unaryExp) {
        String operand = generateUnaryExp(unaryExp.getUnaryExp());
        UnaryOp.UnaryOpType opType = unaryExp.getUnaryOp().unaryOpType();
        if (opType == UnaryOp.UnaryOpType.Plus) {
            return operand;
        } else if (opType == UnaryOp.UnaryOpType.Minu) {
            String temp = newTemp();
            if (isNumber(operand)) {
                int res = getRes("sub", "0", operand);
                addQuad("assign", String.valueOf(res), null, temp);
            } else {
                addQuad("sub", "0", operand, temp);
            }
            return temp;
        } else {
            String temp = newTemp();
            if (isNumber(operand)) {
                int res = getRes("eq", "0", operand);
                addQuad("assign", String.valueOf(res), null, temp);
            } else {
                addQuad("eq", "0", operand, temp);
            }
            return temp;
        }
    }

    // FuncRParams → Exp { <COMMA> Exp }
    private void generateFuncRParams(FuncRParams funcRParams) {
        for (Exp exp : funcRParams.getExp()) {
            String value = generateExp(exp);
            SymbolType symbolType = Visitor.getExpType(exp, currentScope);
            String type = (symbolType == SymbolType.IntFunc || symbolType == SymbolType.Int || symbolType == SymbolType.StaticInt || symbolType == SymbolType.ConstInt) ? "int" : "array";
            addQuad("param", value, type, null);
        }
    }

    // MulExp → UnaryExp { ( <MULT> | <DIV> | <MOD> ) UnaryExp }
    private String generateMulExp(MulExp mulExp) {
        ArrayList<UnaryExp> unaryExps = mulExp.getUnaryExp();
        ArrayList<MulExp.MulExpType> ops = mulExp.getMulExpType();
        String result = generateUnaryExp(unaryExps.get(0));
        for (int i = 1; i < unaryExps.size(); i++) {
            String right = generateUnaryExp(unaryExps.get(i));
            String temp = newTemp();
            String op = switch (ops.get(i - 1)) {
                case Mult -> "mul";
                case Div -> "div";
                case Mod -> "mod";
            };
            if (isNumber(right) && isNumber(result)) {
                int res = getRes(op, result, right);
                addQuad("assign", String.valueOf(res), null, temp);
            } else {
                addQuad(op, result, right, temp);
            }
            result = temp;
        }
        return result;
    }

    // AddExp → MulExp { ( <PLUS> | <MINU> ) MulExp }
    private String generateAddExp(AddExp addExp) {
        ArrayList<MulExp> mulExps = addExp.getMulExp();
        ArrayList<AddExp.AddExpType> ops = addExp.getAddExpType();
        String result = generateMulExp(mulExps.get(0));
        for (int i = 1; i < mulExps.size(); i++) {
            String right = generateMulExp(mulExps.get(i));
            String temp = newTemp();
            String op = switch (ops.get(i - 1)) {
                case Plus -> "add";
                case Minu -> "sub";
            };
            if (isNumber(right) && isNumber(result)) {
                int res = getRes(op, result, right);
                addQuad("assign", String.valueOf(res), null, temp);
            } else {
                addQuad(op, result, right, temp);
            }
            result = temp;
        }
        return result;
    }

    // RelExp → AddExp { ( <LSS> | <GRE> | <LEQ> | <GEQ> ) AddExp }
    private String generateRelExp(RelExp relExp) {
        ArrayList<AddExp> addExps = relExp.getAddExp();
        ArrayList<RelExp.RelExpType> ops = relExp.getRelExpType();
        String result = generateAddExp(addExps.get(0));
        for (int i = 1; i < addExps.size(); i++) {
            String right = generateAddExp(addExps.get(i));
            String temp = newTemp();
            String op = switch (ops.get(i - 1)) {
                case Lss -> "lt";
                case Gre -> "gt";
                case Leq -> "leq";
                case Geq -> "geq";
            };
            if (isNumber(right) && isNumber(result)) {
                int res = getRes(op, result, right);
                addQuad("assign", String.valueOf(res), null, temp);
            } else {
                addQuad(op, result, right, temp);
            }
            result = temp;
        }
        return result;
    }

    // EqExp → RelExp { ( <EQL> | <NEQ> ) RelExp }
    private String generateEqExp(EqExp eqExp) {
        ArrayList<RelExp> relExps = eqExp.getRelExp();
        ArrayList<EqExp.EqExpType> ops = eqExp.getEqExpType();
        String result = generateRelExp(relExps.get(0));
        for (int i = 1; i < relExps.size(); i++) {
            String right = generateRelExp(relExps.get(i));
            String temp = newTemp();
            String op = switch (ops.get(i - 1)) {
                case Eql -> "eq";
                case Neq -> "neq";
            };
            if (isNumber(right) && isNumber(result)) {
                int res = getRes(op, result, right);
                addQuad("assign", String.valueOf(res), null, temp);
            } else {
                addQuad(op, result, right, temp);
            }
            result = temp;
        }
        return result;
    }

    // LAndExp → EqExp { <AND> EqExp }
    private void generateLAndExp(LAndExp lAndExp, String labelTrue, String labelFalse) {
        ArrayList<EqExp> eqExps = lAndExp.getEqExp();
        for (EqExp eqExp : eqExps) {
            String result = generateEqExp(eqExp);
            addQuad("beq", result, "0", labelFalse);
        }
        addQuad("j", null, null, labelTrue);
    }

    // LOrExp → LAndExp { <OR> LAndExp }
    private void generateLOrExp(LOrExp lOrExp, String labelTrue, String labelFalse) {
        ArrayList<LAndExp> lAndExps = lOrExp.getlAndExp();
        for (int i = 0; i < lAndExps.size(); i++) {
            LAndExp lAndExp = lAndExps.get(i);
            if (i < lAndExps.size() - 1) {
                String labelNext = newLabel();
                generateLAndExp(lAndExp, labelTrue, labelNext);
                addQuad("label", null, null, labelNext);
            } else {
                generateLAndExp(lAndExp, labelTrue, labelFalse);
            }
        }
    }


    private String getFlag() {
        return isStatic ? "static" : "int";
    }

    private String getIrName(String idenfr, int useLine) {
        return "ir_idenfr_" + idenfr + "_" + currentScope.findSymbolRecursive(idenfr, useLine).getScopeId();
    }

    private int getRes(String op, String arg1, String arg2) {
        int val1 = Integer.parseInt(arg1);
        int val2 = Integer.parseInt(arg2);
        return switch (op) {
            case "add" -> val1 + val2;
            case "sub" -> val1 - val2;
            case "mul" -> val1 * val2;
            case "div" -> val1 / val2;
            case "mod" -> val1 % val2;
            case "lt" -> (val1 < val2) ? 1 : 0;
            case "gt" -> (val1 > val2) ? 1 : 0;
            case "leq" -> (val1 <= val2) ? 1 : 0;
            case "geq" -> (val1 >= val2) ? 1 : 0;
            case "eq" -> (val1 == val2) ? 1 : 0;
            case "neq" -> (val1 != val2) ? 1 : 0;
            default -> 0;
        };
    }

    private boolean isNumber(String str) {
        return str.matches("^-?[0-9]+$");
    }
}