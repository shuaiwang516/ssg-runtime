package org.zlab.ocov.tracker.type;

import org.zlab.ocov.tracker.ClassInfo;

import java.util.Map;

public class LongType extends TypeInfo {
    long max = Long.MIN_VALUE;
    long min = Long.MAX_VALUE;

    // describe some characteristics
    public LongType() {
        super("Long");
    }

    @Override
    public boolean update(Object value, Map<String, ClassInfo> baseClassInfo) {
        if (value == null) {
            return false;
        }
        if (value instanceof Long) {
            long v = (Long) value;
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
        throw new RuntimeException(String.format("Not an %s but claimed to be", typeName));
    }

    @Override
    public boolean merge(TypeInfo otherTypeInfo) {
        if (otherTypeInfo instanceof LongType) {
            LongType otherLongType = (LongType) otherTypeInfo;
            boolean changed = false;
            if (otherLongType.max > max) {
                max = otherLongType.max;
                changed = true;
            }
            if (otherLongType.min < min) {
                min = otherLongType.min;
                changed = true;
            }
            return changed;
        }
        throw new RuntimeException(String.format("Not an %s but claimed to be", typeName));
    }

}
