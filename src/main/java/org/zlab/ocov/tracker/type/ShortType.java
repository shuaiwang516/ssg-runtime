package org.zlab.ocov.tracker.type;

import org.zlab.ocov.tracker.ClassInfo;

import java.util.Map;
import java.util.Set;

public class ShortType extends TypeInfo {
    short max = Short.MIN_VALUE;
    short min = Short.MAX_VALUE;

    // describe some characteristics
    public ShortType() {
        super("Short");
    }

    @Override
    public boolean update(Object value, Map<String, ClassInfo> baseClassInfo) {
        if (value == null) {
            return false;
        }
        if (value instanceof Short) {
            short v = (Short) value;
            boolean changed = false;
            if (v > max) {
                max = v;
                changed = true;
            }
            if (v < min) {
                min = v;
                changed = true;
            }
            return changed;
        }
        // Why would it not be integer?
        throw new RuntimeException(String.format("Not an %s but claimed to be", typeName));
    }

}
