package org.zlab.ocov.tracker.inv.unary;

import java.util.Set;

public class PreservedStringOnce extends OccurOnceInvariant {
    private static final long serialVersionUID = 20231215L;

    private final Set<String> preservedStrings;

    public PreservedStringOnce(Set<String> preservedStrings) {
        super();
        this.preservedStrings = preservedStrings;
    }

    @Override
    public boolean checkPure(Object val, LogInfo logInfo) {
        if (val instanceof String) {
            String value = (String) val;
            if (preservedStrings.contains(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean add(Object val, LogInfo logInfo) {
        if (val instanceof String && !occurOnce) {
            String value = (String) val;
            if (preservedStrings.contains(value)) {
                occurOnce = true;
                dumpId = logInfo.dumpId;
                return true;
            }
        }
        return false;
    }
}
