package org.zlab.net.tracker.flow;

/**
 * How a {@link TraceFlowKey} obtained its correlation token.
 *
 * <p>
 * Phase 2 requires that explicit-ID flows and deterministic-fallback flows stay
 * distinguishable: fallback buckets are bounded intra-window grouping aids and
 * must not be treated as the sole cross-lane identity without replay evidence.
 * Encoding the source in the key enforces that contract —
 * {@link TraceFlowKey#equals(Object)} keeps an explicit-ID flow and a
 * deterministic-fallback flow separate even when every other field happens to
 * match.
 *
 * <p>
 * Priority order, highest first:
 * <ol>
 * <li>{@link #EXPLICIT_LOGICAL_ID} — the runtime captured
 * {@code logicalMessageId} via the bridge's {@code id}/{@code getId}/
 * {@code messageId}/{@code streamId}/{@code sessionId}/
 * {@code scannerId}/{@code callId}/{@code tracingId} accessors. This is the
 * request-level identifier the plan designates as the default correlation
 * key.</li>
 * <li>{@link #EXPLICIT_DELIVERY_ID} — the runtime captured {@code deliveryId}
 * through a native accessor ({@code deliveryId} or {@code requestId}), distinct
 * from {@code logicalMessageId}. Only consulted when {@code logicalMessageId}
 * is absent; a synthesized {@code logicalMessageId@peer} alias does not reach
 * this tier (it is recognized by the extractor and demoted to fallback).</li>
 * <li>{@link #DETERMINISTIC_FALLBACK} — no usable explicit identifier; the
 * extractor assembled a bounded bucket key from
 * {@code (stage, srcRole, dstRole, family, rpcService, rpcMethod,
 *       messageKind, bucketIdx)}. Bounded enough to keep the bucket size under
 * control, deterministic enough to stay reproducible across replays on the same
 * lane.</li>
 * <li>{@link #GROUPING_FAILED} — a defensive fallback used when the entry
 * carries no metadata at all. The extractor still produces a flow so the
 * counter stays honest, but the key is unlikely to be comparable across
 * lanes.</li>
 * </ol>
 */
public enum CorrelationSource {
    EXPLICIT_LOGICAL_ID(true), EXPLICIT_DELIVERY_ID(true), DETERMINISTIC_FALLBACK(
            false), GROUPING_FAILED(false);

    private final boolean explicit;

    CorrelationSource(boolean explicit) {
        this.explicit = explicit;
    }

    public boolean isExplicit() {
        return explicit;
    }
}
