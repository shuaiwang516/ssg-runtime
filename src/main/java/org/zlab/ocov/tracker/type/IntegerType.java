package org.zlab.ocov.tracker.type;

import org.zlab.ocov.tracker.ClassInfo;

import java.util.Map;
import java.util.Set;

public class IntegerType extends TypeInfo {
    int max = Integer.MIN_VALUE;
    int min = Integer.MAX_VALUE;

    // describe some characteristics
    public IntegerType() {
        super("Integer");
    }

    @Override
    public boolean update(Object value, Map<String, ClassInfo> baseClassInfo) {
        if (value == null) {
            return false;
        }
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
        throw new RuntimeException(String.format("Not an %s but claimed to be", typeName));
    }

}
