package org.zlab.net.tracker;

import java.io.Serializable;

public class TraceEntry implements Serializable {
    private static final long serialVersionUID = 20250311L;

    public final int id;
    public final String methodName;
    public final int hashcode;

    public long recentExecPathHash; // Hash of the recent execution path, if needed for debugging
    // TODO: use more efficient representation (e.g. bitset, hashcodes, etc.)
    public int[] recentExecPath; // Recent execution path, if needed for debugging

    public final boolean changedMessage;

    // Serialize messages across the cluster
    public final long timestamp;

    public final String log;

    public TraceEntry(int id, String methodName, int hashcode, boolean changedMessage) {
        this(id, methodName, hashcode, changedMessage, System.currentTimeMillis(), -1, null, null);
    }

    public TraceEntry(int id, String methodName, int hashcode, boolean changedMessage,
            long timestamp, long recentExecPathHash, int[] recentExecPath, String log) {
        this.id = id;
        this.methodName = methodName;
        this.hashcode = hashcode;
        this.changedMessage = changedMessage;
        this.timestamp = timestamp;
        this.recentExecPathHash = recentExecPathHash;
        this.recentExecPath = recentExecPath;
        this.log = log;
    }

    // toString method
    @Override
    public String toString() {
        return "TraceEntry{" + "id=" + id + ", methodName='" + methodName + '\'' + ", hashcode="
                + hashcode + ", changedMessage=" + changedMessage + ", timestamp=" + timestamp
                + ", recentExecPathHash=" + recentExecPathHash + ", recentExecPath="
                + (recentExecPath != null ? java.util.Arrays.toString(recentExecPath) : "null")
                + ", log='" + log + '\'' + '}';
    }
}
