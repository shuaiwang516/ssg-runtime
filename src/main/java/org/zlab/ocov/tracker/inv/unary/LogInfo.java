package org.zlab.ocov.tracker.inv.unary;

import java.util.Map;
import java.util.Set;

public class LogInfo {
    public int dumpId;
    public int contextHashCode;
    public Map<String, Map<String, String>> matchableClassInfo = null;
    public Set<String> changedClasses = null;
    public Set<String> visitedChangedClasses = null;

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

    public LogInfo(int dumpId, int contextHashCode,
            Map<String, Map<String, String>> matchableClassInfo, Set<String> changedClasses,
            Set<String> visitedChangedClasses) {
        this.dumpId = dumpId;
        this.contextHashCode = contextHashCode;
        this.matchableClassInfo = matchableClassInfo;
        this.changedClasses = changedClasses;
        this.visitedChangedClasses = visitedChangedClasses;
    }
}
