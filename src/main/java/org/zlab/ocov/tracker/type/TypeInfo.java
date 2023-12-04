package org.zlab.ocov.tracker.type;

public abstract class TypeInfo {
    // Constraint information

    // Declared in source code
    public String typeName;

    public TypeInfo(String typeName) {
        this.typeName = typeName;
    }

    // Update constraint information
    public abstract boolean update(Object value);

}
