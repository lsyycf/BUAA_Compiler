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
            default -> 0;
        };
    }

    public static boolean canCalculate(String op) {
        return op.equals("addu") || op.equals("subu") || op.equals("mulu") || op.equals("div") || op.equals("mod") || op.equals("slt") || op.equals("sgt") || op.equals("sle") || op.equals("sge") || op.equals("seq") || op.equals("sne") || op.equals("sllv");
    }

    public static boolean isNumber(String str) {
        return str.matches("^-?[0-9]+$");
    }

    public static int getPower(String str) {
        if (isNumber(str)) {
            return switch (Integer.parseInt(str)) {
                case 2 -> 1;
                case 4 -> 2;
                case 8 -> 3;
                case 16 -> 4;
                case 32 -> 5;
                case 64 -> 6;
                case 128 -> 7;
                case 256 -> 8;
                case 512 -> 9;
                case 1024 -> 10;
                case 2048 -> 11;
                case 4096 -> 12;
                case 8192 -> 13;
                case 16384 -> 14;
                case 32768 -> 15;
                case 65536 -> 16;
                case 131072 -> 17;
                case 262144 -> 18;
                case 524288 -> 19;
                case 1048576 -> 20;
                case 2097152 -> 21;
                case 4194304 -> 22;
                case 8388608 -> 23;
                case 16777216 -> 24;
                case 33554432 -> 25;
                case 67108864 -> 26;
                case 134217728 -> 27;
                case 268435456 -> 28;
                case 536870912 -> 29;
                case 1073741824 -> 30;
                default -> -1;
            };
        }
        return -1;
    }

    public static String change(String op) {
        return switch (op) {
            case "sllv" -> "sll";
            case "slt" -> "slti";
            case "mod" -> null;
            default -> op;
        };
    }

    public static boolean canSwap(String op) {
        return op.equals("addu") || op.equals("seq") || op.equals("mulu") || op.equals("sne");
    }
}
