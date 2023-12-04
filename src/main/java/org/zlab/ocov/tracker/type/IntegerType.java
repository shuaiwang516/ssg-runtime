package org.zlab.ocov.tracker.type;

public class IntegerType extends TypeInfo {
    int max = Integer.MIN_VALUE;
    int min = Integer.MAX_VALUE;

    // describe some characteristics
    public IntegerType() {
        super("Integer");
    }

    @Override
    public boolean update(Object value) {
        if (value instanceof Integer) {
            int v = (Integer) value;
            boolean changed = false;
            if (v > max) {
                max = v;
                changed = true;
            }
            if (v < min) {
                min = v;
                changed = true;
            }
            return changed;
        }
        // Why would it not be integer?
        throw new RuntimeException("Not an integer but claimed to be integer");
    }

}
