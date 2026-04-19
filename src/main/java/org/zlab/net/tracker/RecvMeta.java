package org.zlab.net.tracker;

import java.io.Serializable;

public class RecvMeta implements Serializable {
    private static final long serialVersionUID = 20260419L;

    public final String nodeId;
    public final String peerId;
    public final String channel;
    public final String protocol;
    public final String messageType;
    public final String messageVersion;
    /**
     * Phase 1 classifier input — see
     * {@link SendMeta#rpcService} for the contract.
     */
    public final String rpcService;
    /**
     * Phase 1 classifier input — see
     * {@link SendMeta#rpcMethod} for the contract.
     */
    public final String rpcMethod;
    /**
     * Phase 1 classifier input — see
     * {@link SendMeta#messageKind} for the contract.
     */
    public final String messageKind;
    public final String logicalMessageId;
    public final String deliveryId;
    public final String nodeRole;
    public final String peerRole;

    private RecvMeta(Builder builder) {
        this.nodeId = builder.nodeId;
        this.peerId = builder.peerId;
        this.channel = builder.channel;
        this.protocol = builder.protocol;
        this.messageType = builder.messageType;
        this.messageVersion = builder.messageVersion;
        this.rpcService = builder.rpcService;
        this.rpcMethod = builder.rpcMethod;
        this.messageKind = builder.messageKind;
        this.logicalMessageId = builder.logicalMessageId;
        this.deliveryId = builder.deliveryId;
        this.nodeRole = builder.nodeRole;
        this.peerRole = builder.peerRole;
    }

    public static Builder builder() {
        return new Builder();
    }

    public RecvMeta withDefaults(String defaultNodeId, String defaultNodeRole) {
        boolean needsNodeId = nodeId == null || nodeId.isEmpty();
        boolean needsNodeRole = nodeRole == null || nodeRole.isEmpty();
        if (!needsNodeId && !needsNodeRole) {
            return this;
        }
        return builder().nodeId(needsNodeId ? defaultNodeId : nodeId).peerId(peerId)
                .channel(channel).protocol(protocol).messageType(messageType)
                .messageVersion(messageVersion).rpcService(rpcService).rpcMethod(rpcMethod)
                .messageKind(messageKind).logicalMessageId(logicalMessageId).deliveryId(deliveryId)
                .nodeRole(needsNodeRole ? defaultNodeRole : nodeRole).peerRole(peerRole).build();
    }

    /** Backward-compatible overload. */
    public RecvMeta withDefaults(String defaultNodeId) {
        return withDefaults(defaultNodeId, null);
    }

    public static class Builder {
        private String nodeId;
        private String peerId;
        private String channel;
        private String protocol;
        private String messageType;
        private String messageVersion;
        private String rpcService;
        private String rpcMethod;
        private String messageKind;
        private String logicalMessageId;
        private String deliveryId;
        private String nodeRole;
        private String peerRole;

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

        public Builder rpcService(String rpcService) {
            this.rpcService = rpcService;
            return this;
        }

        public Builder rpcMethod(String rpcMethod) {
            this.rpcMethod = rpcMethod;
            return this;
        }

        public Builder messageKind(String messageKind) {
            this.messageKind = messageKind;
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

        public Builder nodeRole(String nodeRole) {
            this.nodeRole = nodeRole;
            return this;
        }

        public Builder peerRole(String peerRole) {
            this.peerRole = peerRole;
            return this;
        }

        public RecvMeta build() {
            return new RecvMeta(this);
        }
    }
}
