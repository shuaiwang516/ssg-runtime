package org.zlab.dinv.logger.inv.unary;

import org.zlab.dinv.logger.inv.Invariant;

public class UpperBound extends UnaryInvariant {
    // var < x
    public Integer x = Integer.MAX_VALUE;

    public UpperBound() {
    }

    @Override
    public void add(Object val, int count) {
        assert val instanceof Integer;
        Integer v = (Integer) val;
        if (v > x) {
            x = v;
        }
    }

    @Override
    public boolean check(Object val, int count) {
        assert val instanceof Integer;
        Integer v = (Integer) val;
        return v < x;
    }

    @Override
    public Invariant instantiate() {
        return new UpperBound();
    }
}
