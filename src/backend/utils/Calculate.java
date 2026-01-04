package backend.utils;

public class Calculate {
    public static int getRes(String op, String arg1, String arg2) {
        int val1 = Integer.parseInt(arg1);
        int val2 = Integer.parseInt(arg2);
        return switch (op) {
            case "addu" -> val1 + val2;
            case "subu" -> val1 - val2;
            case "mulu" -> val1 * val2;
            case "div" -> val1 / val2;
            case "mod" -> val1 % val2;
            case "slt" -> (val1 < val2) ? 1 : 0;
            case "sgt" -> (val1 > val2) ? 1 : 0;
            case "sle" -> (val1 <= val2) ? 1 : 0;
            case "sge" -> (val1 >= val2) ? 1 : 0;
            case "seq" -> (val1 == val2) ? 1 : 0;
            case "sne" -> (val1 != val2) ? 1 : 0;
            case "sllv" -> val1 << val2;
            case "srav" -> val1 >> val2;
            case "srlv" -> val1 >>> val2;
            case "and" -> val1 & val2;
            default -> 0;
        };
    }

    public static boolean canCalculate(String op) {
        return op.equals("addu") || op.equals("subu") || op.equals("mulu") || op.equals("div") || op.equals("mod") || op.equals("slt") || op.equals("sgt") || op.equals("sle") || op.equals("sge") || op.equals("seq") || op.equals("sne") || op.equals("sllv") || op.equals("srav") || op.equals("srlv") || op.equals("and");
    }

    public static boolean isNumber(String str) {
        return str.matches("^-?[0-9]+$");
    }

    public static int getPower(String str) {
        if (isNumber(str)) {
            int n = Integer.parseInt(str);
            if (n > 0 && (n & (n - 1)) == 0) {
                return Integer.numberOfTrailingZeros(n);
            }
        }
        return -1;
    }

    public static boolean canSwap(String op) {
        return op.equals("addu") || op.equals("seq") || op.equals("mulu") || op.equals("sne") || op.equals("and");
    }

    public static String change(String op) {
        return switch (op) {
            case "sllv" -> "sll";
            case "srav" -> "sra";
            case "srlv" -> "srl";
            case "and" -> "andi";
            case "slt" -> "slti";
            case "mod" -> null;
            default -> op;
        };
    }
}