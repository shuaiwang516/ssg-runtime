package org.zlab.ocov.tracker.type;

import org.apache.commons.lang3.SerializationUtils;
import org.zlab.ocov.tracker.ClassInfo;
import org.zlab.ocov.tracker.ObjectCoverage;
import org.zlab.ocov.tracker.Runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ObjectType extends TypeInfo {
    // Could be Object type, which might be any type
    // Or it could be a specific class type, the class name might change

    public Map<String, ClassInfo> classNames = new HashMap<>();

    public ObjectType() {
        super("object");
    }

    @Override
    public boolean update(Object value, Set<String> visitedClasses,
            Map<String, ClassInfo> baseClassInfo) {
        // TODO: Handle null situation
        if (value == null) {
            return false;
        }
        String className = value.getClass().getName();
        if (classNames.containsKey(className)) {
            return classNames.get(className).update(value, visitedClasses, baseClassInfo);
        } else {
            // Check whether this is a field that could be serialized
            if (!baseClassInfo.containsKey(className)) {
                return false;
            }
            Runtime.log("New class " + className);
            // Avoid self reference
            ClassInfo newClassInfo = SerializationUtils.clone(baseClassInfo.get(className));
            newClassInfo.update(value, visitedClasses, baseClassInfo);
            classNames.put(className, newClassInfo);
            return true;
        }
    }

}
