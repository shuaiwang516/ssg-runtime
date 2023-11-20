package org.zlab.dinv.logger.inv.unary;

import org.zlab.dinv.logger.LogEntry;

public class UpperBound extends UnaryInvariant {
    // var < x
    LogEntry.VariableInfo var;
    public Integer x;

    public UpperBound(LogEntry.VariableInfo var) {
        this.var = var;
        this.x = Integer.MAX_VALUE;
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
}
