package org.zlab.net.tracker;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Trace implements Serializable {
    private final List<TraceEntry> traceEntries = new LinkedList<>();

    // id: unique identifier for the instrumented location
    public void record(String name, int id, Object... contextArgs) {
        // TODO: record contents
        // Iterate the args, (1) identify messages with special type (2) special content
        boolean changedMessage = examineChangedMessage(contextArgs);
        traceEntries.add(new TraceEntry(id, name, name.hashCode(), changedMessage));
        Runtime.log("Recorded trace entry: " + id);
    }

    public boolean examineChangedMessage(Object... contextArgs) {
        for (Object arg : contextArgs) {
            if (examineChangedMessage(arg))
                return true;
        }
        return false;
    }

    public boolean examineChangedMessage(Object message) {
        if (Runtime.changedClasses == null)
            return false;

        Set<String> types = extractTypes(message);
        // TODO: Iterate the object call graph, collect all types (custom)

        for (String changedClass : Runtime.changedClasses)
            if (types.contains(changedClass))
                return true;

        return false;
    }

    public static Set<String> extractTypes(Object message) {
        ObjectGraphTraverser traverser = new ObjectGraphTraverser();
        traverser.traverse(message);
        return traverser.getVisitedTypes();
    }

    // This is actually an append operation
    public void append(Trace trace) {
        if (trace == null)
            return;
        assert traceEntries.isEmpty() || traceEntries
                .get(traceEntries.size() - 1).timestamp < trace.traceEntries.get(0).timestamp
                : "The trace to be appended is not in the correct order";
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

    public void mergeBasedOnTimestamp(Trace otherTrace) {
        Trace mergedTrace = mergeBasedOnTimestamp(this, otherTrace);
        traceEntries.clear();
        traceEntries.addAll(mergedTrace.traceEntries);
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

    public static Trace mergeBasedOnTimestamp(Trace[] traces) {
        Trace mergedTrace = new Trace();
        for (Trace trace : traces) {
            // skip if the trace is null or empty
            if (trace == null || trace.size() == 0)
                continue;
            mergedTrace.mergeBasedOnTimestamp(trace);
        }
        return mergedTrace;
    }

    public void print() {
        for (TraceEntry entry : traceEntries) {
            System.out.println(entry);
        }
    }
}
