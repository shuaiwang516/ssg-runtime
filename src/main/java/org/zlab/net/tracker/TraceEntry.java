package org.zlab.net.tracker;

import java.io.Serializable;

public class TraceEntry implements Serializable {
    public int id;
    public String methodName;
    public int hashcode;

    // Serialize messages across the cluster
    public long timestamp;

    public TraceEntry(int id, String methodName, int hashcode) {
        this.id = id;
        this.methodName = methodName;
        this.hashcode = hashcode;
        this.timestamp = System.currentTimeMillis();
    }

    public TraceEntry(int id, String methodName, int hashcode, long timestamp) {
        this.id = id;
        this.methodName = methodName;
        this.hashcode = hashcode;
        this.timestamp = timestamp;
    }
}
