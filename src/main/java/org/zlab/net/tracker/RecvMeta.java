package org.zlab.net.tracker;

import java.io.Serializable;

public class RecvMeta implements Serializable {
    private static final long serialVersionUID = 20260208L;

    public final String nodeId;
    public final String peerId;
    public final String channel;
    public final String protocol;
    public final String messageType;
    public final String messageVersion;
    public final String logicalMessageId;
    public final String deliveryId;

    private RecvMeta(Builder builder) {
        this.nodeId = builder.nodeId;
        this.peerId = builder.peerId;
        this.channel = builder.channel;
        this.protocol = builder.protocol;
        this.messageType = builder.messageType;
        this.messageVersion = builder.messageVersion;
        this.logicalMessageId = builder.logicalMessageId;
        this.deliveryId = builder.deliveryId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public RecvMeta withDefaults(String defaultNodeId) {
        if (nodeId != null && !nodeId.isEmpty()) {
            return this;
        }
        return builder().nodeId(defaultNodeId).peerId(peerId).channel(channel).protocol(protocol)
                .messageType(messageType).messageVersion(messageVersion)
                .logicalMessageId(logicalMessageId).deliveryId(deliveryId).build();
    }

    public static class Builder {
        private String nodeId;
        private String peerId;
        private String channel;
        private String protocol;
        private String messageType;
        private String messageVersion;
        private String logicalMessageId;
        private String deliveryId;

        public Builder nodeId(String nodeId) {
            this.nodeId = nodeId;
            return this;
        }

        public Builder peerId(String peerId) {
            this.peerId = peerId;
            return this;
        }

        public Builder channel(String channel) {
            this.channel = channel;
            return this;
        }

        public Builder protocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public Builder messageType(String messageType) {
            this.messageType = messageType;
            return this;
        }

        public Builder messageVersion(String messageVersion) {
            this.messageVersion = messageVersion;
            return this;
        }

        public Builder logicalMessageId(String logicalMessageId) {
            this.logicalMessageId = logicalMessageId;
            return this;
        }

        public Builder deliveryId(String deliveryId) {
            this.deliveryId = deliveryId;
            return this;
        }

        public RecvMeta build() {
            return new RecvMeta(this);
        }
    }
}
