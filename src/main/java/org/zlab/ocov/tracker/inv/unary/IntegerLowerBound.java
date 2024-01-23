package org.zlab.ocov.tracker.inv.unary;

import org.zlab.ocov.tracker.inv.Invariant;

public class IntegerLowerBound extends UnaryInvariant {
    private static final long serialVersionUID = 20231215L;

    int lowerBound = Integer.MAX_VALUE;

    @Override
    public boolean merge(Invariant other) {
        IntegerLowerBound otherBound = (IntegerLowerBound) other;
        if (otherBound.lowerBound < lowerBound) {
            lowerBound = otherBound.lowerBound;
            dumpId = otherBound.dumpId;
            return true;
        }
        return false;
    }

    @Override
    public boolean add(Object val, LogInfo logInfo) {
        if (val instanceof Integer) {
            int tmpVal = (Integer) val;
            if (tmpVal < lowerBound) {
                lowerBound = tmpVal;
                dumpId = logInfo.dumpId;
                return true;
            }
        }
        return false;
    }
}
