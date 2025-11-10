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
            default -> 0;
        };
    }

    public static boolean canCalculate(String op) {
        return op.equals("addu") || op.equals("subu") || op.equals("mulu") || op.equals("div") || op.equals("mod") || op.equals("slt") || op.equals("sgt") || op.equals("sle") || op.equals("sge") || op.equals("seq") || op.equals("sne");
    }

    public static boolean isNumber(String str) {
        return str.matches("^-?[0-9]+$");
    }
}
