package org.zlab.net.tracker.flow;

/**
 * Aggregated rolling-upgrade boundary status for a {@link TraceFlowSummary}.
 *
 * <p>
 * A flow is the natural unit for boundary corroboration because multiple raw
 * events share the same logical exchange — a single flow should be counted
 * once, not once per constituent event. The aggregation rule is: a flow is
 * {@link #CROSSING} when any event in it was classified as crossing the
 * upgraded/non-upgraded cut; it is {@link #ROLE_AMBIGUOUS} when no event
 * crossed but at least one event's endpoint resolved only to a shared role; it
 * is {@link #UNRESOLVED} when no event crossed or was role-ambiguous but at
 * least one event could not be resolved at all; otherwise it is {@link #NONE}.
 *
 * <p>
 * Boundary classification itself is delegated to a {@link BoundaryOracle}
 * injected by the caller — {@code ssg-runtime-shuai} stays independent of the
 * upfuzz-side topology types.
 */
public enum FlowBoundaryStatus {
    NONE, CROSSING, ROLE_AMBIGUOUS, UNRESOLVED;

    /**
     * Combine the existing flow status with a fresh per-entry classification.
     * Ordering mirrors severity so that a single crossing wins over every weaker
     * signal within the flow and an ambiguous entry survives an
     * otherwise-unresolved flow.
     */
    public FlowBoundaryStatus combineWith(FlowBoundaryStatus entryStatus) {
        if (this == CROSSING || entryStatus == CROSSING) {
            return CROSSING;
        }
        if (this == ROLE_AMBIGUOUS || entryStatus == ROLE_AMBIGUOUS) {
            return ROLE_AMBIGUOUS;
        }
        if (this == UNRESOLVED || entryStatus == UNRESOLVED) {
            return UNRESOLVED;
        }
        return NONE;
    }
}
