package org.zlab.ocov.tracker;

import org.zlab.ocov.tracker.type.TypeInfo;

import java.util.HashMap;
import java.util.Map;

public class ClassInfo {
    // Iterate all instances of this class, and update the constraint information
    Map<String, TypeInfo> fields = new HashMap<>();

    public boolean update(String fieldName, Object value) {
        if (!fields.containsKey(fieldName)) {
            // Not our target field
            return false;
        }
        TypeInfo typeInfo = fields.get(fieldName);
        assert typeInfo != null;
        return typeInfo.update(value);
    }
}
