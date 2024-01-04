package org.zlab.ocov.tracker.type;

import org.zlab.ocov.tracker.ClassInfo;
import org.zlab.ocov.tracker.Runtime;

import java.util.Map;

public class ShortType extends TypeInfo {
    private static final long serialVersionUID = 20231215L;

    short max = Short.MIN_VALUE;
    short min = Short.MAX_VALUE;

    // Special values: null, -1, 0, 1
    boolean beenNullOnce = false;
    boolean beenMinusOneOnce = false;
    boolean beenZeroOnce = false;
    boolean beenOneOnce = false;
    boolean beenRestConditionOnce = false;

    boolean enableRangeCheck = false;

    // describe some characteristics
    public ShortType() {
        super("Short");
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
        if (value instanceof Short) {
            short v = (Short) value;
            boolean changed = false;
            if (v == -1) {
                if (!beenMinusOneOnce) {
                    beenMinusOneOnce = true;
                    changed = true;
                }
            } else if (v == 0) {
                if (!beenZeroOnce) {
                    beenZeroOnce = true;
                    changed = true;
                }
            } else if (v == 1) {
                if (!beenOneOnce) {
                    beenOneOnce = true;
                    changed = true;
                }
            } else {
                if (!beenRestConditionOnce) {
                    beenRestConditionOnce = true;
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
        if (otherTypeInfo instanceof ShortType) {
            ShortType otherShortType = (ShortType) otherTypeInfo;
            boolean changed = false;
            if (otherShortType.beenNullOnce && !beenNullOnce) {
                beenNullOnce = true;
                changed = true;
            }
            if (otherShortType.beenMinusOneOnce && !beenMinusOneOnce) {
                beenMinusOneOnce = true;
                changed = true;
            }
            if (otherShortType.beenZeroOnce && !beenZeroOnce) {
                beenZeroOnce = true;
                changed = true;
            }
            if (otherShortType.beenOneOnce && !beenOneOnce) {
                beenOneOnce = true;
                changed = true;
            }
            if (otherShortType.beenRestConditionOnce && !beenRestConditionOnce) {
                beenRestConditionOnce = true;
                changed = true;
            }
            if (enableRangeCheck) {
                if (otherShortType.max > max) {
                    max = otherShortType.max;
                    changed = true;
                }
                if (otherShortType.min < min) {
                    min = otherShortType.min;
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
