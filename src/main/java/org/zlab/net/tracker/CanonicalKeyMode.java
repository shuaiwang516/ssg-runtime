package org.zlab.net.tracker;

/**
 * Selects how strictly {@link TraceEntry#canonicalMessageKey(CanonicalKeyMode)}
 * distinguishes two messages.
 *
 * <p>
 * Tiers are ordered from coarsest (best cross-version matching) to strictest
 * (most precise but most fragile across versions):
 *
 * <ol>
 * <li>{@link #SEMANTIC} — {direction|endpoint|semanticType}. The original Phase
 * 2 key. Guaranteed to survive message-type renames handled by
 * {@link SemanticAliasTable}.</li>
 * <li>{@link #SEMANTIC_SHAPE} — adds {@code messageShapeHash}. Separates
 * messages whose payload field set differs even when the raw semantic type
 * matches. Shape hashes are stable as long as payload classes are unchanged
 * across versions.</li>
 * <li>{@link #SEMANTIC_SHAPE_SUMMARY} — adds a bucketed summary hash derived
 * from {@link TraceEntry#messageSummary}. Summary tokens are normalized
 * (numbers, UUIDs, hex, volatile wrapper types, variable-size containers)
 * before hashing, so the bucket is stable against value noise while still
 * separating structurally different messages.</li>
 * <li>{@link #SEMANTIC_SHAPE_VALUE} — adds {@code messageValueHash} directly.
 * Offered for offline analysis and strict experiments. Not recommended as a
 * default because cross-version refactors (field reorderings, new fields,
 * different representations) easily break matching.</li>
 * </ol>
 *
 * <p>
 * The Apr 12 improvement plan picks {@link #SEMANTIC_SHAPE_SUMMARY} as the
 * rerun-candidate default for mode 5 because it is strictly more discriminating
 * than the previous {@link #SEMANTIC} key while still bucketing benign value
 * drift.
 */
public enum CanonicalKeyMode {
    SEMANTIC, SEMANTIC_SHAPE, SEMANTIC_SHAPE_SUMMARY, SEMANTIC_SHAPE_VALUE;

    /**
     * Recommended default for production fuzzing runs. Chosen by the Apr 12 mode-5
     * efficiency plan — tighter than {@link #SEMANTIC} but does not over-fragment
     * across versions the way raw {@link #SEMANTIC_SHAPE_VALUE} does.
     */
    public static final CanonicalKeyMode DEFAULT = SEMANTIC_SHAPE_SUMMARY;
}
