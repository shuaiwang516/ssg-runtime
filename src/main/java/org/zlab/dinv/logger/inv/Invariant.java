package org.zlab.dinv.logger.inv;

import org.zlab.dinv.logger.SSGPointSlice;

import java.io.Serializable;

public abstract class Invariant implements Serializable {

    static final long serialVersionUID = 20040921L;

    // Do update this value when implement add
    public boolean falsified = false;

    public abstract Invariant instantiate(SSGPointSlice ssgPpt);

    public boolean is_false() {
        return falsified;
    }

    public abstract String formatString();

    public SSGPointSlice ssgPpt;

    public Invariant(SSGPointSlice ssgPpt) {
        this.ssgPpt = ssgPpt;
    }

}
