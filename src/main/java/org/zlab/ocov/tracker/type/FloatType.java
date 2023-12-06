package org.zlab.ocov.tracker.type;

import org.zlab.ocov.tracker.ClassInfo;

import java.util.Map;
import java.util.Set;

public class FloatType extends TypeInfo {
    float max = Float.MIN_VALUE;
    float min = Float.MAX_VALUE;

    // describe some characteristics
    public FloatType() {
        super("Float");
    }

    @Override
    public boolean update(Object value, Set<String> visitedClasses,
            Map<String, ClassInfo> baseClassInfo) {
        if (value == null) {
            return false;
        }
        if (value instanceof Float) {
            float v = (Float) value;
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

}
