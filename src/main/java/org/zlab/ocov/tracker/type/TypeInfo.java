package org.zlab.ocov.tracker.type;

import java.io.Serializable;

public abstract class TypeInfo implements Serializable {
    // Constraint information

    // Declared in source code
    public String typeName;

    public TypeInfo(String typeName) {
        this.typeName = typeName;
    }

    // Update constraint information
    public abstract boolean update(Object value);

}
