package org.zlab.ocov.tracker.type;

import org.zlab.ocov.tracker.ClassInfo;

import java.util.Map;
import java.util.Set;

public class ArrayType extends TypeInfo {
    int maxSize = Integer.MIN_VALUE;
    int minSize = Integer.MAX_VALUE;

    public ArrayType() {
        super("array");
    }

    @Override
    public boolean update(Object value, Set<String> visitedClasses,
            Map<String, ClassInfo> baseClassInfo) {
        // value should be array type, update its size

        // TODO: Handle null situation, it should be a special type
        if (value == null)
            return false;

        int size = getArrayLength(value);
        boolean changed = false;
        if (size > maxSize) {
            maxSize = size;
            changed = true;
        }
        if (size < minSize) {
            minSize = size;
            changed = true;
        }
        return changed;
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
