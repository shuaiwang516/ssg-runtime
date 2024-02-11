package org.zlab.ocov.tracker.type;

import org.apache.commons.lang3.SerializationUtils;
import org.zlab.ocov.tracker.ClassInfo;
import org.zlab.ocov.tracker.EqualitySet;
import org.zlab.ocov.tracker.IsSerialize;
import org.zlab.ocov.tracker.graph.ObjectGraph;
import org.zlab.ocov.tracker.inv.unary.LogInfo;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;

public class ArrayType extends SequenceType {
    private static final long serialVersionUID = 20231215L;

    public Map<String, ClassInfo> classNames = new HashMap<>();
    public Map<String, Integer> classNamesDumpId = new HashMap<>();

    public ArrayType(String itinerary) {
        super("array", itinerary);
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
    public boolean update(Object value, Map<String, ClassInfo> baseClassInfo, int dumpId,
            EqualitySet equalitySet, IsSerialize isSerialized, int objId) {
        if (value == null)
            return nullOnce.add(value, new LogInfo(dumpId));

        if (value.getClass().isArray()) {
            boolean changed = false;
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                Object object = Array.get(value, i);
                if (object == null) {
                    continue;
                }
                String className = object.getClass().getName();
                if (classNames.containsKey(className)) {
                    if (classNames.get(className).update(object, baseClassInfo, dumpId, equalitySet,
                            isSerialized, objId))
                        changed = true;
                } else {
                    // Check whether this is a field that could be serialized
                    if (baseClassInfo.containsKey(className)) {
                        // Runtime.log("New class " + className);
                        ClassInfo newClassInfo = SerializationUtils
                                .clone(baseClassInfo.get(className));
                        newClassInfo.updateItinerary(itinerary + ".array_item");
                        newClassInfo.update(object, baseClassInfo, dumpId, equalitySet,
                                isSerialized, objId);
                        classNames.put(className, newClassInfo);
                        classNamesDumpId.put(className, dumpId);
                        changed = true;
                    }
                }
            }
            if (updateSize(length, dumpId))
                changed = true;
            return changed;
        }
        throw new RuntimeException("Not an array but claimed to be");
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

}
