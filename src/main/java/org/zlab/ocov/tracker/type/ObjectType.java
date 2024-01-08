package org.zlab.ocov.tracker.type;

import org.apache.commons.lang3.SerializationUtils;
import org.zlab.ocov.tracker.ClassInfo;
import org.zlab.ocov.tracker.Runtime;

import java.util.HashMap;
import java.util.Map;

public class ObjectType extends TypeInfo {
    private static final long serialVersionUID = 20231215L;

    // Could be Object type, which might be any type
    // Or it could be a specific class type, the class name might change
    boolean beenNullOnce = false;
    int dumpIdNullOnce = -1;

    public Map<String, ClassInfo> classNames = new HashMap<>();
    public Map<String, Integer> classNamesDumpId = new HashMap<>();

    public ObjectType(String itinerary) {
        super("object", itinerary);
    }

    @Override
    public void updateItinerary(String itineraryPrefix) {
        itinerary = itineraryPrefix + itineraryPrefix;
        // Update itinerary for all classInfo
        for (String className : classNames.keySet()) {
            classNames.get(className).updateItinerary(itineraryPrefix);
        }
    }

    @Override
    public boolean update(Object value, Map<String, ClassInfo> baseClassInfo, int dumpId) {
        if (value == null) {
            if (!beenNullOnce) {
                beenNullOnce = true;
                dumpIdNullOnce = dumpId;
                return true;
            }
            return false;
        }
        boolean changed = false;
        String className = value.getClass().getName();
        if (classNames.containsKey(className)) {
            if (classNames.get(className).update(value, baseClassInfo, dumpId))
                changed = true;
        } else {
            // Check whether this is a field that could be serialized
            if (baseClassInfo.containsKey(className)) {
                // Runtime.log("New class " + className);
                ClassInfo newClassInfo = SerializationUtils.clone(baseClassInfo.get(className));
                newClassInfo.updateItinerary(itinerary);
                newClassInfo.update(value, baseClassInfo, dumpId);
                classNames.put(className, newClassInfo);
                classNamesDumpId.put(className, dumpId);
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public boolean merge(TypeInfo otherTypeInfo) {
        if (otherTypeInfo instanceof ObjectType) {
            ObjectType otherObjectType = (ObjectType) otherTypeInfo;
            boolean changed = false;
            if (otherObjectType.beenNullOnce && !beenNullOnce) {
                beenNullOnce = true;
                dumpIdNullOnce = otherObjectType.dumpIdNullOnce;
                log("itineraryNullOnce", itinerary, dumpIdNullOnce);
                changed = true;
            }
            for (Map.Entry<String, ClassInfo> entry : otherObjectType.classNames.entrySet()) {
                String className = entry.getKey();
                ClassInfo otherClassInfo = entry.getValue();
                if (classNames.containsKey(className)) {
                    ClassInfo classInfo = classNames.get(className);
                    if (classInfo.merge(otherClassInfo))
                        changed = true;
                } else {
                    ClassInfo newClassInfo = SerializationUtils.clone(otherClassInfo);
                    classNames.put(className, newClassInfo);
                    classNamesDumpId.put(className,
                            otherObjectType.classNamesDumpId.get(className));
                    log("new class: " + className, itinerary,
                            otherObjectType.classNamesDumpId.get(className));
                    changed = true;
                }
            }
            return changed;
        }
        throw new RuntimeException(String.format("Not an %s but claimed to be", typeName));
    }

}
