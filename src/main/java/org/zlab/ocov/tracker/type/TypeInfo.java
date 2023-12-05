package org.zlab.ocov.tracker.type;

import org.zlab.ocov.tracker.ClassInfo;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public abstract class TypeInfo implements Serializable {
    // Constraint information

    // Declared in source code
    public String typeName;

    public TypeInfo(String typeName) {
        this.typeName = typeName;
    }

    // Update constraint information
    public abstract boolean update(Object value, Set<String> visitedClasses,
            Map<String, ClassInfo> baseClassInfo);

}
