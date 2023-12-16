package org.zlab.ocov.tracker.type;

import org.zlab.ocov.tracker.ClassInfo;

import java.util.Map;

public class StringType extends TypeInfo {
    private static final long serialVersionUID = 20231215L;

    int max = Integer.MIN_VALUE;
    int min = Integer.MAX_VALUE;

    public StringType() {
        super("String");
    }

    @Override
    public boolean update(Object value, Map<String, ClassInfo> baseClassInfo) {
        if (value == null) {
            return false;
        }
        if (value instanceof String) {
            String v = (String) value;
            int len = v.length();
            boolean changed = false;
            if (len > max) {
                max = v.length();
                changed = true;
            }
            if (len < min) {
                min = v.length();
                changed = true;
            }
            return changed;
        }
        throw new RuntimeException(String.format("Not an %s but claimed to be", typeName));
    }

    @Override
    public boolean merge(TypeInfo otherTypeInfo) {
        if (otherTypeInfo instanceof StringType) {
            StringType otherStringType = (StringType) otherTypeInfo;
            boolean changed = false;
            if (otherStringType.max > max) {
                max = otherStringType.max;
                changed = true;
            }
            if (otherStringType.min < min) {
                min = otherStringType.min;
                changed = true;
            }
            return changed;
        }
        throw new RuntimeException(String.format("Not an %s but claimed to be", typeName));
    }

}
