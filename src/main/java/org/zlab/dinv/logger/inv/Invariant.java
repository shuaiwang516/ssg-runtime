package org.zlab.dinv.logger.inv;

import org.zlab.dinv.logger.Node;

import java.io.Serializable;

public abstract class Invariant implements Serializable {

    static final long serialVersionUID = 20040921L;

    public abstract void process(Node node);

}
