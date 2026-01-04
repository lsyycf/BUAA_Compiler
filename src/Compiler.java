import backend.data.*;
import backend.ir.*;
import backend.mips.*;
import frontend.data.*;
import frontend.element.*;
import frontend.lexer.*;
import frontend.parser.*;
import frontend.symbol.*;
import frontend.utils.*;
import optimize.*;

import java.io.*;
import java.util.*;

public class Compiler {
    private static final ErrorList errorList = new ErrorList();
    private static final Version version = Version.code;
    private static final boolean optimize = true;

    public static void main(String[] args) throws IOException {
        ArrayList<String> lines = FileIO.readlines("testfile.txt");
        Lexer lexer = new Lexer(lines);
        TokenList tokenList = lexer.tokenizer();
        errorList.addAllError(lexer.getErrorList());
        FileIO.writefile("lexer.txt", tokenList.toString());
        if (version != Version.lexer) {
            Parser parser = new Parser(lexer.getTokenList());
            CompUnit compUnit = parser.parseCompUnit();
            errorList.addAllError(parser.getErrorList());
            FileIO.writefile("parser.txt", compUnit.toString());
            if (version != Version.parser) {
                Visitor visitor = new Visitor(compUnit);
                SymbolTree symbolTree = visitor.visitCompUnit();
                errorList.addAllError(visitor.getErrorList());
                FileIO.writefile("symbol.txt", symbolTree.toString());
                if (version == Version.code && !errorList.isFatal()) {
                    IrGenerator irGenerator = new IrGenerator(compUnit, symbolTree);
                    IrList irList = irGenerator.generateCompUnit();
                    if (optimize) {
                        Optimize optimize = new Optimize(irList);
                        irList = optimize.optimize();
                    }
                    FileIO.writefile("ir.txt", irList.toString());
                    MipsGenerator mipsGenerator = new MipsGenerator(irList);
                    String mipsCode = mipsGenerator.generate();
                    FileIO.writefile("mips.txt", mipsCode);
                }
            }
        }
        FileIO.writefile("error.txt", errorList.toString());
    }

    public enum Version {lexer, parser, symbol, code}
}
