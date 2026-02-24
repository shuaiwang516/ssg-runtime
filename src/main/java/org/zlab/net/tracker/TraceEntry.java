package org.zlab.net.tracker;

import java.io.Serializable;
import java.util.Arrays;

public class TraceEntry implements Serializable {
    private static final long serialVersionUID = 20260224L;

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
                null, null, null, -1, -1, -1, null, null, false, -1, null, -1, null, null);
    }

    public TraceEntry(int id, String methodName, int hashcode, EventType eventType,
            boolean changedMessage, long timestamp, long timestampNanos, String nodeId,
            String peerId, String channel, String protocol, String messageType,
            String messageVersion, String logicalMessageId, String deliveryId, String fanoutType,
            int targetCount, long messageShapeHash, long messageValueHash, String messageKey,
            String messageSummary, boolean timedOut, long beforeExecPathHash,
            int[] beforeExecPath, long afterExecPathHash, int[] afterExecPath,
            String payloadType) {
        this.id = id;
        this.methodName = methodName;
        this.hashcode = hashcode;
        this.eventType = eventType == null ? EventType.UNKNOWN : eventType;
        this.changedMessage = changedMessage;
        this.timestamp = timestamp;
        this.timestampNanos = timestampNanos;
        this.nodeId = nodeId;
        this.peerId = peerId;
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
                timestampNanos, nodeId, peerId, channel, protocol, messageType, messageVersion,
                logicalMessageId, deliveryId, fanoutType, targetCount, messageShapeHash,
                messageValueHash, messageKey, messageSummary, timedOut, beforeExecPathHash,
                beforeExecPath, afterExecPathHash, afterExecPath, log);
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
                + '\'' + ", messageType='" + messageType + '\'' + ", messageVersion='"
                + messageVersion + '\'' + ", logicalMessageId='" + logicalMessageId + '\''
                + ", deliveryId='" + deliveryId + '\'' + ", messageShapeHash=" + messageShapeHash
                + ", messageValueHash=" + messageValueHash + ", messageKey='" + messageKey + '\''
                + ", messageSummary='" + messageSummary + '\'' + ", timedOut=" + timedOut
                + ", beforeExecPathHash=" + beforeExecPathHash + ", beforeExecPath="
                + (beforeExecPath != null ? Arrays.toString(beforeExecPath) : "null")
                + ", afterExecPathHash=" + afterExecPathHash + ", afterExecPath="
                + (afterExecPath != null ? Arrays.toString(afterExecPath) : "null") + ", log='"
                + log + '\'' + '}';
    }
}
