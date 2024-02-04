package org.zlab.ocov.tracker.inv.unary;

import org.zlab.ocov.tracker.inv.Invariant;

public class LongLowerBound extends UnaryInvariant {
    private static final long serialVersionUID = 20231215L;

    long lowerBound = Long.MAX_VALUE;

    @Override
    public boolean merge(Invariant other) {
        LongLowerBound otherBound = (LongLowerBound) other;
        if (otherBound.lowerBound < lowerBound) {
            lowerBound = otherBound.lowerBound;
            dumpId = otherBound.dumpId;
            return true;
        }
        return false;
    }

    @Override
    public boolean checkPure(Object val, LogInfo logInfo) {
        if (val instanceof Long) {
            long tmpVal = (Long) val;
            if (tmpVal < lowerBound) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean add(Object val, LogInfo logInfo) {
        if (val instanceof Long) {
            long tmpVal = (Long) val;
            if (tmpVal < lowerBound) {
                lowerBound = tmpVal;
                dumpId = logInfo.dumpId;
                return true;
            }
        }
        return false;
    }
}
