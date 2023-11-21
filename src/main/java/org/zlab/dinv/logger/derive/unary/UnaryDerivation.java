package org.zlab.dinv.logger.derive.unary;

import org.zlab.dinv.logger.derive.Derivation;
import org.zlab.dinv.logger.inv.VarInfo;

public class UnaryDerivation extends Derivation {
    static final long serialVersionUID = 20231120L;

    public VarInfo base;

    @Override
    public VarInfo getBase(int i) {
        return base;
    }
}
