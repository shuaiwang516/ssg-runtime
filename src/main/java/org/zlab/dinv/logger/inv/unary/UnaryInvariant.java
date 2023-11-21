package org.zlab.dinv.logger.inv.unary;

import org.zlab.dinv.logger.SSGPointSlice;
import org.zlab.dinv.logger.inv.Invariant;
import org.zlab.dinv.logger.inv.VarInfo;

public abstract class UnaryInvariant extends Invariant {

    static final long serialVersionUID = 20020122L;

    public UnaryInvariant(SSGPointSlice ssgPpt) {
        super(ssgPpt);
    }

    public abstract void add(Object val, int count);
    public abstract boolean check(Object val, int count);

    public VarInfo var() {
        return ssgPpt.varInfo;
    }

}
