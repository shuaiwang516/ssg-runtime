package org.zlab.ocov.tracker.inv.unary;

import org.zlab.ocov.tracker.inv.Invariant;

public abstract class UnaryInvariant extends Invariant {

    // For debug
    public int dumpId = -1;
    public String typeName;

    public UnaryInvariant() {
        typeName = this.getClass().getSimpleName();
    }

    /**
     * @param val
     * @param logInfo
     * @return true if the invariant is modified
     */
    public abstract boolean add(Object val, LogInfo logInfo);

    @Override
    public String toString() {
        return String.format("<%s> dumpId = %d", typeName, dumpId);
    }

}
