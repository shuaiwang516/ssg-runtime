package org.zlab.ocov.tracker;

import org.zlab.ocov.tracker.type.TypeInfo;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ClassInfo implements Serializable {
    private static final long serialVersionUID = 20231215L;

    // Iterate all instances of this class, and update the constraint information
    public Map<String, TypeInfo> fields = new HashMap<>();

    public String itinerary;

    public ClassInfo(String itinerary) {
        this.itinerary = itinerary;
    }

    // Update itinerary if necessary
    public void updateItinerary(String itineraryPrefix) {
        // Runtime.log("[hklog] ClassInfo1 itinerary = " + itinerary + " itineraryPrefix
        // = "
        // + itineraryPrefix);
        itinerary = itineraryPrefix + "->" + itinerary;
        for (String fieldName : fields.keySet()) {
            TypeInfo typeInfo = fields.get(fieldName);
            if (typeInfo == null) {
                // Skip this
                continue;
            }
            typeInfo.updateItinerary(itineraryPrefix + "->");
        }
        // Runtime.log("[hklog] ClassInfo2 itinerary = " + itinerary + " itineraryPrefix
        // = "
        // + itineraryPrefix);
    }

    public boolean update(Object obj, Map<String, ClassInfo> baseClassInfo, int dumpId,
            EqualitySet equalitySet, IsSerialize isSerialized, int objId) {
        if (obj == null) {
            return false;
        }
        boolean isNew = false;
        try {
            // Equality likely invariants
            if (equalitySet != null) {
                String fieldClassName = obj.getClass().getName();
                equalitySet.update(obj, fieldClassName, itinerary, objId);
            }
            // IsSerialize likely invariants
            if (isSerialized != null) {
                String fieldClassName = obj.getClass().getName();
                isSerialized.updateVisitedClasses(fieldClassName);
            }
            Class<?> currentClass = obj.getClass();
            while (currentClass != Object.class) { // Traverse up the class hierarchy
                Field[] fields = currentClass.getDeclaredFields();
                for (Field field : fields) {
                    if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())
                            || !java.lang.reflect.Modifier.isFinal(field.getModifiers())) {
                        field.setAccessible(true);
                        Object value = field.get(obj);

                        String fieldName = field.getName();
                        String objectClassName = obj.getClass().getName();
                        // Runtime.log("[hklog] processing object classname = " + objectClassName
                        // + ", field = " + field.getName() + ", value = " + value);
                        if (update(fieldName, value, baseClassInfo, dumpId, equalitySet,
                                isSerialized, objId)) {
                            isNew = true;
                        }
                    }
                }
                currentClass = currentClass.getSuperclass(); // Move to the superclass
            }
            // Runtime.log("");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return isNew;
    }
    private boolean update(String fieldName, Object value, Map<String, ClassInfo> baseClassInfo,
            int id, EqualitySet equalitySet, IsSerialize isSerialized, int objId) {
        if (!fields.containsKey(fieldName)) {
            // Only track target fields
            return false;
        }
        TypeInfo typeInfo = fields.get(fieldName);
        // if typeInfo is null, skip it
        if (typeInfo == null) {
            return false;
        }
        return typeInfo.update(value, baseClassInfo, id, equalitySet, isSerialized, objId);
    }

    public boolean merge(ClassInfo otherClassInfo) {
        boolean newCoverage = false;
        for (String fieldName : otherClassInfo.fields.keySet()) {
            TypeInfo otherTypeInfo = otherClassInfo.fields.get(fieldName);
            if (otherTypeInfo == null) {
                // Skip this
                continue;
            }
            TypeInfo typeInfo = fields.get(fieldName);
            if (typeInfo == null) {
                // FIXME: This could be removed?
                fields.put(fieldName, otherTypeInfo);
                newCoverage = true;
            } else {
                // merge it
                if (typeInfo.merge(otherTypeInfo))
                    newCoverage = true;
            }
        }
        return newCoverage;
    }

}
