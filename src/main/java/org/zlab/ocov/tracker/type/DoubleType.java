package org.zlab.ocov.tracker.type;

import org.zlab.ocov.tracker.ClassInfo;

import java.util.Map;
import java.util.Set;

public class DoubleType extends TypeInfo {
    double max = Double.MIN_VALUE;
    double min = Double.MAX_VALUE;

    // describe some characteristics
    public DoubleType() {
        super("Float");
    }

    @Override
    public boolean update(Object value, Map<String, ClassInfo> baseClassInfo) {

        if (value == null) {
            return false;
        }
        if (value instanceof Double) {
            double v = (Double) value;
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
        throw new RuntimeException(String.format("Not an %s but claimed to be", typeName));
    }

}
