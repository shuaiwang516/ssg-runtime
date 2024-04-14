package org.zlab.ocov.tracker.inv.unary;

import org.zlab.ocov.tracker.inv.Invariant;

public abstract class OccurOnceInvariant extends UnaryInvariant {

    public boolean occurOnce = false;

    @Override
    public boolean merge(Invariant other) {
        OccurOnceInvariant OccurOnceInvariant = (OccurOnceInvariant) other;
        if (!occurOnce && OccurOnceInvariant.occurOnce) {
            occurOnce = true;
            dumpId = OccurOnceInvariant.dumpId;
            return true;
        }
        return false;
    }

    @Override
    public void reset() {
        occurOnce = false;
    }
}
