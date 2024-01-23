package org.zlab.ocov.tracker.inv.unary;

import org.zlab.ocov.tracker.inv.Invariant;

public abstract class UnaryInvariant extends Invariant {

    // For debug
    public int dumpId = -1;

    /**
     * @param val
     * @param logInfo
     * @return true if the invariant is modified
     */
    public abstract boolean add(Object val, LogInfo logInfo);

}
