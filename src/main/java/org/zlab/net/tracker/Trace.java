package org.zlab.net.tracker;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Trace implements Serializable {
    private static final long serialVersionUID = 20250311L;
    public static final boolean debug = true;

    private final List<TraceEntry> traceEntries = new LinkedList<>();

    // id: unique identifier for the instrumented location
    public void record(String name, int id, Object... contextArgs) {
        // TODO: record contents
        // Iterate the args, (1) identify messages with special type (2) special content
        boolean changedMessage = examineChangedMessage(contextArgs);

        String payloadType = null;
        if (debug)
            payloadType = getFirstPayloadType(contextArgs);

        traceEntries.add(new TraceEntry(id, name, name.hashCode(), changedMessage));

        if (debug && contextArgs.length > 0 && contextArgs[0] != null) {
            Runtime.log("First entry type = " + contextArgs[0].getClass().getName());
        }

        Runtime.log("Recorded trace entry: " + id + ", payloadType: " + payloadType);
    }

    public boolean examineChangedMessage(Object... contextArgs) {
        for (Object arg : contextArgs) {
            if (examineChangedMessage(arg))
                return true;
        }
        return false;
    }

    // Debug
    public static String getFirstPayloadType(Object[] objects) {
        for (Object obj : objects) {
            ObjectGraphTraverser traverser = new ObjectGraphTraverser();
            traverser.traverse(obj);
            if (traverser.payloadType != null)
                return traverser.payloadType;
        }
        return null;
    }

    // Debug
    public static String getPayloadType(Object obj) {
        // if the object has a field name called "payload", return the type of the
        // payload
        if (obj == null)
            return null;
        try {
            Class<?> clazz = obj.getClass();
            // get the reference to the payload field
            java.lang.reflect.Field field = clazz.getDeclaredField("payload");
            field.setAccessible(true);
            Object payload = field.get(obj);
            if (payload == null)
                return null;
            return payload.getClass().getName();
        } catch (Exception e) {
            return null;
        }
    }

    public boolean examineChangedMessage(Object message) {
        if (Runtime.changedClasses == null || Runtime.changedClasses.isEmpty())
            return false;

        Set<String> types = extractTypes(message);
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
