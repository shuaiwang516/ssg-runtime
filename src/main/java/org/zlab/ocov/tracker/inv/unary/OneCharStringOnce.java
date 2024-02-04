package org.zlab.ocov.tracker.inv.unary;

public class OneCharStringOnce extends OccurOnceInvariant {
    private static final long serialVersionUID = 20231215L;

    @Override
    public boolean checkPure(Object val, LogInfo logInfo) {
        if (val instanceof String) {
            String value = (String) val;
            if (value.length() == 1) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean add(Object val, LogInfo logInfo) {
        if (val instanceof String && !occurOnce) {
            String value = (String) val;
            if (value.length() == 1) {
                occurOnce = true;
                dumpId = logInfo.dumpId;
                return true;
            }
        }
        return false;
    }

}
