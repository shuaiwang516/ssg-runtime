package org.zlab.net.tracker;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class Trace implements Serializable {
    private final List<TraceEntry> traceEntries = new LinkedList<>();

    public void record(String name, int id, Object... contextArgs) {
        // TODO: record contents
        traceEntries.add(new TraceEntry(id, name, name.hashCode()));
        Runtime.log("Recorded trace entry: " + id);
    }

    // This is actually an append operation
    public void merge(Trace trace) {
        if (trace == null)
            return;
        traceEntries.addAll(trace.traceEntries);
    }

    public int size() {
        return traceEntries.size();
    }

    public List<String> getHashCodes() {
        List<String> hashCodes = new LinkedList<>();
        for (TraceEntry entry : traceEntries) {
            hashCodes.add(String.valueOf(entry.hashcode));
        }
        return hashCodes;
    }

    public List<TraceEntry> getTraceEntries() {
        return traceEntries;
    }

    public static Trace mergeBasedOnTimestamp(Trace trace0, Trace trace1) {
        Trace mergedTrace = new Trace();
        int i = 0, j = 0;
        while (i < trace0.size() && j < trace1.size()) {
            TraceEntry entry0 = trace0.traceEntries.get(i);
            TraceEntry entry1 = trace1.traceEntries.get(j);
            if (entry0.timestamp < entry1.timestamp) {
                mergedTrace.traceEntries.add(entry0);
                i++;
            } else {
                mergedTrace.traceEntries.add(entry1);
                j++;
            }
        }
        while (i < trace0.size()) {
            mergedTrace.traceEntries.add(trace0.traceEntries.get(i));
            i++;
        }
        while (j < trace1.size()) {
            mergedTrace.traceEntries.add(trace1.traceEntries.get(j));
            j++;
        }
        return mergedTrace;
    }
}
