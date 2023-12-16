package org.zlab.ocov.tracker.type;

import org.zlab.ocov.tracker.ClassInfo;

import java.util.Map;

public class CollectionType extends TypeInfo {
    private static final long serialVersionUID = 20231215L;

    int maxSize = Integer.MIN_VALUE;
    int minSize = Integer.MAX_VALUE;

    public CollectionType() {
        super("collection");
    }

    @Override
    public boolean update(Object value, Map<String, ClassInfo> baseClassInfo) {
        if (value == null) {
            return false;
        }
        if (value instanceof java.util.Collection) {
            int size = ((java.util.Collection) value).size();
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
        // Why would it not be a collection type?
        throw new RuntimeException(String.format("Not an %s but claimed to be", typeName));
    }

    @Override
    public boolean merge(TypeInfo otherTypeInfo) {
        if (otherTypeInfo instanceof CollectionType) {
            CollectionType otherCollectionType = (CollectionType) otherTypeInfo;
            boolean changed = false;
            if (otherCollectionType.maxSize > maxSize) {
                maxSize = otherCollectionType.maxSize;
                changed = true;
            }
            if (otherCollectionType.minSize < minSize) {
                minSize = otherCollectionType.minSize;
                changed = true;
            }
            return changed;
        }
        throw new RuntimeException(String.format("Not an %s but claimed to be", typeName));
    }

}
