package org.zlab.ocov.tracker.type;

import org.zlab.ocov.tracker.ClassInfo;
import org.zlab.ocov.tracker.Runtime;

import java.util.Map;

public class IntegerType extends TypeInfo {
    private static final long serialVersionUID = 20231215L;

    int max = Integer.MIN_VALUE;
    int min = Integer.MAX_VALUE;

    // Special values: null, -1, 0, 1
    boolean beenNullOnce = false;
    boolean beenMinusOneOnce = false;
    boolean beenZeroOnce = false;
    boolean beenOneOnce = false;

    boolean enableRangeCheck = false;

    // describe some characteristics
    public IntegerType() {
        super("Integer");
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
        if (value instanceof Integer) {
            int v = (Integer) value;
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
        // Why would it not be integer?
        throw new RuntimeException(String.format("Not an %s but claimed to be", typeName));
    }

    @Override
    public boolean merge(TypeInfo otherTypeInfo) {
        if (otherTypeInfo instanceof IntegerType) {
            IntegerType otherIntegerType = (IntegerType) otherTypeInfo;
            boolean changed = false;
            if (otherIntegerType.beenNullOnce && !beenNullOnce) {
                beenNullOnce = true;
                changed = true;
            }
            if (otherIntegerType.beenMinusOneOnce && !beenMinusOneOnce) {
                beenMinusOneOnce = true;
                changed = true;
            }
            if (otherIntegerType.beenZeroOnce && !beenZeroOnce) {
                beenZeroOnce = true;
                changed = true;
            }
            if (otherIntegerType.beenOneOnce && !beenOneOnce) {
                beenOneOnce = true;
                changed = true;
            }
            if (enableRangeCheck) {
                if (otherIntegerType.max > max) {
                    max = otherIntegerType.max;
                    changed = true;
                }
                if (otherIntegerType.min < min) {
                    min = otherIntegerType.min;
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
