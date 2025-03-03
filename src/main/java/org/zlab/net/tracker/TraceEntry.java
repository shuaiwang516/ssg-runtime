package org.zlab.net.tracker;

import java.io.Serializable;

public class TraceEntry implements Serializable {
    public int id;
    public int hashcode;

    // Serialize messages across the cluster
    public long timestamp = System.currentTimeMillis();

    public TraceEntry(int id, int hashcode) {
        this.id = id;
        this.hashcode = hashcode;
    }
}
