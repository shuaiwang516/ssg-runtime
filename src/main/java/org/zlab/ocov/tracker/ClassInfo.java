package org.zlab.ocov.tracker;

import org.zlab.ocov.tracker.type.TypeInfo;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class ClassInfo implements Serializable {
    // Iterate all instances of this class, and update the constraint information
    Map<String, TypeInfo> fields = new HashMap<>();

    public boolean update(Object obj) {
        // Iterate all fields
        boolean isNew = false;
        try {
            Field[] fields = obj.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(obj);
                if (update(field.getName(), value)) {
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

    private boolean update(String fieldName, Object value) {
        if (!fields.containsKey(fieldName)) {
            // Only track target fields
            return false;
        }
        TypeInfo typeInfo = fields.get(fieldName);
        assert typeInfo != null;
        return typeInfo.update(value);
    }

}
