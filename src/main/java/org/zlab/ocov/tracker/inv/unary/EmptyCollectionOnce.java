package org.zlab.ocov.tracker.inv.unary;

public class EmptyCollectionOnce extends OccurOnceInvariant {
    private static final long serialVersionUID = 20231215L;

    @Override
    public boolean add(Object val, LogInfo logInfo) {
        if (val instanceof Number && !occurOnce) {
            Number number = (Number) val;
            if (number.doubleValue() == 0.0) {
                occurOnce = true;
                dumpId = logInfo.dumpId;
                return true;
            }
        }
        return false;
    }

}
