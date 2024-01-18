package org.zlab.ocov.tracker.type;

import org.apache.commons.lang3.SerializationUtils;
import org.zlab.ocov.tracker.ClassInfo;
import org.zlab.ocov.tracker.EqualitySet;
import org.zlab.ocov.tracker.IsSerialize;
import org.zlab.ocov.tracker.Runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ObjectType extends TypeInfo {
    private static final long serialVersionUID = 20231215L;

    // Could be Object type, which might be any type
    // Or it could be a specific class type, the class name might change
    boolean beenNullOnce = false;
    int dumpIdNullOnce = -1;

    // Enum Type
    boolean beenEnumOnce = false;
    // Enum Constants
    public Map<String, Set<String>> enumConstants = new HashMap<>();
    public Map<String, Set<String>> visitedEnumConstants = new HashMap<>();
    public Map<String, Map<String, Integer>> visitedEnumConstantsDumpId = new HashMap<>();

    public Map<String, ClassInfo> classNames = new HashMap<>();
    public Map<String, Integer> classNamesDumpId = new HashMap<>();

    public ObjectType(String itinerary) {
        super("object", itinerary);
    }

    @Override
    public void updateItinerary(String itineraryPrefix) {
        // Runtime.log("[hklog] ObjectType1 itinerary = " + itinerary + "
        // itineraryPrefix = "
        // + itineraryPrefix);
        itinerary = itineraryPrefix + itinerary;
        // Update itinerary for all classInfo
        for (String className : classNames.keySet()) {
            classNames.get(className).updateItinerary(itineraryPrefix);
        }
        // Runtime.log("[hklog] ObjectType2 itinerary = " + itinerary + "
        // itineraryPrefix = "
        // + itineraryPrefix);
    }

    @Override
    public boolean update(Object value, Map<String, ClassInfo> baseClassInfo, int dumpId,
            EqualitySet equalitySet, IsSerialize isSerialized) {
        if (value == null) {
            if (!beenNullOnce) {
                beenNullOnce = true;
                dumpIdNullOnce = dumpId;
                return true;
            }
            return false;
        }

        boolean changed = false;
        if (value.getClass().isEnum()) {
            // Object Enum Tracking
            if (!beenEnumOnce) {
                beenEnumOnce = true;
            }
            String enumName = value.getClass().getName();
            if (!enumConstants.containsKey(enumName)) {
                // Only need to update once
                enumConstants.put(enumName, new java.util.HashSet<>());
                for (Object enumConstant : value.getClass().getEnumConstants()) {
                    String enumConstantName = enumConstant.toString();
                    enumConstants.get(enumName).add(enumConstantName);
                }
            }
            // Update visited constants
            // Check whether it's visited before
            boolean visited = false;
            if (visitedEnumConstants.containsKey(enumName)) {
                if (visitedEnumConstants.get(enumName).contains(value.toString())) {
                    visited = true;
                }
            }
            if (!visited) {
                // Update visited constants
                if (!visitedEnumConstants.containsKey(enumName)) {
                    visitedEnumConstants.put(enumName, new java.util.HashSet<>());
                }
                visitedEnumConstants.get(enumName).add(value.toString());
                // Update dumpId
                if (!visitedEnumConstantsDumpId.containsKey(enumName)) {
                    visitedEnumConstantsDumpId.put(enumName, new HashMap<>());
                }
                visitedEnumConstantsDumpId.get(enumName).put(value.toString(), dumpId);
                // A new constant is reached
                changed = true;
            }

            // isSerialized Enum Tracking
            if (isSerialized != null) {
                isSerialized.updateVisitedEnums(enumName, value.toString());
            }
        }

        String className = value.getClass().getName();
        if (classNames.containsKey(className)) {
            if (classNames.get(className).update(value, baseClassInfo, dumpId, equalitySet,
                    isSerialized))
                changed = true;
        } else {
            // Check whether this is a field that could be serialized
            if (baseClassInfo.containsKey(className)) {
                // Runtime.log("New class " + className);
                ClassInfo newClassInfo = SerializationUtils.clone(baseClassInfo.get(className));
                newClassInfo.updateItinerary(itinerary);
                newClassInfo.update(value, baseClassInfo, dumpId, equalitySet, isSerialized);
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
            if (otherObjectType.beenEnumOnce && !beenEnumOnce) {
                beenEnumOnce = true;
                changed = true;
            }
            // merge enum related
            if (otherObjectType.beenEnumOnce) {
                // merge enum Constants
                for (Map.Entry<String, Set<String>> entry : otherObjectType.enumConstants
                        .entrySet()) {
                    String enumName = entry.getKey();
                    Set<String> otherEnumConstants = entry.getValue();
                    if (!enumConstants.containsKey(enumName)) {
                        enumConstants.put(enumName, new java.util.HashSet<>());

                    }
                }
                // merge visited enum Constants
                for (Map.Entry<String, Set<String>> entry : otherObjectType.visitedEnumConstants
                        .entrySet()) {
                    String enumName = entry.getKey();
                    Set<String> otherVisitedEnumConstants = entry.getValue();
                    if (!visitedEnumConstants.containsKey(enumName)) {
                        visitedEnumConstants.put(enumName, new java.util.HashSet<>());
                    }
                    if (!visitedEnumConstantsDumpId.containsKey(enumName)) {
                        visitedEnumConstantsDumpId.put(enumName, new HashMap<>());
                    }
                    for (String visitedEnumConstant : otherVisitedEnumConstants) {
                        if (!visitedEnumConstants.get(enumName).contains(visitedEnumConstant)) {
                            visitedEnumConstants.get(enumName).add(visitedEnumConstant);
                            visitedEnumConstantsDumpId.get(enumName).put(visitedEnumConstant,
                                    otherObjectType.visitedEnumConstantsDumpId.get(enumName)
                                            .get(visitedEnumConstant));
                            log("new visited enum constant: " + visitedEnumConstant, itinerary,
                                    otherObjectType.visitedEnumConstantsDumpId.get(enumName)
                                            .get(visitedEnumConstant));
                            changed = true;
                        }
                    }
                }
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
