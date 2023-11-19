package org.zlab.dinv.logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.util.Objects;

public class LogEntry {
    public long threadId;
    public VariableInfo parent;
    public VariableInfo field;

    public static final ObjectMapper mapper = new ObjectMapper();

    public LogEntry() {
    }

    public LogEntry(Object parent, Object field, String name) {
        this.threadId = Thread.currentThread().getId();
        if (parent == null)
            this.parent = null;
        else
            this.parent = createVariableInfo(parent, null);
        if (field == null)
            this.field = null;
        else
            this.field = createVariableInfo(field, name);
    }

    public LogEntry(Class<?> clazz, Object field, String name) {
        this.threadId = Thread.currentThread().getId();
        this.parent = createVariableInfo(clazz, null);
        if (field == null)
            this.field = null;
        else
            this.field = createVariableInfo(field, name);
    }

    public static LogEntry constructLogEntry(Object object, Object field, String name) {
        return new LogEntry(object, field, name);
    }

    public static LogEntry constructLogEntry(Class<?> clazz, Object field, String name) {
        return new LogEntry(clazz, field, name);
    }

    public enum VariableType {
        INTEGER, FLOAT, BOOLEAN, CHARACTER, BYTE, SHORT, LONG, DOUBLE, STRING, ENUM, CLASS, UNKNOWN;

        public static VariableType getVariableType(Object object) {
            if (object instanceof Integer) {
                return VariableType.INTEGER;
            } else if (object instanceof Float) {
                return VariableType.FLOAT;
            } else if (object instanceof Boolean) {
                return VariableType.BOOLEAN;
            } else if (object instanceof Character) {
                return VariableType.CHARACTER;
            } else if (object instanceof Byte) {
                return VariableType.BYTE;
            } else if (object instanceof Short) {
                return VariableType.SHORT;
            } else if (object instanceof Long) {
                return VariableType.LONG;
            } else if (object instanceof Double) {
                return VariableType.DOUBLE;
            } else if (object instanceof String) {
                return VariableType.STRING;
            } else if (object instanceof Enum) {
                return VariableType.ENUM;
            } else {
                return VariableType.UNKNOWN;
            }
        }

        public boolean isPrimitive() {
            return this != STRING && this != ENUM && this != UNKNOWN;
        }

        public boolean isPrintable() {
            return this != CLASS && this != UNKNOWN;
        }
    }

    public static class VariableInfo implements Serializable {
        public VariableType type; // This will hold either the primitive type name or "String"
        public String className;
        public int identifyHash = 0;
        public String value; // hashcode for object, value for primitive
        public String name = null; // field name
        public VariableInfo() {
        }
        @Override
        public String toString() {
            return "VariableInfo{" + "type=" + type + ", className='" + className + '\''
                    + ", identifyHash=" + identifyHash + ", value='" + value + '\'' + ", name='"
                    + name + '\'' + '}';
        }
        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof VariableInfo))
                return false;
            VariableInfo that = (VariableInfo) o;
            if (identifyHash != that.identifyHash)
                return false;
            if (type != that.type)
                return false;
            if (!Objects.equals(className, that.className))
                return false;
            if (!Objects.equals(value, that.value))
                return false;
            return Objects.equals(name, that.name);
        }
        @Override
        public int hashCode() {
            return Objects.hash(type, className, identifyHash, value, name);
        }
    }

    public static VariableInfo createVariableInfo(Object var, String name) {
        VariableInfo varInfo = new VariableInfo();
        varInfo.type = VariableType.getVariableType(var);
        varInfo.className = var.getClass().getName();
        if (!varInfo.type.isPrimitive())
            varInfo.identifyHash = System.identityHashCode(var);
        if (varInfo.type.isPrintable())
            varInfo.value = var.toString();
        varInfo.name = name;
        return varInfo;
    }

    public static VariableInfo createVariableInfo(Class<?> clazz, String name) {
        VariableInfo varInfo = new VariableInfo();
        varInfo.type = VariableType.CLASS;
        varInfo.className = clazz.getName();
        varInfo.value = null; // clazz type do not have value
        varInfo.name = name;
        return varInfo;
    }

    // transform to json string
    public String toJsonString() {
        // return "INFO [MemtableFlushWriter:1] 2023-11-18 15:16:59,582
        // CompoundSparseCellName.java:63";
        String jsonString;
        try {
            jsonString = mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return jsonString;
    }

    public static LogEntry fromJsonString(String jsonString) {
        ObjectMapper mapper = new ObjectMapper();
        LogEntry logEntry;
        try {
            logEntry = mapper.readValue(jsonString, LogEntry.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return logEntry;
    }

    @Override
    public String toString() {
        return "LogEntry{" + "threadId=" + threadId + ", object=" + parent + ", field=" + field
                + '}';
    }

}
