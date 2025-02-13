package org.zlab.net.tracker;

import java.io.Serializable;

public class TraceEntry implements Serializable {
    int id;

    public TraceEntry(int id) {
        this.id = id;
    }
}
