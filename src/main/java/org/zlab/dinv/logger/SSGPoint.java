package org.zlab.dinv.logger;

import org.zlab.dinv.logger.inv.VarInfo;

import java.io.Serializable;

/**
 * An SSGPoint can only contain one type of varInfo, since this is object level.
 * A Ppt can contain multiple varInfos, since it is method level.
 *
 * Might need modification when binary invariants are added.
 */
public abstract class SSGPoint implements Serializable {
    static final long serialVersionUID = 20040914L;

    // public LogEntry.VariableInfo[] variableInfos;
    public VarInfo varInfo;
}
