package org.zlab.dinv.logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LogEntry {
    public int threadId;
    public VariableInfo object;
    public VariableInfo field;

    public LogEntry() {
    }

    public LogEntry(int threadId, Object object, Object field, String name) {
        this.threadId = threadId;
        if (object == null)
            this.object = null;
        else
            this.object = createVariableInfo(object, null);
        if (field == null)
            this.field = null;
        else
            this.field = createVariableInfo(field, name);
    }

    public static LogEntry constructLogEntry(int threadId, Object object, Object field,
            String name) {
        return new LogEntry(threadId, object, field, name);
    }

    public enum VariableType {
        INTEGER, FLOAT, BOOLEAN, CHARACTER, BYTE, SHORT, LONG, DOUBLE, STRING, ENUM, UNKNOWN;

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
    }

    public static class VariableInfo {
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
    }

    public static VariableInfo createVariableInfo(Object var, String name) {
        // Add a checker: if it's primitive, we don't use identified hashcode
        VariableInfo varInfo = new VariableInfo();
        // type
        varInfo.type = VariableType.getVariableType(var);
        // className
        varInfo.className = var.getClass().getName();
        // identifyHash
        if (!varInfo.type.isPrimitive())
            varInfo.identifyHash = System.identityHashCode(var);
        // value
        varInfo.value = var.toString();
        // name
        varInfo.name = name;
        return varInfo;
    }

    // transform to json string
    public String toJsonString() {
        ObjectMapper mapper = new ObjectMapper();
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
        return "LogEntry{" + "threadId=" + threadId + ", object=" + object + ", field=" + field
                + '}';
    }

}
