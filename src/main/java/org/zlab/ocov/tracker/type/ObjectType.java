package org.zlab.ocov.tracker.type;

import org.apache.commons.lang3.SerializationUtils;
import org.zlab.ocov.tracker.ClassInfo;
import org.zlab.ocov.tracker.Runtime;

import java.util.HashMap;
import java.util.Map;

public class ObjectType extends TypeInfo {
    private static final long serialVersionUID = 20231215L;

    // Could be Object type, which might be any type
    // Or it could be a specific class type, the class name might change
    boolean beenNullOnce = false;

    public Map<String, ClassInfo> classNames = new HashMap<>();

    public ObjectType() {
        super("object");
    }

    @Override
    public boolean update(Object value, Map<String, ClassInfo> baseClassInfo) {
        // TODO: Handle null situation
        if (value == null) {
            if (!beenNullOnce) {
                beenNullOnce = true;
                return true;
            }
            return false;
        }
        String className = value.getClass().getName();
        if (classNames.containsKey(className)) {
            return classNames.get(className).update(value, baseClassInfo);
        } else {
            // Check whether this is a field that could be serialized
            if (!baseClassInfo.containsKey(className)) {
                return false;
            }
            // Runtime.log("New class " + className);
            ClassInfo newClassInfo = SerializationUtils.clone(baseClassInfo.get(className));
            newClassInfo.update(value, baseClassInfo);
            classNames.put(className, newClassInfo);
            return true;
        }
    }

    @Override
    public boolean merge(TypeInfo otherTypeInfo) {
        if (otherTypeInfo instanceof ObjectType) {
            ObjectType otherObjectType = (ObjectType) otherTypeInfo;
            boolean changed = false;
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
                    changed = true;
                }
            }
            if (changed)
                Runtime.log(String.format("[hklog] %s merge changed", typeName));
            return changed;
        }
        throw new RuntimeException(String.format("Not an %s but claimed to be", typeName));
    }

}
