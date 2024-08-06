package org.zlab.ocov.tracker.inv.unary;

public class NegativeOneOnce extends OccurOnceInvariant {
    private static final long serialVersionUID = 20231215L;

    @Override
    public boolean checkPure(Object val, LogInfo logInfo) {
        // TODO: Add this check to all invariants
        if (!(val instanceof Number)) {
            // assert false : "val is not a number";
            return false;
        }
        Number number = (Number) val;
        return number.doubleValue() == -1.0;
    }

    @Override
    public boolean add(Object val, LogInfo logInfo) {
        if (!(val instanceof Number)) {
            // assert false : "val is not a number";
            return false;
        }
        if (!occurOnce) {
            Number number = (Number) val;
            if (number.doubleValue() == -1.0) {
                occurOnce = true;
                dumpId = logInfo.dumpId;
                return true;
            }
        }
        return false;
    }

}
