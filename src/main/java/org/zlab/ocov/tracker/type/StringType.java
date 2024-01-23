package org.zlab.ocov.tracker.type;

import org.zlab.ocov.tracker.ClassInfo;
import org.zlab.ocov.tracker.EqualitySet;
import org.zlab.ocov.tracker.IsSerialize;
import org.zlab.ocov.tracker.Runtime;
import org.zlab.ocov.tracker.inv.unary.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class StringType extends TypeInfo {
    private static final long serialVersionUID = 20231215L;

    IntegerUpperBound upperBound = new IntegerUpperBound();
    IntegerLowerBound lowerBound = new IntegerLowerBound();

    NullOnce nullOnce = new NullOnce();
    ZeroOnce zeroOnce = new ZeroOnce();
    OneOnce oneOnce = new OneOnce();
    RestOnce restOnce;

    boolean enableRangeCheck = false;

    public StringType(String itinerary) {
        super("String", itinerary);
        Set<Number> targetValues = new HashSet<>();
        targetValues.add(0);
        targetValues.add(1);
        restOnce = new RestOnce(targetValues);
    }

    @Override
    public void updateItinerary(String itineraryPrefix) {
        itinerary = itineraryPrefix + itinerary;
    }

    @Override
    public boolean update(Object value, Map<String, ClassInfo> baseClassInfo, int dumpId,
            EqualitySet equalitySet, IsSerialize isSerialize) {
        if (value == null)
            return nullOnce.add(value, new LogInfo(dumpId));

        if (value instanceof String) {
            String v = (String) value;
            int len = v.length();
            boolean changed = false;
            LogInfo logInfo = new LogInfo(dumpId);
            if (zeroOnce.add(len, logInfo) || oneOnce.add(len, logInfo)
                    || restOnce.add(len, logInfo))
                changed = true;
            if (enableRangeCheck) {
                if (upperBound.add(len, logInfo) || lowerBound.add(len, logInfo))
                    changed = true;
            }
            return changed;
        }
        throw new RuntimeException(String.format("Not an %s but claimed to be", typeName));
    }

    @Override
    public boolean merge(TypeInfo other) {
        if (other instanceof StringType) {
            StringType otherType = (StringType) other;
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
                log("itineraryRestOnce", itinerary, restOnce.dumpId);
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
        throw new RuntimeException(String.format("Not an %s but claimed to be", typeName));
    }

}
