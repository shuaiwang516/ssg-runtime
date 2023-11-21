package org.zlab.dinv.logger.inv;

import java.io.Serializable;

public abstract class Invariant implements Serializable {

    static final long serialVersionUID = 20040921L;

    // Do update this value when implement add
    public boolean falsified = false;

    public abstract Invariant instantiate();

    public boolean is_false() {
        return falsified;
    }

}
