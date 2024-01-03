package org.zlab.ocov.tracker.type;

import org.zlab.ocov.tracker.ClassInfo;
import org.zlab.ocov.tracker.Runtime;

import java.util.Map;

public class BooleanType extends TypeInfo {
    private static final long serialVersionUID = 20231215L;

    boolean beenTrueOnce = false;
    boolean beenFalseOnce = false;
    boolean beenNullOnce = false;

    // describe some characteristics
    public BooleanType() {
        super("Boolean");
    }

    @Override
    public boolean update(Object value, Map<String, ClassInfo> baseClassInfo) {
        if (value == null) {
            if (!beenNullOnce) {
                beenNullOnce = true;
                return true;
            }
            return false;
        }
        if (value instanceof Boolean) {
            boolean v = (Boolean) value;
            boolean changed = false;
            if (v) {
                if (!beenTrueOnce) {
                    beenTrueOnce = true;
                    changed = true;
                }
            } else {
                if (!beenFalseOnce) {
                    beenFalseOnce = true;
                    changed = true;
                }
            }
            return changed;
        }
        throw new RuntimeException(String.format("Not an %s but claimed to be", typeName));
    }

    @Override
    public boolean merge(TypeInfo otherTypeInfo) {
        if (otherTypeInfo instanceof BooleanType) {
            BooleanType otherBooleanType = (BooleanType) otherTypeInfo;
            boolean changed = false;
            if (otherBooleanType.beenTrueOnce && !beenTrueOnce) {
                beenTrueOnce = true;
                changed = true;
            }
            if (otherBooleanType.beenFalseOnce && !beenFalseOnce) {
                beenFalseOnce = true;
                changed = true;
            }
            if (changed)
                Runtime.log(String.format("[hklog] %s merge changed", typeName));
            return changed;
        }
        throw new RuntimeException(String.format("Not an %s but claimed to be", typeName));
    }

}
