package org.zlab.ocov.tracker.type;

import org.apache.commons.lang3.SerializationUtils;
import org.zlab.ocov.tracker.ClassInfo;
import org.zlab.ocov.tracker.Runtime;

import java.util.HashMap;
import java.util.Map;

public class CollectionType extends SequenceType {
    private static final long serialVersionUID = 20231215L;

    public Map<String, ClassInfo> classNames = new HashMap<>();
    public Map<String, Integer> classNamesDumpId = new HashMap<>();

    public CollectionType(String itinerary) {
        super("collection", itinerary);
    }

    @Override
    public void updateItinerary(String itineraryPrefix) {
        itinerary = itineraryPrefix + itinerary;
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
        if (value instanceof java.util.Collection) {
            boolean changed = false;
            // Recursively check the objects inside this collection
            // Do we track all objects inside it?
            // Track all for now
            for (Object object : (java.util.Collection) value) {
                if (object == null) {
                    continue;
                }
                String className = object.getClass().getName();
                if (classNames.containsKey(className)) {
                    if (classNames.get(className).update(object, baseClassInfo, dumpId))
                        changed = true;
                } else {
                    // Check whether this is a field that could be serialized
                    if (baseClassInfo.containsKey(className)) {
                        // Runtime.log("New class " + className);
                        ClassInfo newClassInfo = SerializationUtils
                                .clone(baseClassInfo.get(className));
                        newClassInfo.updateItinerary(itinerary + ".collection_item");
                        newClassInfo.update(object, baseClassInfo, dumpId);
                        classNames.put(className, newClassInfo);
                        classNamesDumpId.put(className, dumpId);
                        changed = true;
                    }
                }
            }
            int size = ((java.util.Collection) value).size();
            if (updateSize(size, dumpId))
                changed = true;
            return changed;
        }
        // Why would it not be a collection type?
        throw new RuntimeException(String.format("Not an %s but claimed to be", typeName));
    }

    /**
     * OriCov NewCov - dumpId = 1 => New object - dumpId = 2 => Another object but
     * with different format
     *
     * When merging: OriCov.merge(NewCov) - The object is created by 2 dumps, but
     * the second dump is - recorded inside the dump. - We should only record
     * dumpId1
     */
    @Override
    public boolean merge(TypeInfo other) {
        if (other instanceof CollectionType) {
            CollectionType otherType = (CollectionType) other;
            boolean changed = false;
            if (merge(otherType))
                changed = true;
            for (String className : otherType.classNames.keySet()) {
                if (classNames.containsKey(className)) {
                    if (classNames.get(className).merge(otherType.classNames.get(className)))
                        changed = true;
                } else {
                    ClassInfo newClassInfo = SerializationUtils
                            .clone(otherType.classNames.get(className));
                    classNames.put(className, newClassInfo);
                    classNamesDumpId.put(className, otherType.classNamesDumpId.get(className));
                    log("new class in collection: " + className, this.itinerary,
                            otherType.classNamesDumpId.get(className));
                    changed = true;
                }
            }
            return changed;
        }
        throw new RuntimeException(String.format("Not an %s but claimed to be", typeName));
    }

}
