package org.zlab.ocov.tracker.type;

import org.zlab.ocov.tracker.ClassInfo;
import org.zlab.ocov.tracker.EqualitySet;
import org.zlab.ocov.tracker.IsSerialize;
import org.zlab.ocov.tracker.Runtime;

import java.util.Map;

public class StringType extends TypeInfo {
    private static final long serialVersionUID = 20231215L;

    int max = Integer.MIN_VALUE;
    int dumpIdMaxSize = -1;
    int min = Integer.MAX_VALUE;
    int dumpIdMinSize = -1;

    // Special values: null, -1, 0, 1
    boolean beenNullOnce = false;
    int dumpIdNullOnce = -1;

    boolean beenEmptyOnce = false;
    int dumpIdEmptyOnce = -1;

    boolean beenOneCharacterOnce = false;
    int dumpIdOneCharacterOnce = -1;

    boolean beenRestConditionOnce = false;
    int dumpIdRestConditionOnce = -1;

    boolean enableRangeCheck = false;

    public StringType(String itinerary) {
        super("String", itinerary);
    }

    @Override
    public void updateItinerary(String itineraryPrefix) {
        itinerary = itineraryPrefix + itinerary;
    }

    @Override
    public boolean update(Object value, Map<String, ClassInfo> baseClassInfo, int dumpId,
            EqualitySet equalitySet, IsSerialize isSerialize) {
        if (value == null) {
            if (!beenNullOnce) {
                beenNullOnce = true;
                dumpIdNullOnce = dumpId;
                return true;
            }
            return false;
        }
        if (value instanceof String) {
            String v = (String) value;
            int len = v.length();
            boolean changed = false;
            if (len == 0) {
                if (!beenEmptyOnce) {
                    beenEmptyOnce = true;
                    dumpIdEmptyOnce = dumpId;
                    changed = true;
                }
            } else if (len == 1) {
                if (!beenOneCharacterOnce) {
                    beenOneCharacterOnce = true;
                    dumpIdOneCharacterOnce = dumpId;
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
                if (len > max) {
                    max = v.length();
                    dumpIdMaxSize = dumpId;
                    changed = true;
                }
                if (len < min) {
                    min = v.length();
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
        if (other instanceof StringType) {
            StringType otherType = (StringType) other;
            boolean changed = false;
            if (otherType.beenNullOnce && !beenNullOnce) {
                beenNullOnce = true;
                dumpIdNullOnce = otherType.dumpIdNullOnce;
                log("itineraryNullOnce", itinerary, dumpIdNullOnce);
                changed = true;
            }
            if (otherType.beenEmptyOnce && !beenEmptyOnce) {
                beenEmptyOnce = true;
                dumpIdEmptyOnce = otherType.dumpIdEmptyOnce;
                log("itineraryEmptyOnce", itinerary, dumpIdEmptyOnce);
                changed = true;
            }
            if (otherType.beenOneCharacterOnce && !beenOneCharacterOnce) {
                beenOneCharacterOnce = true;
                dumpIdOneCharacterOnce = otherType.dumpIdOneCharacterOnce;
                log("itineraryOneCharacterOnce", itinerary, dumpIdOneCharacterOnce);
                changed = true;
            }
            if (otherType.beenRestConditionOnce && !beenRestConditionOnce) {
                beenRestConditionOnce = true;
                dumpIdRestConditionOnce = otherType.dumpIdRestConditionOnce;
                log("itineraryRestConditionOnce", itinerary, dumpIdRestConditionOnce);
                changed = true;
            }
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
