package org.zlab.ocov.tracker.type;

import org.zlab.ocov.tracker.ClassInfo;
import org.zlab.ocov.tracker.EqualitySet;
import org.zlab.ocov.tracker.IsSerialize;

import java.util.Map;

public abstract class ScalaType extends TypeInfo {

    int dumpIdMaxSize = -1;
    int dumpIdMinSize = -1;

    boolean beenNullOnce = false;
    int dumpIdNullOnce = -1;
    boolean beenMinusOneOnce = false;
    int dumpIdMinusOneOnce = -1;
    boolean beenZeroOnce = false;
    int dumpIdZeroOnce = -1;
    boolean beenOneOnce = false;
    int dumpIdOneOnce = -1;
    boolean beenRestConditionOnce = false;
    int dumpIdRestConditionOnce = -1;

    boolean enableRangeCheck = false;

    public ScalaType(String name, String itinerary) {
        super(name, itinerary);
    }

    @Override
    public void updateItinerary(String itineraryPrefix) {
        itinerary = itineraryPrefix + itinerary;
    }

    public boolean merge(ScalaType otherType) {
        boolean changed = false;
        if (otherType.beenNullOnce && !beenNullOnce) {
            beenNullOnce = true;
            dumpIdNullOnce = otherType.dumpIdNullOnce;
            log("beenNullOnce", itinerary, dumpIdNullOnce);
            changed = true;
        }
        if (otherType.beenMinusOneOnce && !beenMinusOneOnce) {
            beenMinusOneOnce = true;
            dumpIdMinusOneOnce = otherType.dumpIdMinusOneOnce;
            log("itineraryMinusOneOnce", itinerary, dumpIdMinusOneOnce);
            changed = true;
        }
        if (otherType.beenZeroOnce && !beenZeroOnce) {
            beenZeroOnce = true;
            dumpIdZeroOnce = otherType.dumpIdZeroOnce;
            log("itineraryZeroOnce", itinerary, dumpIdZeroOnce);
            changed = true;
        }
        if (otherType.beenOneOnce && !beenOneOnce) {
            beenOneOnce = true;
            dumpIdOneOnce = otherType.dumpIdOneOnce;
            log("itineraryOneOnce", itinerary, dumpIdOneOnce);
            changed = true;
        }
        if (otherType.beenRestConditionOnce && !beenRestConditionOnce) {
            beenRestConditionOnce = true;
            dumpIdRestConditionOnce = otherType.dumpIdRestConditionOnce;
            log("itineraryRestConditionOnce", itinerary, dumpIdRestConditionOnce);
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
