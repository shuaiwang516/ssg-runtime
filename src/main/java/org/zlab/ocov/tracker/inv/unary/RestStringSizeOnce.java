package org.zlab.ocov.tracker.inv.unary;

import java.util.Set;

public class RestStringSizeOnce extends OccurOnceInvariant {
    private static final long serialVersionUID = 20231215L;

    Set<Integer> targetValues;

    public RestStringSizeOnce(Set<Integer> targetValues) {
        super();
        this.targetValues = targetValues;
    }

    @Override
    public boolean checkPure(Object val, LogInfo logInfo) {
        if (val instanceof String) {
            int length = ((String) val).length();
            if (!targetValues.contains(length)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean add(Object val, LogInfo logInfo) {
        if (val instanceof String && !occurOnce) {
            int length = ((String) val).length();
            if (!targetValues.contains(length)) {
                occurOnce = true;
                dumpId = logInfo.dumpId;
                return true;
            }
        }
        return false;
    }

}
