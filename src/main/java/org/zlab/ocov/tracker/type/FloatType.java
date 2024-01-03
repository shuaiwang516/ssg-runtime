package org.zlab.ocov.tracker.type;

import org.zlab.ocov.tracker.ClassInfo;
import org.zlab.ocov.tracker.Runtime;

import java.util.Map;

public class FloatType extends TypeInfo {
    private static final long serialVersionUID = 20231215L;

    float max = Float.MIN_VALUE;
    float min = Float.MAX_VALUE;

    boolean beenNullOnce = false;
    boolean beenMinusOneOnce = false;
    boolean beenZeroOnce = false;
    boolean beenOneOnce = false;

    boolean enableRangeCheck = false;

    // describe some characteristics
    public FloatType() {
        super("Float");
    }

    @Override
    public boolean update(Object value, Map<String, ClassInfo> baseClassInfo) {
        if (value == null) {
            if (!beenNullOnce) {
                beenNullOnce = true;
                return true;
            }
            return false;
        }
        if (value instanceof Float) {
            float v = (Float) value;
            boolean changed = false;
            if (v == -1) {
                if (!beenMinusOneOnce) {
                    beenMinusOneOnce = true;
                    changed = true;
                }
            }
            if (v == 0) {
                if (!beenZeroOnce) {
                    beenZeroOnce = true;
                    changed = true;
                }
            }
            if (v == 1) {
                if (!beenOneOnce) {
                    beenOneOnce = true;
                    changed = true;
                }
            }
            if (enableRangeCheck) {
                if (v > max) {
                    max = v;
                    changed = true;
                }
                if (v < min) {
                    min = v;
                    changed = true;
                }
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
            if (otherFloatType.beenNullOnce && !beenNullOnce) {
                beenNullOnce = true;
                changed = true;
            }
            if (otherFloatType.beenMinusOneOnce && !beenMinusOneOnce) {
                beenMinusOneOnce = true;
                changed = true;
            }
            if (otherFloatType.beenZeroOnce && !beenZeroOnce) {
                beenZeroOnce = true;
                changed = true;
            }
            if (otherFloatType.beenOneOnce && !beenOneOnce) {
                beenOneOnce = true;
                changed = true;
            }
            if (enableRangeCheck) {
                if (otherFloatType.max > max) {
                    max = otherFloatType.max;
                    changed = true;
                }
                if (otherFloatType.min < min) {
                    min = otherFloatType.min;
                    changed = true;
                }
            }
            if (changed)
                Runtime.log(String.format("[hklog] %s merge changed", typeName));
            return changed;
        }
        throw new RuntimeException(String.format("Not an %s but claimed to be", typeName));
    }

}
