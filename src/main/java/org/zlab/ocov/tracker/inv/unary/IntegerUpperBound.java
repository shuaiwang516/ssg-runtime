package org.zlab.ocov.tracker.inv.unary;

import org.zlab.ocov.tracker.inv.Invariant;

public class IntegerUpperBound extends UnaryInvariant {
    private static final long serialVersionUID = 20231215L;

    int upperBound = Integer.MIN_VALUE;

    @Override
    public boolean merge(Invariant other) {
        IntegerUpperBound otherBound = (IntegerUpperBound) other;
        if (otherBound.upperBound > upperBound) {
            upperBound = otherBound.upperBound;
            dumpId = otherBound.dumpId;
            return true;
        }
        return false;
    }

    @Override
    public boolean add(Object val, LogInfo logInfo) {
        if (val instanceof Integer) {
            int tmpVal = (Integer) val;
            if (tmpVal > upperBound) {
                upperBound = tmpVal;
                dumpId = logInfo.dumpId;
                return true;
            }
        }
        return false;
    }
}
