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
        itinerary = itineraryPrefix + "->" + itinerary;
        for (String fieldName : fields.keySet()) {
            TypeInfo typeInfo = fields.get(fieldName);
            if (typeInfo == null) {
                // Skip this
                continue;
            }
            typeInfo.updateItinerary(itineraryPrefix + "->");
        }
    }

    public boolean update(Object obj, Map<String, ClassInfo> baseClassInfo, int dumpId,
            EqualitySet equalitySet) {
        if (obj == null) {
            return false;
        }
        boolean isNew = false;
        try {
            // Equality check
            if (equalitySet != null) {
                String fieldClassName = obj.getClass().getName();
                if (equalitySet.comparableClasses.contains(fieldClassName)) {
                    // Comparable classes! Mark it
                    int hashCode = obj.hashCode();
                    // get itinerary
                    // Add it to the equality set
                    Map<Integer, Set<String>> hashCodeMap = equalitySet.compClass2EqualitySet
                            .computeIfAbsent(fieldClassName, k -> new HashMap<>());
                    Set<String> itinerarySet = hashCodeMap.computeIfAbsent(hashCode,
                            k -> new HashSet<>());
                    itinerarySet.add(this.itinerary);
                    // Runtime.log("[hklog] equality check: fieldClassName = " + fieldClassName
                    // + ", hashCode = " + hashCode + ", itinerary = " + itinerary
                    // + ", equalityset = " + itinerarySet);
                }
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

                        if (update(fieldName, value, baseClassInfo, dumpId, equalitySet)) {
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
            int id, EqualitySet equalitySet) {
        if (!fields.containsKey(fieldName)) {
            // Only track target fields
            return false;
        }
        TypeInfo typeInfo = fields.get(fieldName);
        // if typeInfo is null, skip it
        if (typeInfo == null) {
            return false;
        }
        return typeInfo.update(value, baseClassInfo, id, equalitySet);
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
