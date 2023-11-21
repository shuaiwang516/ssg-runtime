package org.zlab.dinv.logger.inv.unary;

import org.zlab.dinv.logger.inv.Invariant;

public abstract class UnaryInvariant extends Invariant {

    static final long serialVersionUID = 20020122L;

    public abstract void add(Object val, int count);
    public abstract boolean check(Object val, int count);
}
