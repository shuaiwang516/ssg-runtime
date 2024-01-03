package org.zlab.ocov.tracker.type;

import org.zlab.ocov.tracker.ClassInfo;
import org.zlab.ocov.tracker.Runtime;

import java.util.Map;

public class DoubleType extends TypeInfo {
    private static final long serialVersionUID = 20231215L;

    double max = Double.MIN_VALUE;
    double min = Double.MAX_VALUE;

    boolean beenNullOnce = false;
    boolean beenMinusOneOnce = false;
    boolean beenZeroOnce = false;
    boolean beenOneOnce = false;

    boolean enableRangeCheck = false;

    // describe some characteristics
    public DoubleType() {
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
        if (value instanceof Double) {
            double v = (Double) value;
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
        if (otherTypeInfo instanceof DoubleType) {
            DoubleType otherDoubleType = (DoubleType) otherTypeInfo;
            boolean changed = false;
            if (otherDoubleType.beenNullOnce && !beenNullOnce) {
                beenNullOnce = true;
                changed = true;
            }
            if (otherDoubleType.beenMinusOneOnce && !beenMinusOneOnce) {
                beenMinusOneOnce = true;
                changed = true;
            }
            if (otherDoubleType.beenZeroOnce && !beenZeroOnce) {
                beenZeroOnce = true;
                changed = true;
            }
            if (otherDoubleType.beenOneOnce && !beenOneOnce) {
                beenOneOnce = true;
                changed = true;
            }
            if (enableRangeCheck) {
                if (otherDoubleType.max > max) {
                    max = otherDoubleType.max;
                    changed = true;
                }
                if (otherDoubleType.min < min) {
                    min = otherDoubleType.min;
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
