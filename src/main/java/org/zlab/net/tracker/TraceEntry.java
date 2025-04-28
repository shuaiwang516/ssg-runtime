package org.zlab.net.tracker;

import java.io.Serializable;

public class TraceEntry implements Serializable {
    private static final long serialVersionUID = 20250311L;

    public final int id;
    public final String methodName;
    public final int hashcode;

    // Serialize messages across the cluster
    public final long timestamp;

    public final boolean changedMessage;

    public final String log;

    public TraceEntry(int id, String methodName, int hashcode, boolean changedMessage) {
        this(id, methodName, hashcode, changedMessage, System.currentTimeMillis(), null);
    }

    public TraceEntry(int id, String methodName, int hashcode, boolean changedMessage,
            long timestamp, String log) {
        this.id = id;
        this.methodName = methodName;
        this.hashcode = hashcode;
        this.changedMessage = changedMessage;
        this.timestamp = timestamp;
        this.log = log;
    }

    // toString method
    @Override
    public String toString() {
        return "TraceEntry{" + "id=" + id + ", methodName='" + methodName + '\'' + ", hashcode="
                + hashcode + ", timestamp=" + timestamp + ", changedMessage=" + changedMessage
                + ", log='" + log + '\'' + '}';
    }
}
