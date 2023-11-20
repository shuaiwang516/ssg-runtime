package org.zlab.dinv.logger.inv.unary;

import org.zlab.dinv.logger.LogEntry;

public class UpperBound extends UnaryTemplate {
    // var < x
    LogEntry.VariableInfo var;
    public Integer x;

    public UpperBound(LogEntry.VariableInfo var) {
        this.var = var;
        this.x = Integer.MAX_VALUE;
    }

    @Override
    public boolean validate(Object val) {
        assert val instanceof Integer;
        Integer v = (Integer) val;
        return v < x;
    }

    @Override
    public void update(Object val) {
        assert val instanceof Integer;
        Integer v = (Integer) val;
        if (v > x) {
            x = v;
        }
    }

    @Override
    public boolean isValid() {
        return x != Integer.MAX_VALUE;
    }
}
