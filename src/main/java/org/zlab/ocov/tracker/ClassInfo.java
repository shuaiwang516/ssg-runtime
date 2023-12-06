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
    public boolean update(Object obj,
            Map<String, ClassInfo> baseClassInfo) {
        // Iterate all fields
        boolean isNew = false;
        try {
            Field[] fields = obj.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(obj);
                Runtime.log("[hklog] processing field name = " + field.getName() + ", value = "
                        + value);
                // get field class name
                String fieldName = field.getName();
                String fieldClassName = field.getType().getName();
                // if fieldClassName is visited, skip it to avoid stackoverflow
                // if field is static and final, skip it: but still might change?
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())
                        && java.lang.reflect.Modifier.isFinal(field.getModifiers())) {
                    Runtime.log("[hklog] field is static and final, skip it");
                    continue;
                }
                // Log classname + name
                Runtime.log("[hklog] end fieldClassName = " + fieldClassName + ", fieldName = "
                        + fieldName);
                if (update(fieldName, value, baseClassInfo)) {
                    if (!isNew)
                        isNew = true;
                }
                Runtime.log(fieldName + ": " + value);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return isNew;
    }

    private boolean update(String fieldName, Object value,
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
        return typeInfo.update(value, baseClassInfo);
    }

}
