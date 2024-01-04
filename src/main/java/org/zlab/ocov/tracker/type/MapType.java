package org.zlab.ocov.tracker.type;

import org.apache.commons.lang3.SerializationUtils;
import org.zlab.ocov.tracker.ClassInfo;

import java.util.HashMap;
import java.util.Map;

public class MapType extends TypeInfo {

    private static final long serialVersionUID = 20231215L;

    int maxSize = Integer.MIN_VALUE;
    int minSize = Integer.MAX_VALUE;

    // FIXME: recursive record objects inside the collection
    boolean beenNullOnce = false;
    boolean beenZeroOnce = false;

    public Map<String, ClassInfo> keyClassNames = new HashMap<>();
    public Map<String, ClassInfo> valueClassNames = new HashMap<>();

    public MapType() {
        super("map");
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
        if (value instanceof java.util.Map) {
            int size = ((java.util.Map) value).size();
            boolean changed = false;
            // Recursively check the objects inside this collection
            // Do we track all objects inside it?
            // Track all for now
            for (Object object : ((java.util.Map) value).keySet()) {
                String className = object.getClass().getName();
                if (keyClassNames.containsKey(className)) {
                    if (keyClassNames.get(className).update(object, baseClassInfo))
                        changed = true;
                } else {
                    // Check whether this is a field that could be serialized
                    if (baseClassInfo.containsKey(className)) {
                        // Runtime.log("New class " + className);
                        ClassInfo newClassInfo = SerializationUtils
                                .clone(baseClassInfo.get(className));
                        newClassInfo.update(object, baseClassInfo);
                        keyClassNames.put(className, newClassInfo);
                        changed = true;
                    }
                }
            }
            for (Object object : ((java.util.Map) value).values()) {
                String className = object.getClass().getName();
                if (valueClassNames.containsKey(className)) {
                    if (valueClassNames.get(className).update(object, baseClassInfo))
                        changed = true;
                } else {
                    // Check whether this is a field that could be serialized
                    if (baseClassInfo.containsKey(className)) {
                        // Runtime.log("New class " + className);
                        ClassInfo newClassInfo = SerializationUtils
                                .clone(baseClassInfo.get(className));
                        newClassInfo.update(object, baseClassInfo);
                        valueClassNames.put(className, newClassInfo);
                        changed = true;
                    }
                }
            }
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
        throw new RuntimeException(String.format("Not an %s but claimed to be", typeName));
    }

    @Override
    public boolean merge(TypeInfo otherTypeInfo) {
        return false;
    }
}
