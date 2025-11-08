package frontend.element;

import frontend.config.TokenType;

import java.util.ArrayList;

// LOrExp → LAndExp { <OR> LAndExp }
public class LOrExp {
    private final ArrayList<LAndExp> lAndExp = new ArrayList<>();

    public LOrExp(ArrayList<LAndExp> lAndExp) {
        this.lAndExp.addAll(lAndExp);
    }

    public ArrayList<LAndExp> getlAndExp() {
        return lAndExp;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lAndExp.size() - 1; i++) {
            sb.append(lAndExp.get(i)).append("\n");
            sb.append("<LOrExp>").append("\n");
            sb.append(TokenType.OR).append(" ||\n");
        }
        if (!lAndExp.isEmpty()) {
            sb.append(lAndExp.get(lAndExp.size() - 1)).append("\n");
        }
        sb.append("<LOrExp>");
        return sb.toString();
    }
}
