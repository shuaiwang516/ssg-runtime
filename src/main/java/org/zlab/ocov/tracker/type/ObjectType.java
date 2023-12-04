package org.zlab.ocov.tracker.type;

import java.util.Set;

public class ObjectType extends TypeInfo {
    // Could be Object type, which might be any type
    // Or it could be a specific class type, the class name might change

    public Set<String> classNames;

    public ObjectType(String typeName) {
        super(typeName);
    }

    @Override
    public boolean update(Object value) {
        String className = value.getClass().getName();
        if (classNames.contains(className)) {
            return false;
        } else {
            classNames.add(className);
            return true;
        }
    }

}
