package org.zlab.net.tracker;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Trace implements Serializable {
    private static final long serialVersionUID = 20260208L;
    public static final boolean debug = false;

    private final List<TraceEntry> traceEntries = new LinkedList<>();

    public synchronized Trace copy() {
        Trace copied = new Trace();
        for (TraceEntry entry : traceEntries) {
            copied.traceEntries.add(entry.copy());
        }
        return copied;
    }

    // id: unique identifier for the instrumented location
    public synchronized void record(String name, int id) {
        record(name, id, null);
    }

    // Legacy API: treated as send event with pre-branch context only.
    public synchronized void record(String name, int id, int[] recentExecPath,
            Object... contextArgs) {
        Object message = firstArg(contextArgs);
        recordSend(name, id, recentExecPath, message, null, contextArgs);
    }

    public synchronized void recordSend(String name, int id, int[] beforeExecPath, Object message,
            SendMeta sendMeta, Object... contextArgs) {
        boolean changedMessage = examineChangedMessage(message)
                || examineChangedMessage(contextArgs);
        String payloadType = getPayloadType(message);
        if (payloadType == null && debug) {
            payloadType = getFirstPayloadType(contextArgs);
        }
        Set<String> types = extractTypes(message);
        long messageShapeHash = Utils.computeHash(types);

        SendMeta normalized = sendMeta != null ? sendMeta : SendMeta.builder().build();
        addEntry(name, id, TraceEntry.EventType.SEND, changedMessage, normalized.nodeId,
                normalized.peerId, normalized.channel, normalized.protocol, normalized.messageType,
                normalized.messageVersion, normalized.logicalMessageId, normalized.deliveryId,
                normalized.fanoutType, normalized.targetCount, messageShapeHash, false,
                beforeExecPath, null, payloadType);
    }

    public synchronized void recordReceiveBegin(String name, int id, int[] beforeExecPath,
            Object message, RecvMeta recvMeta, Object... contextArgs) {
        boolean changedMessage = examineChangedMessage(message)
                || examineChangedMessage(contextArgs);
        String payloadType = getPayloadType(message);
        if (payloadType == null && debug) {
            payloadType = getFirstPayloadType(contextArgs);
        }
        Set<String> types = extractTypes(message);
        long messageShapeHash = Utils.computeHash(types);

        RecvMeta normalized = recvMeta != null ? recvMeta : RecvMeta.builder().build();
        addEntry(name, id, TraceEntry.EventType.RECV_BEGIN, changedMessage, normalized.nodeId,
                normalized.peerId, normalized.channel, normalized.protocol, normalized.messageType,
                normalized.messageVersion, normalized.logicalMessageId, normalized.deliveryId, null,
                -1, messageShapeHash, false, beforeExecPath, null, payloadType);
    }

    public synchronized void recordReceiveEnd(String name, int id, int[] beforeExecPath,
            int[] afterExecPath, Object message, RecvMeta recvMeta, boolean timedOut,
            Object... contextArgs) {
        boolean changedMessage = examineChangedMessage(message)
                || examineChangedMessage(contextArgs);
        String payloadType = getPayloadType(message);
        if (payloadType == null && debug) {
            payloadType = getFirstPayloadType(contextArgs);
        }
        Set<String> types = extractTypes(message);
        long messageShapeHash = Utils.computeHash(types);

        RecvMeta normalized = recvMeta != null ? recvMeta : RecvMeta.builder().build();
        addEntry(name, id, TraceEntry.EventType.RECV_END, changedMessage, normalized.nodeId,
                normalized.peerId, normalized.channel, normalized.protocol, normalized.messageType,
                normalized.messageVersion, normalized.logicalMessageId, normalized.deliveryId, null,
                -1, messageShapeHash, timedOut, beforeExecPath, afterExecPath, payloadType);
    }

    private void addEntry(String name, int id, TraceEntry.EventType eventType,
            boolean changedMessage, String nodeId, String peerId, String channel, String protocol,
            String messageType, String messageVersion, String logicalMessageId, String deliveryId,
            String fanoutType, int targetCount, long messageShapeHash, boolean timedOut,
            int[] beforeExecPath, int[] afterExecPath, String payloadType) {
        long nowMillis = System.currentTimeMillis();
        long nowNanos = System.nanoTime();
        long beforeHash = Utils.computeHash(beforeExecPath);
        long afterHash = Utils.computeHash(afterExecPath);
        traceEntries.add(new TraceEntry(id, name, name.hashCode(), eventType, changedMessage,
                nowMillis, nowNanos, nodeId, peerId, channel, protocol, messageType, messageVersion,
                logicalMessageId, deliveryId, fanoutType, targetCount, messageShapeHash, timedOut,
                beforeHash, beforeExecPath, afterHash, afterExecPath, payloadType));
    }

    public boolean examineChangedMessage(Object... contextArgs) {
        if (contextArgs == null) {
            return false;
        }
        for (Object arg : contextArgs) {
            if (examineChangedMessage(arg))
                return true;
        }
        return false;
    }

    // Debug
    public static String getFirstPayloadType(Object[] objects) {
        if (objects == null) {
            return null;
        }
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
        if (obj == null)
            return null;
        ObjectGraphTraverser traverser = new ObjectGraphTraverser();
        traverser.traverse(obj);
        return traverser.payloadType;
    }

    public boolean examineChangedMessage(Object message) {
        if (message == null || Runtime.changedClasses == null || Runtime.changedClasses.isEmpty()) {
            return false;
        }

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
    public synchronized void append(Trace trace) {
        if (trace == null)
            return;
        assert traceEntries.isEmpty() || trace.traceEntries.isEmpty()
                || compareEntry(traceEntries.get(traceEntries.size() - 1),
                        trace.traceEntries.get(0)) <= 0
                : "The trace to be appended is not in the correct order";
        traceEntries.addAll(trace.traceEntries);
    }

    public synchronized int size() {
        return traceEntries.size();
    }

    public synchronized List<String> getHashCodes() {
        List<String> hashCodes = new LinkedList<>();
        for (TraceEntry entry : traceEntries) {
            // Combine verb name hash with execution path hash
            // Same verb with different execution paths -> different hashcodes -> different
            // 2-grams.
            hashCodes.add(entry.hashcode + "_" + entry.recentExecPathHash);
        }
        return hashCodes;
    }

    public synchronized List<TraceEntry> getTraceEntries() {
        return new LinkedList<>(traceEntries);
    }

    public synchronized void mergeBasedOnTimestamp(Trace otherTrace) {
        Trace mergedTrace = mergeBasedOnTimestamp(this, otherTrace);
        traceEntries.clear();
        traceEntries.addAll(mergedTrace.traceEntries);
    }

    public static Trace mergeBasedOnTimestamp(Trace trace0, Trace trace1) {
        Trace mergedTrace = new Trace();
        int i = 0, j = 0;
        List<TraceEntry> left = trace0.getTraceEntries();
        List<TraceEntry> right = trace1.getTraceEntries();
        while (i < left.size() && j < right.size()) {
            TraceEntry entry0 = left.get(i);
            TraceEntry entry1 = right.get(j);
            if (compareEntry(entry0, entry1) <= 0) {
                mergedTrace.traceEntries.add(entry0);
                i++;
            } else {
                mergedTrace.traceEntries.add(entry1);
                j++;
            }
        }
        while (i < left.size()) {
            mergedTrace.traceEntries.add(left.get(i));
            i++;
        }
        while (j < right.size()) {
            mergedTrace.traceEntries.add(right.get(j));
            j++;
        }
        return mergedTrace;
    }

    public static Trace mergeBasedOnTimestamp(Trace[] traces) {
        Trace mergedTrace = new Trace();
        for (Trace trace : traces) {
            if (trace == null || trace.size() == 0)
                continue;
            mergedTrace.mergeBasedOnTimestamp(trace);
        }
        return mergedTrace;
    }

    public synchronized void print() {
        for (TraceEntry entry : traceEntries) {
            System.out.println(entry);
        }
    }

    private static int compareEntry(TraceEntry left, TraceEntry right) {
        if (left.timestamp != right.timestamp) {
            return left.timestamp < right.timestamp ? -1 : 1;
        }
        if (left.timestampNanos != right.timestampNanos) {
            return left.timestampNanos < right.timestampNanos ? -1 : 1;
        }
        if (left.id != right.id) {
            return left.id < right.id ? -1 : 1;
        }
        return left.hashcode < right.hashcode ? -1 : left.hashcode == right.hashcode ? 0 : 1;
    }

    private static Object firstArg(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        return args[0];
    }
}
