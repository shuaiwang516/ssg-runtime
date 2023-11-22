package org.zlab.dinv.logger.inv.unary;

import org.zlab.dinv.logger.SSGPointSlice;
import org.zlab.dinv.logger.inv.Invariant;
import org.zlab.dinv.logger.inv.VarInfo;

public class UpperBound extends UnaryInvariant {
    // var < x
    public Integer x = Integer.MIN_VALUE;

    public UpperBound() {
        super(null);
    }

    public UpperBound(SSGPointSlice ssgPointSlice) {
        super(ssgPointSlice);
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
    public Invariant instantiate(SSGPointSlice ssgPointSlice) {
        return new UpperBound(ssgPointSlice);
    }

    @Override
    public String formatString() {
        VarInfo var = var();
        String name = var.parentClassName + "." + var().name;
        return name + " < " + x;
    }

}
