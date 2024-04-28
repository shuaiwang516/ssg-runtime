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
     * Do not care about whether the invariant is modified, just check without
     * considering the state.
     *
     * If o1 breaks it, our inv mark is as broken and won't record if o2 breaks it.
     *
     * checkPure will already mark it broken from a clean state. FIXME: this is not
     * checking from a clean state, but from the current state.
     */
    public abstract boolean checkPure(Object val, LogInfo logInfo);

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
