package org.zlab.ocov.tracker.inv.unary;

public class NullOnce extends OccurOnceInvariant {
    private static final long serialVersionUID = 20231215L;

    @Override
    public boolean checkPure(Object val, LogInfo logInfo) {
        if (val == null) {
            return true;
        }
        return false;
    }

    @Override
    public boolean add(Object val, LogInfo logInfo) {
        if (val == null && !occurOnce) {
            occurOnce = true;
            dumpId = logInfo.dumpId;
            return true;
        }
        return false;
    }

}
