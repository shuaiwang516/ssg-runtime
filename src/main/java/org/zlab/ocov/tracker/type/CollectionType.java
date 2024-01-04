package org.zlab.ocov.tracker.type;

import org.apache.commons.lang3.SerializationUtils;
import org.zlab.ocov.tracker.ClassInfo;
import org.zlab.ocov.tracker.Runtime;

import java.util.HashMap;
import java.util.Map;

public class CollectionType extends TypeInfo {
    private static final long serialVersionUID = 20231215L;

    int maxSize = Integer.MIN_VALUE;
    int minSize = Integer.MAX_VALUE;

    // FIXME: recursive record objects inside the collection
    boolean beenNullOnce = false;
    boolean beenZeroOnce = false;

    public Map<String, ClassInfo> classNames = new HashMap<>();

    public CollectionType() {
        super("collection");
    }

    @Override
    public boolean update(Object value, Map<String, ClassInfo> baseClassInfo) {
        if (value == null) {
            if (!beenNullOnce) {
                beenNullOnce = true;
                return true;
            }
            return false;
        }
        if (value instanceof java.util.Collection) {
            int size = ((java.util.Collection) value).size();
            boolean changed = false;
            if (size == 0) {
                if (!beenZeroOnce) {
                    beenZeroOnce = true;
                    changed = true;
                }
            } else {
                // Recursively check the objects inside this collection
                // Do we track all objects inside it?
                // Track all for now
                for (Object object : (java.util.Collection) value) {
                    String className = object.getClass().getName();
                    if (classNames.containsKey(className)) {
                        if (classNames.get(className).update(object, baseClassInfo))
                            changed = true;
                    } else {
                        // Check whether this is a field that could be serialized
                        if (baseClassInfo.containsKey(className)) {
                            // Runtime.log("New class " + className);
                            ClassInfo newClassInfo = SerializationUtils
                                    .clone(baseClassInfo.get(className));
                            newClassInfo.update(object, baseClassInfo);
                            classNames.put(className, newClassInfo);
                            changed = true;
                        }
                    }
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
        // Why would it not be a collection type?
        throw new RuntimeException(String.format("Not an %s but claimed to be", typeName));
    }

    @Override
    public boolean merge(TypeInfo otherTypeInfo) {
        if (otherTypeInfo instanceof CollectionType) {
            CollectionType otherCollectionType = (CollectionType) otherTypeInfo;
            boolean changed = false;
            if (otherCollectionType.beenNullOnce) {
                if (!beenNullOnce) {
                    beenNullOnce = true;
                    changed = true;
                }
            }
            if (otherCollectionType.beenZeroOnce) {
                if (!beenZeroOnce) {
                    beenZeroOnce = true;
                    changed = true;
                }
            }
            if (otherCollectionType.maxSize > maxSize) {
                maxSize = otherCollectionType.maxSize;
                changed = true;
            }
            if (otherCollectionType.minSize < minSize) {
                minSize = otherCollectionType.minSize;
                changed = true;
            }
            if (changed)
                Runtime.log(String.format("[hklog] %s merge changed", typeName));
            return changed;
        }
        throw new RuntimeException(String.format("Not an %s but claimed to be", typeName));
    }

}
