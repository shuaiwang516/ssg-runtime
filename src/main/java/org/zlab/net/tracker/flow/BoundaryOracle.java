package org.zlab.net.tracker.flow;

import org.zlab.net.tracker.TraceEntry;

/**
 * Per-entry rolling-upgrade boundary classifier injected into
 * {@link TraceFlowExtractor}. The runtime library in {@code ssg-runtime} must
 * stay decoupled from the upfuzz-side {@code TopologySnapshot} /
 * {@code ResolvedTraceEndpoint} types, so boundary classification is supplied
 * as a small functional interface and the caller wires it with whatever
 * topology information is available on the server.
 *
 * <p>
 * Returning {@link FlowBoundaryStatus#NONE} means "no boundary involvement for
 * this entry" — both endpoints resolved to the same side of the upgraded cut.
 * All other values are propagated into the flow's aggregated status (see
 * {@link FlowBoundaryStatus#combineWith}).
 */
@FunctionalInterface
public interface BoundaryOracle {

    BoundaryOracle UNKNOWN = entry -> FlowBoundaryStatus.UNRESOLVED;
    BoundaryOracle NO_BOUNDARY = entry -> FlowBoundaryStatus.NONE;

    FlowBoundaryStatus classify(TraceEntry entry);
}
