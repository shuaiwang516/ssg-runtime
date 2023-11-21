package org.zlab.dinv.logger.derive;

import org.zlab.dinv.logger.inv.VarInfo;

import java.io.Serializable;

public abstract class Derivation implements Serializable {
    static final long serialVersionUID = 20231120L;
    public abstract VarInfo getBase(int i);
}
