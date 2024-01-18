package org.zlab.ocov.tracker.type;

import org.zlab.ocov.tracker.ClassInfo;
import org.zlab.ocov.tracker.EqualitySet;
import org.zlab.ocov.tracker.IsSerialize;
import org.zlab.ocov.tracker.Runtime;

import java.util.Map;

public class ArrayType extends SequenceType {
    private static final long serialVersionUID = 20231215L;

    public ArrayType(String itinerary) {
        super("array", itinerary);
    }

    @Override
    public void updateItinerary(String itineraryPrefix) {
        itinerary = itineraryPrefix + itinerary;
    }

    @Override
    public boolean update(Object value, Map<String, ClassInfo> baseClassInfo, int dumpId,
            EqualitySet equalitySet, IsSerialize isSerialized) {
        // value should be array type, update its size
        if (value == null) {
            if (!beenNullOnce) {
                beenNullOnce = true;
                dumpIdNullOnce = dumpId;
                return true;
            }
            return false;
        }
        int size = getArrayLength(value);
        boolean changed = false;
        if (updateSize(size, dumpId))
            changed = true;
        return changed;
    }

    @Override
    public boolean merge(TypeInfo other) {
        // Check whether it's null
        if (other instanceof ArrayType) {
            ArrayType otherType = (ArrayType) other;
            boolean changed = false;
            if (merge(otherType))
                changed = true;
            return changed;
        } else {
            throw new RuntimeException("Type not match");
        }
    }

    public static int getArrayLength(Object array) {
        // Use refection to get the length of the array
        if (array != null && array.getClass().isArray()) {
            return java.lang.reflect.Array.getLength(array);
        } else {
            throw new RuntimeException("Not an array but claimed to be");
        }
    }

}
