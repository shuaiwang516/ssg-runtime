package org.zlab.ocov.tracker.inv.unary;

import java.util.Map;

public class LogInfo {
    public int dumpId;
    public int contextHashCode;
    public Map<String, Map<String, String>> matchableClassInfo = null;

    public LogInfo(int dumpId, int contextHashCode) {
        this.dumpId = dumpId;
        this.contextHashCode = contextHashCode;
    }

    public LogInfo(int dumpId, int contextHashCode,
            Map<String, Map<String, String>> matchableClassInfo) {
        this.dumpId = dumpId;
        this.contextHashCode = contextHashCode;
        this.matchableClassInfo = matchableClassInfo;
    }
}
