package org.zlab.ocov.tracker.type;

import org.zlab.ocov.tracker.ClassInfo;

import java.util.Map;

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

    @Override
    public boolean merge(TypeInfo otherTypeInfo) {
        if (otherTypeInfo instanceof IntegerType) {
            IntegerType otherIntegerType = (IntegerType) otherTypeInfo;
            boolean changed = false;
            if (otherIntegerType.max > max) {
                max = otherIntegerType.max;
                changed = true;
            }
            if (otherIntegerType.min < min) {
                min = otherIntegerType.min;
                changed = true;
            }
            return changed;
        }
        throw new RuntimeException(String.format("Not an %s but claimed to be", typeName));
    }

}
