package org.zlab.net.tracker;

import java.io.Serializable;
import java.util.Arrays;

public class TraceEntry implements Serializable {
    private static final long serialVersionUID = 20260407L;

    public enum EventType {
        SEND, RECV_BEGIN, RECV_END, UNKNOWN
    }

    public final int id;
    public final String methodName;
    public final int hashcode;
    public final EventType eventType;

    public final boolean changedMessage;
    public final long timestamp;
    public final long timestampNanos;

    public final String nodeId;
    public final String peerId;
    public final String nodeRole;
    public final String peerRole;
    public final String channel;
    public final String protocol;
    public final String messageType;
    public final String messageVersion;
    public final String logicalMessageId;
    public final String deliveryId;
    public final String fanoutType;
    public final int targetCount;

    public final long messageShapeHash;
    public final long messageValueHash;
    public final String messageKey;
    public final String messageSummary;
    public final boolean timedOut;

    public final long beforeExecPathHash;
    public final int[] beforeExecPath;
    public final long afterExecPathHash;
    public final int[] afterExecPath;

    // Legacy aliases kept for compatibility with previous diff/analysis code.
    public final long recentExecPathHash;
    public final int[] recentExecPath;
    public final String log;

    public TraceEntry(int id, String methodName, int hashcode, boolean changedMessage) {
        this(id, methodName, hashcode, EventType.UNKNOWN, changedMessage,
                System.currentTimeMillis(), System.nanoTime(), null, null, null, null, null, null,
                null, null, null, null, null, -1, -1, -1, null, null, false, -1, null, -1, null,
                null);
    }

    public TraceEntry(int id, String methodName, int hashcode, EventType eventType,
            boolean changedMessage, long timestamp, long timestampNanos, String nodeId,
            String peerId, String nodeRole, String peerRole, String channel, String protocol,
            String messageType, String messageVersion, String logicalMessageId, String deliveryId,
            String fanoutType, int targetCount, long messageShapeHash, long messageValueHash,
            String messageKey, String messageSummary, boolean timedOut, long beforeExecPathHash,
            int[] beforeExecPath, long afterExecPathHash, int[] afterExecPath, String payloadType) {
        this.id = id;
        this.methodName = methodName;
        this.hashcode = hashcode;
        this.eventType = eventType == null ? EventType.UNKNOWN : eventType;
        this.changedMessage = changedMessage;
        this.timestamp = timestamp;
        this.timestampNanos = timestampNanos;
        this.nodeId = nodeId;
        this.peerId = peerId;
        this.nodeRole = nodeRole;
        this.peerRole = peerRole;
        this.channel = channel;
        this.protocol = protocol;
        this.messageType = messageType;
        this.messageVersion = messageVersion;
        this.logicalMessageId = logicalMessageId;
        this.deliveryId = deliveryId;
        this.fanoutType = fanoutType;
        this.targetCount = targetCount;
        this.messageShapeHash = messageShapeHash;
        this.messageValueHash = messageValueHash;
        this.messageKey = messageKey;
        this.messageSummary = messageSummary;
        this.timedOut = timedOut;
        this.beforeExecPathHash = beforeExecPathHash;
        this.beforeExecPath = copy(beforeExecPath);
        this.afterExecPathHash = afterExecPathHash;
        this.afterExecPath = copy(afterExecPath);

        this.recentExecPathHash = this.beforeExecPathHash;
        this.recentExecPath = this.beforeExecPath;
        this.log = payloadType;
    }

    public TraceEntry copy() {
        return new TraceEntry(id, methodName, hashcode, eventType, changedMessage, timestamp,
                timestampNanos, nodeId, peerId, nodeRole, peerRole, channel, protocol, messageType,
                messageVersion, logicalMessageId, deliveryId, fanoutType, targetCount,
                messageShapeHash, messageValueHash, messageKey, messageSummary, timedOut,
                beforeExecPathHash, beforeExecPath, afterExecPathHash, afterExecPath, log);
    }

    private static int[] copy(int[] values) {
        if (values == null) {
            return null;
        }
        return Arrays.copyOf(values, values.length);
    }

    @Override
    public String toString() {
        return "TraceEntry{" + "id=" + id + ", methodName='" + methodName + '\'' + ", eventType="
                + eventType + ", hashcode=" + hashcode + ", changedMessage=" + changedMessage
                + ", timestamp=" + timestamp + ", nodeId='" + nodeId + '\'' + ", peerId='" + peerId
                + '\'' + ", nodeRole='" + nodeRole + '\'' + ", peerRole='" + peerRole + '\''
                + ", messageType='" + messageType + '\'' + ", messageVersion='" + messageVersion
                + '\'' + ", logicalMessageId='" + logicalMessageId + '\'' + ", deliveryId='"
                + deliveryId + '\'' + ", messageShapeHash=" + messageShapeHash
                + ", messageValueHash=" + messageValueHash + ", messageKey='" + messageKey + '\''
                + ", messageSummary='" + messageSummary + '\'' + ", timedOut=" + timedOut
                + ", beforeExecPathHash=" + beforeExecPathHash + ", beforeExecPath="
                + (beforeExecPath != null ? Arrays.toString(beforeExecPath) : "null")
                + ", afterExecPathHash=" + afterExecPathHash + ", afterExecPath="
                + (afterExecPath != null ? Arrays.toString(afterExecPath) : "null") + ", log='"
                + log + '\'' + '}';
    }

    // --- Canonical key support (Phase 2) ---

    /**
     * Returns the most specific raw semantic type available for this entry. Does
     * NOT apply cross-version alias mapping (see semanticType() for that).
     *
     * Resolution order: 1. payloadType (this.log) if present and not a generic
     * wrapper 2. messageType if present and not a generic wrapper 3. "UNKNOWN_TYPE"
     *
     * Does NOT fall back to methodName — that is version-specific.
     */
    public String rawSemanticType() {
        String payload = this.log; // payloadType alias
        if (isUsableType(payload) && !isGenericWrapper(payload)) {
            return shortClassName(payload);
        }
        if (isUsableType(this.messageType) && !isGenericWrapper(this.messageType)) {
            return this.messageType;
        }
        return "UNKNOWN_TYPE";
    }

    /**
     * Returns the canonical semantic type after applying cross-version alias
     * mapping.
     */
    public String semanticType() {
        String raw = rawSemanticType();
        return SemanticAliasTable.canonicalize(raw);
    }

    /**
     * Returns a flow-stable canonical endpoint string. For SEND: src=nodeId,
     * dst=peerId (or UNKNOWN). For RECV_BEGIN: src=peerId, dst=nodeId (reversed so
     * same logical flow normalizes to same src->dst regardless of which side
     * recorded it).
     */
    public String canonicalEndpointKey() {
        String src, dst;
        if (this.eventType == EventType.SEND) {
            src = bestRole(this.nodeRole, this.nodeId);
            dst = bestRole(this.peerRole, this.peerId);
        } else {
            // RECV_BEGIN: peer sent TO us, so peer is src, we are dst
            src = bestRole(this.peerRole, this.peerId);
            dst = bestRole(this.nodeRole, this.nodeId);
        }
        return src + "->" + dst;
    }

    private static String bestRole(String role, String rawId) {
        if (role != null && !role.isEmpty() && !"null".equals(role)) {
            return role;
        }
        return normalizeRole(rawId);
    }

    /**
     * Returns the full canonical message key for cross-version comparison. Format:
     * {eventDirection}|{src->dst}|{semanticType}
     */
    public String canonicalMessageKey() {
        String dir = (this.eventType != null) ? this.eventType.name() : "UNKNOWN";
        String endpoint = canonicalEndpointKey();
        String semType = semanticType();

        // If both roles unknown, omit endpoint to reduce noise
        if (endpoint.equals("UNKNOWN->UNKNOWN")) {
            return dir + "|" + semType;
        }
        return dir + "|" + endpoint + "|" + semType;
    }

    private static boolean isUsableType(String type) {
        return type != null && !type.isEmpty() && !"null".equals(type);
    }

    private static String shortClassName(String fqcn) {
        int dollar = fqcn.lastIndexOf('$');
        if (dollar >= 0)
            return fqcn.substring(dollar + 1);
        int dot = fqcn.lastIndexOf('.');
        if (dot >= 0)
            return fqcn.substring(dot + 1);
        return fqcn;
    }

    private static boolean isGenericWrapper(String type) {
        return type.contains("RpcProtobufRequest") || type.contains("RpcResponseWrapper")
                || type.contains("RpcRequestWrapper") || type.contains("WritableRpcEngine")
                // Cassandra 3.x generic send/recv wrappers
                || "MessageOut".equals(type) || "MessageIn".equals(type);
    }

    private static String normalizeRole(String rawId) {
        // Phase 3 will add proper role normalization.
        // For now, extract the node-index suffix if present (e.g., "SrnNTLLS-N0" ->
        // "N0")
        if (rawId == null || rawId.isEmpty() || "null".equals(rawId)) {
            return "UNKNOWN";
        }
        int dashN = rawId.lastIndexOf("-N");
        if (dashN >= 0 && dashN + 2 < rawId.length()) {
            String suffix = rawId.substring(dashN + 1);
            if (suffix.matches("N\\d+"))
                return suffix;
        }
        return rawId;
    }
}
