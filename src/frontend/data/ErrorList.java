package frontend.data;

import frontend.config.ErrorType;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

public class ErrorList {
    private final TreeMap<Integer, TreeSet<ErrorType>> errorMap = new TreeMap<>();
    private boolean isFatal = false;

    public void addError(Integer key, ErrorType errorType) {
        errorMap.computeIfAbsent(key, k -> new TreeSet<>(Comparator.comparing(ErrorType::toString))).add(errorType);
        if (errorType.isFatal()) {
            isFatal = true;
        }
    }

    public void addAllError(ErrorList errorList) {
        for (Map.Entry<Integer, TreeSet<ErrorType>> entry : errorList.errorMap.entrySet()) {
            Integer key = entry.getKey();
            TreeSet<ErrorType> errors = entry.getValue();
            for (ErrorType error : errors) {
                this.addError(key, error);
            }
        }
    }

    public boolean isFatal() {
        return isFatal;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        for (Map.Entry<Integer, TreeSet<ErrorType>> entry : errorMap.entrySet()) {
            if (!isFirst) {
                sb.append("\n");
            } else {
                isFirst = false;
            }
            Integer key = entry.getKey();
            TreeSet<ErrorType> errors = entry.getValue();
            sb.append(key).append(" ");
            for (ErrorType error : errors) {
                sb.append(error.toString());
            }
        }
        return sb.toString();
    }
}
