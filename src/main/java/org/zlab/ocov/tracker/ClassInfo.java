package org.zlab.ocov.tracker;

import org.zlab.ocov.Utils;
import org.zlab.ocov.tracker.type.TypeInfo;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ClassInfo implements Serializable {
    // Iterate all instances of this class, and update the constraint information
    Map<String, TypeInfo> fields = new HashMap<>();

    // Additional Relationships
    public boolean update(Object obj, Set<String> visitedClasses,
            Map<String, ClassInfo> baseClassInfo) {
        // Iterate all fields
        boolean isNew = false;
        try {
            Field[] fields = obj.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(obj);

                // get field class name
                String fieldClassName = field.getType().getName();
                // if fieldClassName is visited, skip it to avoid stackoverflow
                if (visitedClasses.contains(fieldClassName)) {
                    continue;
                }
                if (!Utils.isPrimitiveType(fieldClassName)) {
                    visitedClasses.add(fieldClassName);
                }

                // if field is static and final, skip it: but still might change?
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())
                        && java.lang.reflect.Modifier.isFinal(field.getModifiers())) {
                    continue;
                }

                if (update(field.getName(), value, visitedClasses, baseClassInfo)) {
                    if (!isNew)
                        isNew = true;
                }
                System.out.println(field.getName() + ": " + value);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return isNew;
    }

    private boolean update(String fieldName, Object value, Set<String> visitedClasses,
            Map<String, ClassInfo> baseClassInfo) {
        if (!fields.containsKey(fieldName)) {
            // Only track target fields
            return false;
        }
        TypeInfo typeInfo = fields.get(fieldName);
        // if typeInfo is null, skip it
        if (typeInfo == null) {
            return false;
        }
        return typeInfo.update(value, visitedClasses, baseClassInfo);
    }

}
