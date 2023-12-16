package org.zlab.ocov.tracker.type;

import org.zlab.ocov.tracker.ClassInfo;

import java.util.Map;

public class FloatType extends TypeInfo {
    private static final long serialVersionUID = 20231215L;

    float max = Float.MIN_VALUE;
    float min = Float.MAX_VALUE;

    // describe some characteristics
    public FloatType() {
        super("Float");
    }

    @Override
    public boolean update(Object value, Map<String, ClassInfo> baseClassInfo) {
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

    @Override
    public boolean merge(TypeInfo otherTypeInfo) {
        if (otherTypeInfo instanceof FloatType) {
            FloatType otherFloatType = (FloatType) otherTypeInfo;
            boolean changed = false;
            if (otherFloatType.max > max) {
                max = otherFloatType.max;
                changed = true;
            }
            if (otherFloatType.min < min) {
                min = otherFloatType.min;
                changed = true;
            }
            return changed;
        }
        throw new RuntimeException(String.format("Not an %s but claimed to be", typeName));
    }

}
