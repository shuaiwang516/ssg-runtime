package org.zlab.dinv.logger;

import java.io.Serializable;

/**
 * What's the difference between a SSGPoint and a Ppt? It might involve the
 * structure information...
 */
public abstract class SSGPoint implements Serializable {
    static final long serialVersionUID = 20040914L;

    // public LogEntry.VariableInfo[] variableInfos;
    public String rootClassName;

    public String fieldName;
    public String className;
}
