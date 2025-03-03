package org.zlab.net.tracker;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class Trace implements Serializable {
    private final List<TraceEntry> traceEntries = new LinkedList<>();

    public void record(String name, int id, Object... contextArgs) {
        // TODO: record contents
        traceEntries.add(new TraceEntry(id, name.hashCode()));
        Runtime.log("Recorded trace entry: " + id);
    }

    public void merge(Trace trace) {
        if (trace == null)
            return;
        traceEntries.addAll(trace.traceEntries);
    }

    public int size() {
        return traceEntries.size();
    }
}
