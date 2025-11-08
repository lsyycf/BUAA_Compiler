package backend.data;

import java.util.*;

public class FuncStack {
    private final String name;
    private final HashMap<String, Integer> localOffsets = new HashMap<>();
    private final HashMap<String, Integer> paramOffsets = new HashMap<>();
    private int stackSize = 0;

    public FuncStack(String name) {
        this.name = name;
    }

    public int getStackSize() {
        return stackSize;
    }

    public void setStackSize(int stackSize) {
        this.stackSize = stackSize;
    }

    public String getName() {
        return name;
    }

    public HashMap<String, Integer> getLocalOffsets() {
        return localOffsets;
    }

    public HashMap<String, Integer> getParamOffsets() {
        return paramOffsets;
    }

    public void putParam(String param, int offset) {
        paramOffsets.put(param, offset);
    }

    public void putLocal(String local, int offset) {
        localOffsets.put(local, offset);
    }

    public boolean containsParam(String param) {
        return paramOffsets.containsKey(param);
    }

    public boolean containsLocal(String local) {
        return localOffsets.containsKey(local);
    }
}
