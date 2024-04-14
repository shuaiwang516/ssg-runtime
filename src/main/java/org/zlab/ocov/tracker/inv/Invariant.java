package org.zlab.ocov.tracker.inv;

import java.io.Serializable;

public abstract class Invariant implements Serializable {

    /**
     * @return true if the current invariant is modified
     */
    public abstract boolean merge(Invariant other);

    public abstract void reset();

}
