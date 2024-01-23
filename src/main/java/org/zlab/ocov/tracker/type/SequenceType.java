package org.zlab.ocov.tracker.type;

import org.zlab.ocov.tracker.inv.unary.*;

import java.util.HashSet;
import java.util.Set;

public abstract class SequenceType extends TypeInfo {

    NullOnce nullOnce = new NullOnce();
    ZeroOnce zeroOnce = new ZeroOnce();
    OneOnce oneOnce = new OneOnce();

    RestOnce restOnce;

    boolean enableRangeCheck = false;
    IntegerLowerBound lowerBound = new IntegerLowerBound();
    IntegerUpperBound upperBound = new IntegerUpperBound();

    public SequenceType(String name, String itinerary) {
        super(name, itinerary);
        Set<Number> targetValues = new HashSet<>();
        targetValues.add(0);
        targetValues.add(1);
        restOnce = new RestOnce(targetValues);
    }

    boolean updateSize(int size, int dumpId) {
        LogInfo logInfo = new LogInfo(dumpId);
        boolean changed = false;

        if (zeroOnce.add(size, logInfo) || oneOnce.add(size, logInfo)
                || restOnce.add(size, logInfo)) {
            changed = true;
        }
        if (enableRangeCheck) {
            if (upperBound.add(size, logInfo) || lowerBound.add(size, logInfo))
                changed = true;
        }
        return changed;
    }

    public boolean merge(SequenceType otherType) {
        boolean changed = false;
        if (nullOnce.merge(otherType.nullOnce)) {
            log("itineraryNullOnce", itinerary, nullOnce.dumpId);
            changed = true;
        }

        if (zeroOnce.merge(otherType.zeroOnce)) {
            log("itineraryZeroOnce", itinerary, zeroOnce.dumpId);
            changed = true;
        }
        if (oneOnce.merge(otherType.oneOnce)) {
            log("itineraryOneOnce", itinerary, oneOnce.dumpId);
            changed = true;
        }
        if (restOnce.merge(otherType.restOnce)) {
            log("itineraryRestConditionOnce", itinerary, restOnce.dumpId);
            changed = true;
        }
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

}
