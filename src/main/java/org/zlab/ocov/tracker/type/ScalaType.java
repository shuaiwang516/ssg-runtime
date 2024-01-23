package org.zlab.ocov.tracker.type;

import org.zlab.ocov.tracker.ClassInfo;
import org.zlab.ocov.tracker.EqualitySet;
import org.zlab.ocov.tracker.IsSerialize;
import org.zlab.ocov.tracker.inv.unary.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class ScalaType extends TypeInfo {

    NullOnce nullOnce = new NullOnce();
    NegativeOneOnce negativeOneOnce = new NegativeOneOnce();
    ZeroOnce zeroOnce = new ZeroOnce();
    OneOnce oneOnce = new OneOnce();
    RestOnce restOnce;

    boolean enableRangeCheck = false;

    public ScalaType(String name, String itinerary) {
        super(name, itinerary);
        Set<Number> targetValues = new HashSet<>();
        targetValues.add(-1);
        targetValues.add(0);
        targetValues.add(1);
        restOnce = new RestOnce(targetValues);
    }

    @Override
    public void updateItinerary(String itineraryPrefix) {
        itinerary = itineraryPrefix + itinerary;
    }

    public boolean merge(ScalaType otherType) {
        boolean changed = false;
        if (nullOnce.merge(otherType.nullOnce)) {
            log("itineraryNullOnce", itinerary, nullOnce.dumpId);
            changed = true;
        }
        if (negativeOneOnce.merge(otherType.negativeOneOnce)) {
            log("itineraryNegativeOneOnce", itinerary, negativeOneOnce.dumpId);
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
        return changed;
    }

    public abstract boolean update(Object value, Map<String, ClassInfo> baseClassInfo, int dumpId);

    @Override
    public boolean update(Object value, Map<String, ClassInfo> baseClassInfo, int dumpId,
            EqualitySet equalitySet, IsSerialize isSerialized) {
        return update(value, baseClassInfo, dumpId);
    }

}
