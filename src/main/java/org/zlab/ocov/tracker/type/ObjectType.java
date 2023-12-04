package org.zlab.ocov.tracker.type;

import org.apache.commons.lang3.SerializationUtils;
import org.zlab.ocov.tracker.ClassInfo;
import org.zlab.ocov.tracker.ObjectCoverage;

import java.util.HashMap;
import java.util.Map;

public class ObjectType extends TypeInfo {
    // Could be Object type, which might be any type
    // Or it could be a specific class type, the class name might change

    public Map<String, ClassInfo> classNames = new HashMap<>();

    public ObjectType() {
        super("object");
    }

    @Override
    public boolean update(Object value) {
        String className = value.getClass().getName();
        if (classNames.containsKey(className)) {
            return classNames.get(className).update(value);
        } else {
            // Check whether this is a field that could be serialized
            if (!ObjectCoverage.baseClassInfo.containsKey(className)) {
                return false;
            }
            ClassInfo newClassInfo = SerializationUtils
                    .clone(ObjectCoverage.baseClassInfo.get(className));
            newClassInfo.update(value);
            classNames.put(className, newClassInfo);
            return true;
        }
    }

}
