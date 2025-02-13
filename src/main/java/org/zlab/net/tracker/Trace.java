package org.zlab.net.tracker;

import java.io.Serializable;
import java.util.List;

public class Trace implements Serializable {
    List<TraceEntry> traceEntries;

    public void record(String name, int id, String... contextArgs) {
        traceEntries.add(new TraceEntry(id));
        Runtime.log("Recorded trace entry: " + id);
    }
}
