package org.zlab.ocov.tracker.type;

import org.zlab.ocov.tracker.ClassInfo;
import org.zlab.ocov.tracker.EqualitySet;
import org.zlab.ocov.tracker.Runtime;
import org.zlab.ocov.tracker.inv.unary.LogInfo;

import java.util.Map;

public class DoubleType extends ScalaType {
    private static final long serialVersionUID = 20231215L;

    // describe some characteristics
    public DoubleType(String itinerary) {
        super("Double", itinerary);
    }

    @Override
    public boolean update(Object value, Map<String, ClassInfo> baseClassInfo, int dumpId) {
        if (value == null)
            return nullOnce.add(value, new LogInfo(dumpId));

        if (value instanceof Double) {
            double v = (Double) value;
            boolean changed = false;
            LogInfo logInfo = new LogInfo(dumpId);
            if (negativeOneOnce.add(v, logInfo) || zeroOnce.add(v, logInfo)
                    || oneOnce.add(v, logInfo) || restOnce.add(v, logInfo)) {
                changed = true;
            }
            return changed;
        }
        throw new RuntimeException(String.format("Not an %s but claimed to be", typeName));
    }

    @Override
    public boolean merge(TypeInfo other) {
        if (other instanceof DoubleType) {
            DoubleType otherType = (DoubleType) other;
            boolean changed = false;
            if (merge(otherType))
                changed = true;
            return changed;
        }
        throw new RuntimeException(String.format("Not an %s but claimed to be", typeName));
    }

}
