package org.zlab.net.tracker.flow;

import java.io.Serializable;
import java.util.Objects;

import org.zlab.net.tracker.classifier.ProtocolFamily;

/**
 * Stable identity for a logical flow extracted from an aligned trace window. A
 * flow aggregates send and receive events that belong to the same logical
 * exchange — typically a request/response pair sharing the same correlation
 * token.
 *
 * <p>
 * Fields:
 * <ul>
 * <li>{@link #stage} — the window's {@code comparisonStageId}. Flow grouping is
 * stage-local, so the stage is part of the key by construction and cross-stage
 * matches cannot happen.</li>
 * <li>{@link #srcRole} / {@link #dstRole} — the canonical source and
 * destination roles ({@code UNKNOWN} when unresolved). Normalized by direction:
 * for SEND the role pair is {@code nodeRole ->
 *       peerRole}; for RECV it is reversed so the same exchange looks identical
 * no matter which side recorded it.</li>
 * <li>{@link #protocolFamily} — the classifier output; Phase 1's online
 * identity. A flow does not span families because the family is part of the
 * key.</li>
 * <li>{@link #correlationKey} — the chosen correlation token, either an
 * explicit identifier taken from the trace metadata or a deterministic fallback
 * bucket.</li>
 * <li>{@link #correlationSource} — how the correlation token was chosen.
 * Explicit-ID keys and deterministic-fallback keys are intentionally distinct
 * so cross-lane comparison can treat the fallback tier as bounded grouping aid
 * rather than real logical identity (see Phase 2 rule: "fallback buckets are
 * not the sole cross-lane matching identity").</li>
 * </ul>
 *
 * <p>
 * Instances are immutable and suitable for use as map keys.
 */
public final class TraceFlowKey implements Serializable {
    private static final long serialVersionUID = 20260419L;

    public final String stage;
    public final String srcRole;
    public final String dstRole;
    public final ProtocolFamily protocolFamily;
    public final String correlationKey;
    public final CorrelationSource correlationSource;

    public TraceFlowKey(String stage, String srcRole, String dstRole, ProtocolFamily protocolFamily,
            String correlationKey, CorrelationSource correlationSource) {
        this.stage = stage == null ? "" : stage;
        this.srcRole = srcRole == null ? "UNKNOWN" : srcRole;
        this.dstRole = dstRole == null ? "UNKNOWN" : dstRole;
        this.protocolFamily = protocolFamily == null ? ProtocolFamily.UNKNOWN : protocolFamily;
        this.correlationKey = correlationKey == null ? "" : correlationKey;
        this.correlationSource = correlationSource == null
                ? CorrelationSource.GROUPING_FAILED
                : correlationSource;
    }

    public boolean hasExplicitCorrelation() {
        return correlationSource.isExplicit();
    }

    /**
     * Compact, human-readable representation used by both the flow multiset
     * accessor and the observability CSV writers.
     */
    public String token() {
        return stage + "|" + srcRole + "->" + dstRole + "|" + protocolFamily.token() + "|"
                + correlationSource.name() + "|" + correlationKey;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TraceFlowKey)) {
            return false;
        }
        TraceFlowKey other = (TraceFlowKey) obj;
        return stage.equals(other.stage) && srcRole.equals(other.srcRole)
                && dstRole.equals(other.dstRole) && protocolFamily == other.protocolFamily
                && correlationSource == other.correlationSource
                && correlationKey.equals(other.correlationKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stage, srcRole, dstRole, protocolFamily, correlationSource,
                correlationKey);
    }

    @Override
    public String toString() {
        return "TraceFlowKey{" + token() + "}";
    }
}
