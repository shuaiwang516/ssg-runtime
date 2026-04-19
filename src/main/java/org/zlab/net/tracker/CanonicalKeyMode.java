package org.zlab.net.tracker;

/**
 * Selects how
 * {@link TraceEntry#canonicalMessageKey(CanonicalKeyMode)} turns a trace
 * entry into a canonical identity string.
 *
 * <p>
 * Phase 1 collapses the canonical-key space down to the two tiers that
 * still serve a concrete purpose after the guidance redesign:
 *
 * <ol>
 * <li>{@link #GUIDANCE} — the new online guidance identity. Format is
 * {@code direction|srcRole->dstRole|protocolFamily}, where
 * {@code protocolFamily} is produced by
 * {@link org.zlab.net.tracker.classifier.ProtocolFamilyClassifier}. This is
 * the live-fuzzing default. It is stable against wrapper, summary, and
 * payload-class drift as long as the verb or RPC method name is
 * recognised by the first-cut classifier.</li>
 * <li>{@link #SEMANTIC_SHAPE_SUMMARY} — richer offline-diagnostic identity
 * that keeps {@code direction|srcRole->dstRole|semanticType|shape|summary}.
 * Used by {@link TraceEntry} consumers that explicitly want
 * within-semantic / within-shape drift to surface, notably
 * signature-dedup fixtures and the {@code printTrace} debug log. Not used
 * by the production scorer.</li>
 * </ol>
 *
 * <p>
 * The pre-Phase-1 {@code SEMANTIC}, {@code SEMANTIC_SHAPE}, and
 * {@code SEMANTIC_SHAPE_VALUE} tiers were removed in Phase 1 because the
 * redesigned scorer no longer has a use for them — see
 * {@code cloudlab-fix-plans/apr16/phases/2026-04-17-phase-1-online-identity
 * -split-and-family-canonicalization.md}. Reintroducing one of them in a
 * later phase is fine; rolling it forward only for backwards-compatibility
 * is not.
 */
public enum CanonicalKeyMode {
    GUIDANCE, SEMANTIC_SHAPE_SUMMARY;

    /**
     * Recommended default for production fuzzing runs. Phase 1 retargets
     * this at the new {@link #GUIDANCE} identity so live scoring no longer
     * depends on payload-shape or summary drift.
     */
    public static final CanonicalKeyMode DEFAULT = GUIDANCE;
}
