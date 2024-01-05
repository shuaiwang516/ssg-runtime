package org.zlab.ocov.tracker.type;

public abstract class SequenceType extends TypeInfo {

    int maxSize = Integer.MIN_VALUE;
    int dumpIdMaxSize = -1;

    int minSize = Integer.MAX_VALUE;
    int dumpIdMinSize = -1;

    // FIXME: recursive record objects inside the collection
    boolean beenNullOnce = false;
    int dumpIdNullOnce = -1;

    boolean beenZeroOnce = false;
    int dumpIdZeroOnce = -1;

    boolean beenOneOnce = false;
    int dumpIdOneOnce = -1;

    boolean beenRestConditionOnce = false;
    int dumpIdRestConditionOnce = -1;

    boolean enableRangeCheck = true;

    public SequenceType(String name, String itinerary) {
        super(name, itinerary);
    }

    boolean updateSize(int size, int dumpId) {
        boolean changed = false;
        if (size == 0) {
            if (!beenZeroOnce) {
                beenZeroOnce = true;
                dumpIdZeroOnce = dumpId;
                changed = true;
            }
        } else if (size == 1) {
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
            if (size > maxSize) {
                maxSize = size;
                dumpIdMaxSize = dumpId;
                changed = true;
            }
            if (size < minSize) {
                minSize = size;
                dumpIdMinSize = dumpId;
                changed = true;
            }
        }
        return changed;
    }

    public boolean merge(SequenceType otherType) {
        boolean changed = false;
        if (otherType.beenNullOnce && !beenNullOnce) {
            beenNullOnce = true;
            dumpIdNullOnce = otherType.dumpIdNullOnce;
            log("itineraryNullOnce", itinerary, dumpIdNullOnce);
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
        if (enableRangeCheck) {
            if (otherType.maxSize > maxSize) {
                maxSize = otherType.maxSize;
                dumpIdMaxSize = otherType.dumpIdMaxSize;
                log("itineraryMaxSize", itinerary, dumpIdMaxSize);
                changed = true;
            }
            if (otherType.minSize < minSize) {
                minSize = otherType.minSize;
                dumpIdMinSize = otherType.dumpIdMinSize;
                log("itineraryMinSize", itinerary, dumpIdMinSize);
                changed = true;
            }
        }
        return changed;
    }

}
