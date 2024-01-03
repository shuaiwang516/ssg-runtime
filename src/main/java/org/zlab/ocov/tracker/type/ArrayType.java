package org.zlab.ocov.tracker.type;

import org.zlab.ocov.tracker.ClassInfo;

import java.util.Map;

public class ArrayType extends TypeInfo {
    private static final long serialVersionUID = 20231215L;

    int maxSize = Integer.MIN_VALUE;
    int minSize = Integer.MAX_VALUE;

    boolean beenNullOnce = false;
    boolean beenZeroOnce = false;

    public ArrayType() {
        super("array");
    }

    @Override
    public boolean update(Object value, Map<String, ClassInfo> baseClassInfo) {
        // value should be array type, update its size
        if (value == null) {
            if (!beenNullOnce) {
                beenNullOnce = true;
                return true;
            }
            return false;
        }
        int size = getArrayLength(value);
        boolean changed = false;
        if (size == 0) {
            if (!beenZeroOnce) {
                beenZeroOnce = true;
                changed = true;
            }
        }
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

    @Override
    public boolean merge(TypeInfo otherTypeInfo) {
        // Check whether it's null
        if (otherTypeInfo instanceof ArrayType) {
            ArrayType otherArrayType = (ArrayType) otherTypeInfo;
            boolean changed = false;
            if (otherArrayType.beenNullOnce) {
                if (!beenNullOnce) {
                    beenNullOnce = true;
                    changed = true;
                }
            }
            if (otherArrayType.beenZeroOnce) {
                if (!beenZeroOnce) {
                    beenZeroOnce = true;
                    changed = true;
                }
            }
            if (otherArrayType.maxSize > maxSize) {
                maxSize = otherArrayType.maxSize;
                changed = true;
            }
            if (otherArrayType.minSize < minSize) {
                minSize = otherArrayType.minSize;
                changed = true;
            }
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
