package org.zlab.net.tracker;

import java.io.Serializable;
import java.util.Arrays;

import org.zlab.net.tracker.classifier.ProtocolFamily;
import org.zlab.net.tracker.classifier.ProtocolFamilyClass;
import org.zlab.net.tracker.classifier.ProtocolFamilyClassifier;

public class TraceEntry implements Serializable {
    private static final long serialVersionUID = 20260419L;

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
    /** Phase 1 classifier input — see {@link SendMeta#rpcService}. */
    public final String rpcService;
    /** Phase 1 classifier input — see {@link SendMeta#rpcMethod}. */
    public final String rpcMethod;
    /** Phase 1 classifier input — see {@link SendMeta#messageKind}. */
    public final String messageKind;
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
                System.currentTimeMillis(), System.nanoTime(),
                // nodeId, peerId, nodeRole, peerRole, channel, protocol
                null, null, null, null, null, null,
                // messageType, messageVersion, rpcService, rpcMethod, messageKind,
                // logicalMessageId, deliveryId, fanoutType
                null, null, null, null, null, null, null, null,
                // targetCount, messageShapeHash, messageValueHash
                -1, -1L, -1L,
                // messageKey, messageSummary, timedOut
                null, null, false,
                // beforeExecPathHash, beforeExecPath, afterExecPathHash, afterExecPath
                -1L, null, -1L, null,
                // payloadType
                null);
    }

    public TraceEntry(int id, String methodName, int hashcode, EventType eventType,
            boolean changedMessage, long timestamp, long timestampNanos, String nodeId,
            String peerId, String nodeRole, String peerRole, String channel, String protocol,
            String messageType, String messageVersion, String rpcService, String rpcMethod,
            String messageKind, String logicalMessageId, String deliveryId, String fanoutType,
            int targetCount, long messageShapeHash, long messageValueHash, String messageKey,
            String messageSummary, boolean timedOut, long beforeExecPathHash, int[] beforeExecPath,
            long afterExecPathHash, int[] afterExecPath, String payloadType) {
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
        this.rpcService = rpcService;
        this.rpcMethod = rpcMethod;
        this.messageKind = messageKind;
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
                messageVersion, rpcService, rpcMethod, messageKind, logicalMessageId, deliveryId,
                fanoutType, targetCount, messageShapeHash, messageValueHash, messageKey,
                messageSummary, timedOut, beforeExecPathHash, beforeExecPath, afterExecPathHash,
                afterExecPath, log);
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
                + '\'' + ", rpcService='" + rpcService + '\'' + ", rpcMethod='" + rpcMethod + '\''
                + ", messageKind='" + messageKind + '\'' + ", logicalMessageId='" + logicalMessageId
                + '\'' + ", deliveryId='" + deliveryId + '\'' + ", messageShapeHash="
                + messageShapeHash + ", messageValueHash=" + messageValueHash + ", messageKey='"
                + messageKey + '\'' + ", messageSummary='" + messageSummary + '\'' + ", timedOut="
                + timedOut + ", beforeExecPathHash=" + beforeExecPathHash + ", beforeExecPath="
                + (beforeExecPath != null ? Arrays.toString(beforeExecPath) : "null")
                + ", afterExecPathHash=" + afterExecPathHash + ", afterExecPath="
                + (afterExecPath != null ? Arrays.toString(afterExecPath) : "null") + ", log='"
                + log + '\'' + '}';
    }

    // --- Canonical key support ---

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
     * Returns the most specific available token for the
     * {@link CanonicalKeyMode#GUIDANCE} {@code UNKNOWN:{tail}} fallback. Prefers
     * the Phase 1 classifier inputs ({@link #rpcService} and {@link #rpcMethod})
     * over the raw semantic type so distinct unclassified HDFS / HBase RPC methods
     * remain distinguishable even when neither {@code messageType} nor
     * {@code payloadType} is usable (the common case for protobuf-wrapped calls).
     * Falls back to the raw semantic type when no RPC method is available.
     */
    String guidanceUnknownTail() {
        String method = normalizeOrNull(this.rpcMethod);
        if (method != null) {
            String service = normalizeOrNull(this.rpcService);
            if (service != null) {
                return shortClassName(service) + "#" + method;
            }
            return method;
        }
        String service = normalizeOrNull(this.rpcService);
        if (service != null) {
            return shortClassName(service);
        }
        return rawSemanticType();
    }

    private static String normalizeOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || "null".equals(trimmed)) {
            return null;
        }
        return trimmed;
    }

    /**
     * Returns the canonical semantic type after applying cross-version alias
     * mapping. Still used by the {@link CanonicalKeyMode#SEMANTIC_SHAPE_SUMMARY}
     * diagnostic tier.
     */
    public String semanticType() {
        String raw = rawSemanticType();
        return SemanticAliasTable.canonicalize(raw);
    }

    /**
     * Phase 1 online guidance classification. The classifier consumes the richer
     * attributes ({@code protocol}, {@code messageType}, {@code rpcService},
     * {@code rpcMethod}, {@code messageKind}, and the payload class name) and
     * returns a stable {@link ProtocolFamily}. A {@code null} or unrecognised
     * message falls back to {@link ProtocolFamily#UNKNOWN}.
     */
    public ProtocolFamily protocolFamily() {
        return ProtocolFamilyClassifier.classify(this.protocol, this.messageType, this.rpcService,
                this.rpcMethod, this.messageKind, this.log);
    }

    /**
     * Convenience accessor returning the coarse {@link ProtocolFamilyClass}
     * attached to {@link #protocolFamily()}.
     */
    public ProtocolFamilyClass protocolFamilyClass() {
        return protocolFamily().familyClass();
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
     * Returns the canonical message key at the default
     * {@link CanonicalKeyMode#GUIDANCE} tier. This is the Phase 1 online identity
     * used by the live scorer.
     */
    public String canonicalMessageKey() {
        return canonicalMessageKey(CanonicalKeyMode.GUIDANCE);
    }

    /**
     * Returns the canonical message key at the requested tier. A {@code null} tier
     * defaults to {@link CanonicalKeyMode#GUIDANCE}.
     *
     * <ul>
     * <li>{@link CanonicalKeyMode#GUIDANCE}:
     * {@code {dir}|{endpoint}|{protocolFamily}}, with
     * {@code UNKNOWN:{rawSemanticType}} appended when the classifier falls back to
     * {@link ProtocolFamily#UNKNOWN}. The tail keeps long-tail traffic
     * distinguishable while Phase 5 profiles grow the classifier coverage.</li>
     * <li>{@link CanonicalKeyMode#SEMANTIC_SHAPE_SUMMARY}:
     * {@code {dir}|{endpoint}|{semType}|shape={hex}|sum={bucketHash}}</li>
     * </ul>
     *
     * The endpoint fragment is omitted when both roles resolve to {@code UNKNOWN}
     * so lane comparisons remain stable in role-starved traces.
     */
    public String canonicalMessageKey(CanonicalKeyMode mode) {
        CanonicalKeyMode resolved = mode != null ? mode : CanonicalKeyMode.GUIDANCE;
        String dir = (this.eventType != null) ? this.eventType.name() : "UNKNOWN";
        String endpoint = canonicalEndpointKey();

        StringBuilder key = new StringBuilder();
        key.append(dir).append('|');
        if (!endpoint.equals("UNKNOWN->UNKNOWN")) {
            key.append(endpoint).append('|');
        }
        switch (resolved) {
            case GUIDANCE : {
                ProtocolFamily family = protocolFamily();
                key.append(family.token());
                if (family == ProtocolFamily.UNKNOWN) {
                    // Preserve distinguishability within the UNKNOWN bucket so
                    // long-tail traffic still splits across keys until Phase 5
                    // profiles grow the classifier coverage. The tail prefers
                    // the Phase 1 classifier inputs ({@link #rpcService} and
                    // {@link #rpcMethod}) so distinct unclassified HDFS /
                    // HBase RPCs remain separable even when the payload
                    // / message types are generic protobuf wrappers; only
                    // as a last resort does it fall through to the raw
                    // semantic type.
                    key.append(':').append(guidanceUnknownTail());
                }
                break;
            }
            case SEMANTIC_SHAPE_SUMMARY :
                key.append(semanticType());
                key.append("|shape=").append(Long.toHexString(this.messageShapeHash));
                key.append("|sum=").append(SummaryBucket.bucketHash(this.messageSummary));
                break;
            default :
                key.append(protocolFamily().token());
                break;
        }
        return key.toString();
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
        // Extract the node-index suffix if present (e.g., "SrnNTLLS-N0" ->
        // "N0"); otherwise return the raw id so the endpoint remains stable
        // across runs.
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
