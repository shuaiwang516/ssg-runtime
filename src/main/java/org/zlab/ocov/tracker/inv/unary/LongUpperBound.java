package org.zlab.ocov.tracker.inv.unary;

import org.zlab.ocov.tracker.inv.Invariant;

public class LongUpperBound extends UnaryInvariant {
    private static final long serialVersionUID = 20231215L;

    long upperBound = Long.MIN_VALUE;

    @Override
    public boolean merge(Invariant other) {
        LongUpperBound otherBound = (LongUpperBound) other;
        if (otherBound.upperBound > upperBound) {
            upperBound = otherBound.upperBound;
            dumpId = otherBound.dumpId;
            return true;
        }
        return false;
    }

    @Override
    public void reset() {
        upperBound = Long.MIN_VALUE;
    }

    @Override
    public boolean checkPure(Object val, LogInfo logInfo) {
        if (val instanceof Long) {
            long tmpVal = (Long) val;
            if (tmpVal > upperBound) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean add(Object val, LogInfo logInfo) {
        if (val instanceof Long) {
            long tmpVal = (Long) val;
            if (tmpVal > upperBound) {
                upperBound = tmpVal;
                dumpId = logInfo.dumpId;
                return true;
            }
        }
        return false;
    }
}
