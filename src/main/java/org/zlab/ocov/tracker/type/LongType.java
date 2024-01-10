package org.zlab.ocov.tracker.type;

import org.zlab.ocov.tracker.ClassInfo;
import org.zlab.ocov.tracker.EqualitySet;
import org.zlab.ocov.tracker.Runtime;

import java.util.Map;

public class LongType extends ScalaType {
    private static final long serialVersionUID = 20231215L;

    long max = Long.MIN_VALUE;
    long min = Long.MAX_VALUE;

    public LongType(String itinerary) {
        super("Long", itinerary);
    }

    @Override
    public boolean update(Object value, Map<String, ClassInfo> baseClassInfo, int dumpId,
            EqualitySet equalitySet) {
        if (value == null) {
            if (!beenNullOnce) {
                beenNullOnce = true;
                dumpIdNullOnce = dumpId;
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
                    dumpIdMinusOneOnce = dumpId;
                    changed = true;
                }
            } else if (v == 0) {
                if (!beenZeroOnce) {
                    beenZeroOnce = true;
                    dumpIdZeroOnce = dumpId;
                    changed = true;
                }
            } else if (v == 1) {
                if (!beenOneOnce) {
                    beenOneOnce = true;
                    dumpIdOneOnce = dumpId;
                    changed = true;
                }
            } else {
                if (!beenRestConditionOnce) {
                    beenRestConditionOnce = true;
                    dumpIdRestConditionOnce = dumpId;
                    changed = true;
                }
            }

            if (enableRangeCheck) {
                if (v > max) {
                    max = v;
                    dumpIdMaxSize = dumpId;
                    changed = true;
                }
                if (v < min) {
                    min = v;
                    dumpIdMinSize = dumpId;
                    changed = true;
                }
            }
            return changed;
        }
        throw new RuntimeException(String.format("Not an %s but claimed to be", typeName));
    }

    @Override
    public boolean merge(TypeInfo other) {
        if (other instanceof LongType) {
            LongType otherType = (LongType) other;
            boolean changed = false;
            if (merge(otherType))
                changed = true;
            if (enableRangeCheck) {
                if (otherType.max > max) {
                    max = otherType.max;
                    dumpIdMaxSize = otherType.dumpIdMaxSize;
                    log("itineraryMaxSize", itinerary, dumpIdMaxSize);
                    changed = true;
                }
                if (otherType.min < min) {
                    min = otherType.min;
                    dumpIdMinSize = otherType.dumpIdMinSize;
                    log("itineraryMinSize", itinerary, dumpIdMinSize);
                    changed = true;
                }
            }
            return changed;
        }
        throw new RuntimeException(String.format("Not an %s but claimed to be", typeName));
    }

}
