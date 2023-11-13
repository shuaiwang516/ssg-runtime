package org.zlab.dinv.serializepoint;

import java.util.Objects;

public class SerializePoint {

    public enum Type {
        fieldRef, iterator, collectionGet, arrayRef
    }

    // Location
    public String className;
    public String methodName;
    public int lineNumber;

    public boolean isStatic;

    public Type type;

    public String parentName; // could be this, null if static
    public String fieldName; // handle collection in a special way

    public SerializePoint() {
    }

    public SerializePoint(String className, String methodName, int lineNumber, boolean isStatic,
            Type type, String parentName, String fieldName) {
        this.className = className;
        this.methodName = methodName;
        this.lineNumber = lineNumber;
        this.isStatic = isStatic;
        this.type = type;
        this.parentName = parentName;
        this.fieldName = fieldName;
    }

    // Equal methods, return 2 objects to be equal as long as the fields are equal
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SerializePoint serializePoint = (SerializePoint) obj;

        boolean isPosEqual = (className.equals(serializePoint.className))
                && (methodName.equals(serializePoint.methodName))
                && (lineNumber == serializePoint.lineNumber);

        if (!isPosEqual || isStatic != serializePoint.isStatic || type != serializePoint.type)
            return false;

        if (type != Type.fieldRef) {
            return parentName == null && serializePoint.parentName == null && fieldName == null
                    && serializePoint.fieldName == null;
        } else {
            return parentName.equals(serializePoint.parentName)
                    && fieldName.equals(serializePoint.fieldName);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, methodName, lineNumber, isStatic, type, parentName,
                fieldName);
    }

    // toString method
    @Override
    public String toString() {
        return "SerializePoint{" + "className='" + className + '\'' + ", methodName='" + methodName
                + '\'' + ", lineNumber=" + lineNumber + ", isStatic=" + isStatic + ", type=" + type
                + ", parentName='" + parentName + '\'' + ", fieldName='" + fieldName + '\'' + '}';
    }

}
