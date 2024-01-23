package org.zlab.ocov.tracker.type;

import org.zlab.ocov.tracker.ClassInfo;
import org.zlab.ocov.tracker.EqualitySet;
import org.zlab.ocov.tracker.IsSerialize;
import org.zlab.ocov.tracker.Runtime;
import org.zlab.ocov.tracker.inv.unary.IntegerUpperBound;
import org.zlab.ocov.tracker.inv.unary.LogInfo;
import org.zlab.ocov.tracker.inv.unary.LongLowerBound;
import org.zlab.ocov.tracker.inv.unary.LongUpperBound;

import java.util.Map;

public class LongType extends ScalaType {
    private static final long serialVersionUID = 20231215L;

    LongUpperBound upperBound = new LongUpperBound();
    LongLowerBound lowerBound = new LongLowerBound();

    public LongType(String itinerary) {
        super("Long", itinerary);
    }

    @Override
    public boolean update(Object value, Map<String, ClassInfo> baseClassInfo, int dumpId) {
        if (value == null)
            return nullOnce.add(value, new LogInfo(dumpId));

        if (value instanceof Long) {
            long v = (Long) value;
            boolean changed = false;
            LogInfo logInfo = new LogInfo(dumpId);
            if (negativeOneOnce.add(v, logInfo) || zeroOnce.add(v, logInfo)
                    || oneOnce.add(v, logInfo) || restOnce.add(v, logInfo)) {
                changed = true;
            }
            if (enableRangeCheck) {
                if (upperBound.add(v, logInfo) || lowerBound.add(v, logInfo))
                    changed = true;
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
                if (upperBound.merge(otherType.upperBound)) {
                    log("itineraryUpperBound", itinerary, upperBound.dumpId);
                    changed = true;
                }
                if (lowerBound.merge(otherType.lowerBound)) {
                    log("itineraryLowerBound", itinerary, lowerBound.dumpId);
                    changed = true;
                }
            }
            return changed;
        }
        throw new RuntimeException(String.format("Not an %s but claimed to be", typeName));
    }

}
