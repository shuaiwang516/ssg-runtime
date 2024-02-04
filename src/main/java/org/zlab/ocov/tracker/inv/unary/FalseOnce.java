package org.zlab.ocov.tracker.inv.unary;

public class FalseOnce extends OccurOnceInvariant {
    private static final long serialVersionUID = 20231215L;

    @Override
    public boolean checkPure(Object val, LogInfo logInfo) {
        if (val instanceof Boolean) {
            Boolean bool = (Boolean) val;
            if (!bool) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean add(Object val, LogInfo logInfo) {
        if (val instanceof Boolean && !occurOnce) {
            Boolean bool = (Boolean) val;
            if (!bool) {
                occurOnce = true;
                dumpId = logInfo.dumpId;
                return true;
            }
        }
        return false;
    }

}
