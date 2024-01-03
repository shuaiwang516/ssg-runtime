package org.zlab.ocov.tracker.type;

import org.zlab.ocov.tracker.ClassInfo;
import org.zlab.ocov.tracker.Runtime;

import java.util.Map;

public class LongType extends TypeInfo {
    private static final long serialVersionUID = 20231215L;

    long max = Long.MIN_VALUE;
    long min = Long.MAX_VALUE;

    // Special values: null, -1, 0, 1
    boolean beenNullOnce = false;
    boolean beenMinusOneOnce = false;
    boolean beenZeroOnce = false;
    boolean beenOneOnce = false;

    boolean enableRangeCheck = false;

    // describe some characteristics
    public LongType() {
        super("Long");
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
        if (value instanceof Long) {
            long v = (Long) value;
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
        if (otherTypeInfo instanceof LongType) {
            LongType otherLongType = (LongType) otherTypeInfo;
            boolean changed = false;
            if (otherLongType.beenNullOnce && !beenNullOnce) {
                beenNullOnce = true;
                changed = true;
            }
            if (otherLongType.beenMinusOneOnce && !beenMinusOneOnce) {
                beenMinusOneOnce = true;
                changed = true;
            }
            if (otherLongType.beenZeroOnce && !beenZeroOnce) {
                beenZeroOnce = true;
                changed = true;
            }
            if (otherLongType.beenOneOnce && !beenOneOnce) {
                beenOneOnce = true;
                changed = true;
            }
            if (enableRangeCheck) {
                if (otherLongType.max > max) {
                    max = otherLongType.max;
                    changed = true;
                }
                if (otherLongType.min < min) {
                    min = otherLongType.min;
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
