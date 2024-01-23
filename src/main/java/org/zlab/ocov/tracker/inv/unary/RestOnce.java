package org.zlab.ocov.tracker.inv.unary;

import java.util.Set;

public class RestOnce extends OccurOnceInvariant {
    private static final long serialVersionUID = 20231215L;

    Set<Number> targetValues;

    public RestOnce(Set<Number> targetValues) {
        super();
        this.targetValues = targetValues;
    }

    @Override
    public boolean add(Object val, LogInfo logInfo) {
        if (val instanceof Number && !occurOnce) {
            if (!isEqualToAny(val, targetValues)) {
                occurOnce = true;
                dumpId = logInfo.dumpId;
                return true;
            }
        }
        return false;
    }

    public static boolean isEqualToAny(Object o, Set<Number> numbers) {
        if (o instanceof Number) {
            double value = ((Number) o).doubleValue();
            for (Number number : numbers) {
                if (number.doubleValue() == value) {
                    return true;
                }
            }
        }
        return false;
    }

}
