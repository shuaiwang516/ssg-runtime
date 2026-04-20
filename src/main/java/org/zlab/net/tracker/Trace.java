package org.zlab.net.tracker;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class Trace implements Serializable {
    private static final long serialVersionUID = 20260419L;
    public static final boolean debug = false;
    private static final int DIFF_SUMMARY_TOKEN_LIMIT = 12;
    private static final Pattern NUMBER_TOKEN_PATTERN = Pattern.compile("^-?\\d+(?:\\.\\d+)?$");
    private static final Pattern UUID_TOKEN_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final Pattern HEX_TOKEN_PATTERN = Pattern.compile("^[0-9a-fA-F]{8,}$");
    private static final Set<String> VOLATILE_SUMMARY_TOKENS = new HashSet<>(
            Arrays.asList("Message", "Header", "InetAddressAndPort", "byte[]", "AtomicReference",
                    "CachedSerialization", "Serialization[]", "HeapByteBuffer"));

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
        MessageFingerprint.Fingerprint fp = MessageFingerprint.fingerprint(message, contextArgs);
        String payloadType = fp.payloadType;
        if (payloadType == null && debug) {
            payloadType = getFirstPayloadType(contextArgs);
        }
        Set<String> types = extractTypes(message);
        long messageShapeHash = Utils.computeHash(types);
        long messageValueHash = fp.valueHash;

        SendMeta normalized = sendMeta != null ? sendMeta : SendMeta.builder().build();
        String messageKey = buildMessageKey(TraceEntry.EventType.SEND, name, id,
                normalized.messageType, normalized.messageVersion, messageShapeHash,
                messageValueHash);
        addEntry(name, id, TraceEntry.EventType.SEND, changedMessage, normalized.nodeId,
                normalized.peerId, normalized.nodeRole, normalized.peerRole, normalized.channel,
                normalized.protocol, normalized.messageType, normalized.messageVersion,
                normalized.rpcService, normalized.rpcMethod, normalized.messageKind,
                normalized.logicalMessageId, normalized.deliveryId, normalized.fanoutType,
                normalized.targetCount, messageShapeHash, messageValueHash, messageKey, fp.summary,
                false, beforeExecPath, null, payloadType);
    }

    public synchronized void recordReceiveBegin(String name, int id, int[] beforeExecPath,
            Object message, RecvMeta recvMeta, Object... contextArgs) {
        boolean changedMessage = examineChangedMessage(message)
                || examineChangedMessage(contextArgs);
        MessageFingerprint.Fingerprint fp = MessageFingerprint.fingerprint(message, contextArgs);
        String payloadType = fp.payloadType;
        if (payloadType == null && debug) {
            payloadType = getFirstPayloadType(contextArgs);
        }
        Set<String> types = extractTypes(message);
        long messageShapeHash = Utils.computeHash(types);
        long messageValueHash = fp.valueHash;

        RecvMeta normalized = recvMeta != null ? recvMeta : RecvMeta.builder().build();
        String messageKey = buildMessageKey(TraceEntry.EventType.RECV_BEGIN, name, id,
                normalized.messageType, normalized.messageVersion, messageShapeHash,
                messageValueHash);
        addEntry(name, id, TraceEntry.EventType.RECV_BEGIN, changedMessage, normalized.nodeId,
                normalized.peerId, normalized.nodeRole, normalized.peerRole, normalized.channel,
                normalized.protocol, normalized.messageType, normalized.messageVersion,
                normalized.rpcService, normalized.rpcMethod, normalized.messageKind,
                normalized.logicalMessageId, normalized.deliveryId, null, -1, messageShapeHash,
                messageValueHash, messageKey, fp.summary, false, beforeExecPath, null, payloadType);
    }

    public synchronized void recordReceiveEnd(String name, int id, int[] beforeExecPath,
            int[] afterExecPath, Object message, RecvMeta recvMeta, boolean timedOut,
            Object... contextArgs) {
        boolean changedMessage = examineChangedMessage(message)
                || examineChangedMessage(contextArgs);
        MessageFingerprint.Fingerprint fp = MessageFingerprint.fingerprint(message, contextArgs);
        String payloadType = fp.payloadType;
        if (payloadType == null && debug) {
            payloadType = getFirstPayloadType(contextArgs);
        }
        Set<String> types = extractTypes(message);
        long messageShapeHash = Utils.computeHash(types);
        long messageValueHash = fp.valueHash;

        RecvMeta normalized = recvMeta != null ? recvMeta : RecvMeta.builder().build();
        String messageKey = buildMessageKey(TraceEntry.EventType.RECV_END, name, id,
                normalized.messageType, normalized.messageVersion, messageShapeHash,
                messageValueHash);
        addEntry(name, id, TraceEntry.EventType.RECV_END, changedMessage, normalized.nodeId,
                normalized.peerId, normalized.nodeRole, normalized.peerRole, normalized.channel,
                normalized.protocol, normalized.messageType, normalized.messageVersion,
                normalized.rpcService, normalized.rpcMethod, normalized.messageKind,
                normalized.logicalMessageId, normalized.deliveryId, null, -1, messageShapeHash,
                messageValueHash, messageKey, fp.summary, timedOut, beforeExecPath, afterExecPath,
                payloadType);
    }

    private void addEntry(String name, int id, TraceEntry.EventType eventType,
            boolean changedMessage, String nodeId, String peerId, String nodeRole, String peerRole,
            String channel, String protocol, String messageType, String messageVersion,
            String rpcService, String rpcMethod, String messageKind, String logicalMessageId,
            String deliveryId, String fanoutType, int targetCount, long messageShapeHash,
            long messageValueHash, String messageKey, String messageSummary, boolean timedOut,
            int[] beforeExecPath, int[] afterExecPath, String payloadType) {
        long nowMillis = System.currentTimeMillis();
        long nowNanos = System.nanoTime();
        long beforeHash = Utils.computeHash(beforeExecPath);
        long afterHash = Utils.computeHash(afterExecPath);
        traceEntries.add(new TraceEntry(id, name, name.hashCode(), eventType, changedMessage,
                nowMillis, nowNanos, nodeId, peerId, nodeRole, peerRole, channel, protocol,
                messageType, messageVersion, rpcService, rpcMethod, messageKind, logicalMessageId,
                deliveryId, fanoutType, targetCount, messageShapeHash, messageValueHash, messageKey,
                messageSummary, timedOut, beforeHash, beforeExecPath, afterHash, afterExecPath,
                payloadType));
    }

    public boolean examineChangedMessage(Object... contextArgs) {
        if (contextArgs == null) {
            return false;
        }
        for (Object arg : contextArgs) {
            if (examineChangedMessage(arg)) {
                return true;
            }
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
            if (traverser.payloadType != null) {
                return traverser.payloadType;
            }
        }
        return null;
    }

    // Debug
    public static String getPayloadType(Object obj) {
        if (obj == null) {
            return null;
        }
        ObjectGraphTraverser traverser = new ObjectGraphTraverser();
        traverser.traverse(obj);
        return traverser.payloadType;
    }

    public boolean examineChangedMessage(Object message) {
        if (message == null || Runtime.changedClasses == null || Runtime.changedClasses.isEmpty()) {
            return false;
        }

        Set<String> types = extractTypes(message);
        for (String changedClass : Runtime.changedClasses) {
            if (types.contains(changedClass)) {
                return true;
            }
        }
        return false;
    }

    public static Set<String> extractTypes(Object message) {
        ObjectGraphTraverser traverser = new ObjectGraphTraverser();
        traverser.traverse(message);
        return traverser.getVisitedTypes();
    }

    /** Add a pre-constructed TraceEntry directly. */
    public synchronized void addEntry(TraceEntry entry) {
        traceEntries.add(entry);
    }

    // This is actually an append operation
    public synchronized void append(Trace trace) {
        if (trace == null) {
            return;
        }
        assert traceEntries.isEmpty() || trace.traceEntries.isEmpty()
                || compareEntry(traceEntries.get(traceEntries.size() - 1),
                        trace.traceEntries.get(0)) <= 0
                : "The trace to be appended is not in the correct order";
        traceEntries.addAll(trace.traceEntries);
    }

    public synchronized int size() {
        return traceEntries.size();
    }

    public synchronized List<TraceEntry> getTraceEntries() {
        return new LinkedList<>(traceEntries);
    }

    // --- Canonical key accessors ---

    /**
     * Returns multiset of canonical message keys at the default
     * {@link CanonicalKeyMode#GUIDANCE} tier (order-insensitive). Excludes RECV_END
     * to avoid double-counting.
     *
     * <p>
     * Prefer {@link #getCanonicalMultiset(CanonicalKeyMode)} in places where the
     * tier is selected from configuration.
     */
    public synchronized Map<String, Integer> getCanonicalMultiset() {
        return getCanonicalMultiset(CanonicalKeyMode.GUIDANCE);
    }

    /**
     * Returns multiset of canonical message keys at the requested
     * {@link CanonicalKeyMode} tier. Excludes RECV_END to avoid double-counting.
     */
    public synchronized Map<String, Integer> getCanonicalMultiset(CanonicalKeyMode mode) {
        CanonicalKeyMode resolved = mode != null ? mode : CanonicalKeyMode.GUIDANCE;
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (TraceEntry entry : traceEntries) {
            if (entry.eventType == TraceEntry.EventType.RECV_END)
                continue;
            String key = entry.canonicalMessageKey(resolved);
            counts.merge(key, 1, Integer::sum);
        }
        return counts;
    }

    /**
     * Returns ordered list of canonical message keys for tri-diff at the default
     * {@link CanonicalKeyMode#GUIDANCE} tier. Excludes RECV_END to avoid
     * double-counting.
     *
     * <p>
     * Prefer {@link #getCanonicalKeysForDiff(CanonicalKeyMode)} in places where the
     * tier is selected from configuration.
     */
    public synchronized List<String> getCanonicalKeysForDiff() {
        return getCanonicalKeysForDiff(CanonicalKeyMode.GUIDANCE);
    }

    /**
     * Returns ordered list of canonical message keys for tri-diff at the requested
     * {@link CanonicalKeyMode} tier. Excludes RECV_END to avoid double-counting.
     */
    public synchronized List<String> getCanonicalKeysForDiff(CanonicalKeyMode mode) {
        CanonicalKeyMode resolved = mode != null ? mode : CanonicalKeyMode.GUIDANCE;
        List<String> keys = new ArrayList<>();
        for (TraceEntry entry : traceEntries) {
            if (entry.eventType == TraceEntry.EventType.RECV_END)
                continue;
            keys.add(entry.canonicalMessageKey(resolved));
        }
        return keys;
    }

    /**
     * Returns multiset of raw semantic types only (no direction, no endpoint).
     * Retained as a diagnostic helper for offline analysis and tests — the
     * production scorer uses {@link #getCanonicalMultiset(CanonicalKeyMode)}.
     */
    public synchronized Map<String, Integer> getSemanticTypeMultiset() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (TraceEntry entry : traceEntries) {
            if (entry.eventType == TraceEntry.EventType.RECV_END)
                continue;
            counts.merge(entry.semanticType(), 1, Integer::sum);
        }
        return counts;
    }

    public synchronized void mergeBasedOnTimestamp(Trace otherTrace) {
        Trace mergedTrace = mergeBasedOnTimestamp(this, otherTrace);
        traceEntries.clear();
        traceEntries.addAll(mergedTrace.traceEntries);
    }

    public static Trace mergeBasedOnTimestamp(Trace trace0, Trace trace1) {
        Trace mergedTrace = new Trace();
        int i = 0;
        int j = 0;
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
            if (trace == null || trace.size() == 0) {
                continue;
            }
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

    /**
     * Legacy per-entry key populated onto every recorded
     * {@link TraceEntry#messageKey}. Retained so serialized traces from older runs
     * still deserialize cleanly; production scoring never reads it.
     */
    private static String buildMessageKey(TraceEntry.EventType eventType, String methodName, int id,
            String messageType, String messageVersion, long messageShapeHash,
            long messageValueHash) {
        return eventType + "|" + methodName + "#" + id + "|type=" + normalizeMeta(messageType)
                + "|ver=" + normalizeMeta(messageVersion) + "|shape="
                + Long.toHexString(messageShapeHash) + "|value="
                + Long.toHexString(messageValueHash);
    }

    private static String normalizeMeta(String value) {
        if (value == null || value.isEmpty()) {
            return "-";
        }
        return value;
    }

    private static Object firstArg(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        return args[0];
    }
}
