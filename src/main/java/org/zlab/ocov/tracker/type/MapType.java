package org.zlab.ocov.tracker.type;

import org.apache.commons.lang3.SerializationUtils;
import org.zlab.ocov.tracker.ClassInfo;

import java.util.HashMap;
import java.util.Map;

public class MapType extends SequenceType {

    private static final long serialVersionUID = 20231215L;

    public Map<String, ClassInfo> keyClassNames = new HashMap<>();
    public Map<String, ClassInfo> valueClassNames = new HashMap<>();
    int dumpId = -1;

    public MapType(String itinerary) {
        super("map", itinerary);
    }

    @Override
    public void updateItinerary(String itineraryPrefix) {
        itinerary = itineraryPrefix + itinerary;
        // Update itinerary for all classInfo
        for (String className : keyClassNames.keySet()) {
            keyClassNames.get(className).updateItinerary(itineraryPrefix);
        }
        for (String className : valueClassNames.keySet()) {
            valueClassNames.get(className).updateItinerary(itineraryPrefix);
        }
    }

    @Override
    public boolean update(Object value, Map<String, ClassInfo> baseClassInfo, int dumpId) {
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
                    if (keyClassNames.get(className).update(object, baseClassInfo, dumpId))
                        changed = true;
                } else {
                    // Check whether this is a field that could be serialized
                    if (baseClassInfo.containsKey(className)) {
                        // Runtime.log("New class " + className);
                        ClassInfo newClassInfo = SerializationUtils
                                .clone(baseClassInfo.get(className));
                        newClassInfo.updateItinerary(itinerary + ".map_keyItem");
                        newClassInfo.update(object, baseClassInfo, dumpId);
                        keyClassNames.put(className, newClassInfo);
                        changed = true;
                    }
                }
            }
            for (Object object : ((java.util.Map) value).values()) {
                String className = object.getClass().getName();
                if (valueClassNames.containsKey(className)) {
                    if (valueClassNames.get(className).update(object, baseClassInfo, dumpId))
                        changed = true;
                } else {
                    // Check whether this is a field that could be serialized
                    if (baseClassInfo.containsKey(className)) {
                        // Runtime.log("New class " + className);
                        ClassInfo newClassInfo = SerializationUtils
                                .clone(baseClassInfo.get(className));
                        newClassInfo.updateItinerary(itinerary + ".map_valueItem");
                        newClassInfo.update(object, baseClassInfo, dumpId);
                        valueClassNames.put(className, newClassInfo);
                        changed = true;
                    }
                }
            }
            if (updateSize(size, dumpId))
                changed = true;
            return changed;
        }
        throw new RuntimeException(String.format("Not an %s but claimed to be", typeName));
    }

    @Override
    public boolean merge(TypeInfo other) {
        if (other instanceof MapType) {
            MapType otherType = (MapType) other;
            boolean changed = false;
            if (merge(otherType))
                changed = true;
            for (String className : otherType.keyClassNames.keySet()) {
                ClassInfo otherClassInfo = otherType.keyClassNames.get(className);
                if (otherClassInfo == null) {
                    continue;
                }
                ClassInfo classInfo = keyClassNames.get(className);
                if (classInfo == null) {
                    keyClassNames.put(className, otherClassInfo);
                    this.itinerary = otherType.itinerary;
                    this.dumpId = otherType.dumpId;
                    log("new key class", otherType.itinerary, otherType.dumpId);
                    changed = true;
                } else {
                    // merge it
                    if (classInfo.merge(otherClassInfo)) {
                        changed = true;
                        // Runtime.log("[hklog] new format coverage for " + className);
                    }
                }
            }
            for (String className : otherType.valueClassNames.keySet()) {
                ClassInfo otherClassInfo = otherType.valueClassNames.get(className);
                if (otherClassInfo == null) {
                    // Skip this
                    continue;
                }
                ClassInfo classInfo = valueClassNames.get(className);
                if (classInfo == null) {
                    valueClassNames.put(className, otherClassInfo);
                    this.itinerary = otherType.itinerary;
                    this.dumpId = otherType.dumpId;
                    changed = true;
                } else {
                    if (classInfo.merge(otherClassInfo)) {
                        changed = true;
                    }
                }
            }
            return changed;
        }
        throw new RuntimeException(String.format("Not an %s but claimed to be", typeName));
    }

}
