package org.zlab.ocov.tracker.type;

import org.zlab.ocov.tracker.ClassInfo;
import org.zlab.ocov.tracker.EqualitySet;

import java.util.Map;

public class BooleanType extends TypeInfo {
    private static final long serialVersionUID = 20231215L;

    boolean beenNullOnce = false;
    int dumpIdNullOnce = -1;

    boolean beenTrueOnce = false;
    int dumpIdTrueOnce = -1;

    boolean beenFalseOnce = false;
    int dumpIdFalseOnce = -1;

    // describe some characteristics
    public BooleanType(String itinerary) {
        super("Boolean", itinerary);
    }

    @Override
    public void updateItinerary(String itineraryPrefix) {
        itinerary = itineraryPrefix + itinerary;
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
        if (value instanceof Boolean) {
            boolean v = (Boolean) value;
            boolean changed = false;
            if (v) {
                if (!beenTrueOnce) {
                    beenTrueOnce = true;
                    dumpIdTrueOnce = dumpId;
                    changed = true;
                }
            } else {
                if (!beenFalseOnce) {
                    beenFalseOnce = true;
                    dumpIdFalseOnce = dumpId;
                    changed = true;
                }
            }
            return changed;
        }
        throw new RuntimeException(String.format("Not an %s but claimed to be", typeName));
    }

    @Override
    public boolean merge(TypeInfo otherTypeInfo) {
        if (otherTypeInfo instanceof BooleanType) {
            BooleanType otherBooleanType = (BooleanType) otherTypeInfo;
            boolean changed = false;
            if (otherBooleanType.beenNullOnce && !beenNullOnce) {
                beenNullOnce = true;
                dumpIdNullOnce = otherBooleanType.dumpIdNullOnce;
                log("itineraryNullOnce", itinerary, dumpIdNullOnce);
                changed = true;
            }
            if (otherBooleanType.beenTrueOnce && !beenTrueOnce) {
                beenTrueOnce = true;
                dumpIdTrueOnce = otherBooleanType.dumpIdTrueOnce;
                log("itineraryTrueOnce", itinerary, dumpIdTrueOnce);
                changed = true;
            }
            if (otherBooleanType.beenFalseOnce && !beenFalseOnce) {
                beenFalseOnce = true;
                dumpIdFalseOnce = otherBooleanType.dumpIdFalseOnce;
                log("itineraryFalseOnce", itinerary, dumpIdFalseOnce);
                changed = true;
            }
            return changed;
        }
        throw new RuntimeException(String.format("Not an %s but claimed to be", typeName));
    }

}
