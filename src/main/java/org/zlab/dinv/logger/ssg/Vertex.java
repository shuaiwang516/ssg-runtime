package org.zlab.dinv.logger.ssg;

import org.zlab.dinv.logger.LogEntry;

import java.io.Serializable;
import java.util.*;

public class Vertex implements Serializable {
    private static final long serialVersionUID = 20231122L;

    public LogEntry.VariableType type; // This will hold either the primitive type name or "String"
    public String className;

    public final int identifyHash;
    public String value; // hashcode for object, value for primitive

    public Vertex(LogEntry.VariableInfo variableInfo) {
        this.type = variableInfo.type;
        this.className = variableInfo.className;
        if (variableInfo.identifyHash == 0) {
            // Static class
            if (variableInfo.className != null
                    && variableInfo.type == LogEntry.VariableType.CLASS) {
                identifyHash = variableInfo.className.hashCode();
            } else {
                // Primitives
                identifyHash = Objects.hash(variableInfo.type, variableInfo.className,
                        variableInfo.value);
            }
        } else {
            // primitive type will still be 0
            identifyHash = variableInfo.identifyHash;
        }
        this.value = variableInfo.value;
    }

    public boolean isCollectionOrArray() {
        return className.toLowerCase().contains("list")
                || className.toLowerCase().contains("array");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Vertex))
            return false;
        Vertex that = (Vertex) o;
        return identifyHash == that.identifyHash;
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifyHash);
    }

    @Override
    public String toString() {
        return "Vertex{" + "type=" + type + ", className='" + className + '\'' + ", identifyHash="
                + identifyHash + ", value='" + value + '\'' + '}';
    }

}
