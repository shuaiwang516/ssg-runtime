package org.zlab.dinv.logger.inv.binary;

import org.zlab.dinv.logger.inv.SSGPointSlice;
import org.zlab.dinv.logger.inv.Invariant;

public class BinaryInvariant extends Invariant {

    static final long serialVersionUID = 20020120L;

    public BinaryInvariant(SSGPointSlice ssgPpt) {
        super(ssgPpt);
    }

    @Override
    public Invariant instantiate(SSGPointSlice ssgPpt) {
        return null;
    }

    @Override
    public String formatString() {
        return null;
    }

}
