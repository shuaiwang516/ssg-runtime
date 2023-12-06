package org.zlab.ocov.tracker.type;

import org.zlab.ocov.tracker.ClassInfo;

import java.util.Map;
import java.util.Set;

public class BooleanType extends TypeInfo {
    int trueCount = 0;
    int falseCount = 0;

    // describe some characteristics
    public BooleanType() {
        super("Boolean");
    }

    @Override
    public boolean update(Object value, Set<String> visitedClasses,
            Map<String, ClassInfo> baseClassInfo) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            boolean v = (Boolean) value;
            boolean changed = false;
            if (v) {
                trueCount++;
                changed = true;
            } else {
                falseCount++;
                changed = true;
            }
            return changed;
        }
        throw new RuntimeException(String.format("Not an %s but claimed to be", typeName));
    }

}
