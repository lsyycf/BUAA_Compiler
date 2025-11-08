package frontend.lexer;

import java.util.*;
import java.util.regex.*;

public class Reader {
    private final ArrayList<String> lines;
    private int lineIndex = 0;
    private int columnIndex = 0;

    public Reader(ArrayList<String> lines) {
        this.lines = lines;
    }

    public boolean endOfFile() {
        return this.lineIndex >= this.lines.size();
    }

    public boolean endOfLine() {
        if (endOfFile()) {
            return true;
        }
        return this.columnIndex >= this.lines.get(lineIndex).length();
    }

    public String peekLine() {
        if (endOfFile()) {
            return "";
        } else {
            return this.lines.get(lineIndex);
        }
    }

    public char peekChar() {
        if (endOfLine()) {
            return '\n';
        } else if (endOfFile()) {
            return 0;
        } else {
            return peekLine().charAt(columnIndex);
        }
    }

    public String peekStr(int len) {
        if (endOfFile()) {
            return "";
        } else if (this.columnIndex + len >= peekLine().length()) {
            return peekLine().substring(this.columnIndex);
        } else {
            return peekLine().substring(this.columnIndex, this.columnIndex + len);
        }
    }

    public void skip() {
        while (!endOfFile() && Character.isWhitespace(peekChar())) {
            consume(1);
        }
    }

    public void consume(int steps) {
        int count = steps;
        while (!endOfFile() && count > 0) {
            int len = peekLine().length();
            if (columnIndex + count >= len) {
                lineIndex++;
                count -= (len - columnIndex + 1);
                columnIndex = 0;
            } else {
                columnIndex += count;
                count = 0;
            }
        }
    }

    public void nextLine() {
        if (!endOfFile()) {
            lineIndex++;
            columnIndex = 0;
        }
    }

    public String getRemain() {
        if (endOfFile() || endOfLine()) {
            return "";
        } else {
            return peekLine().substring(columnIndex);
        }
    }

    public String matchStr(Pattern pattern) {
        String remain = getRemain();
        Matcher matcher = pattern.matcher(remain);
        if (matcher.find()) {
            return matcher.group(0);
        } else {
            return null;
        }
    }

    public int getLineIndex() {
        return lineIndex + 1;
    }
}
