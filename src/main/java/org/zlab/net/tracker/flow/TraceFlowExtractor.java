package org.zlab.net.tracker.flow;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.zlab.net.tracker.Trace;
import org.zlab.net.tracker.TraceEntry;
import org.zlab.net.tracker.classifier.ProtocolFamily;

/**
 * Phase 2 bounded per-window flow extractor. Reads a merged {@link Trace} and
 * returns a {@link FlowExtractionResult} grouping send/receive events into
 * logical flows.
 *
 * <p>
 * Rules (see {@code cloudlab-fix-plans/apr16/phases/
 * 2026-04-17-phase-2-logical-flow-reconstruction-and-window-summaries.md} for
 * the authoritative spec):
 * <ul>
 * <li><b>Stage-local.</b> The caller passes the window's
 * {@code comparisonStageId}; it is baked into every {@link TraceFlowKey} so
 * flows cannot span stages by accident.</li>
 * <li><b>Explicit-ID first.</b> When {@link TraceEntry#logicalMessageId} is
 * usable it becomes the correlation token
 * ({@link CorrelationSource#EXPLICIT_LOGICAL_ID}).</li>
 * <li><b>Delivery ID only as a last-resort explicit source.</b> When
 * {@code logicalMessageId} is missing but {@code deliveryId} is present, the
 * extractor promotes it to {@link CorrelationSource#EXPLICIT_DELIVERY_ID} —
 * unless the delivery id is the synthesized {@code logicalId@peer} alias
 * emitted by the bridge fallback, which is demoted to deterministic fallback so
 * it cannot outrank the request-level key on any cross-lane comparison.</li>
 * <li><b>Deterministic fallback bucket.</b> When no explicit ID is available,
 * the extractor constructs
 * {@code stage | srcRole -> dstRole | family | rpcService |
 *       rpcMethod | messageKind | bucketIdx}, where {@code bucketIdx} is a
 * bounded per-tuple counter (see {@link #DEFAULT_FALLBACK_BUCKET_SIZE}).
 * Entries in the same bucket — typically adjacent repetitions of the same RPC
 * within the window — collapse into the same flow; a new bucket opens every
 * {@code FALLBACK_BUCKET_SIZE} entries so the fallback cannot grow without
 * bound.</li>
 * <li><b>Grouping failure</b> is a defensive path for events that carry no
 * usable metadata at all. It still produces a flow (so the observability
 * counters stay honest) but the correlation source is marked
 * {@link CorrelationSource#GROUPING_FAILED}.</li>
 * <li>{@link TraceEntry.EventType#RECV_END} is skipped (same rule as the
 * canonical-multiset accessor) to avoid double-counting receive events.</li>
 * </ul>
 */
public final class TraceFlowExtractor {

    /**
     * Maximum number of raw trace entries that can land in a single
     * deterministic-fallback bucket before a new bucket is opened for the same
     * {@code (srcRole, dstRole, family, rpcService, rpcMethod,
     * messageKind)} tuple. Kept small so no single fallback flow can dominate a
     * window and invisibly merge unrelated traffic.
     */
    public static final int DEFAULT_FALLBACK_BUCKET_SIZE = 8;

    private TraceFlowExtractor() {
    }

    /**
     * Extract flows from a single aligned window. See class-level javadoc for the
     * grouping contract.
     *
     * @param stage
     *            window's {@code comparisonStageId}; baked into each flow key so
     *            flow identity stays stage-local.
     * @param trace
     *            merged per-window trace (one lane's events).
     * @param oracle
     *            per-entry boundary classifier supplied by the caller. Pass
     *            {@link BoundaryOracle#UNKNOWN} when boundary information is
     *            unavailable so the resulting flows land in
     *            {@link FlowBoundaryStatus#UNRESOLVED} instead of silently
     *            pretending they do not involve a boundary.
     */
    public static FlowExtractionResult extract(String stage, Trace trace, BoundaryOracle oracle) {
        return extract(stage, trace, oracle, DEFAULT_FALLBACK_BUCKET_SIZE);
    }

    /**
     * Test-only overload that lets the bounded fallback bucket size be tuned;
     * production code should always use the no-size form to keep the contract
     * uniform across systems.
     */
    static FlowExtractionResult extract(String stage, Trace trace, BoundaryOracle oracle,
            int fallbackBucketSize) {
        if (trace == null) {
            return FlowExtractionResult.empty();
        }
        List<TraceEntry> entries = trace.getTraceEntries();
        if (entries.isEmpty()) {
            return FlowExtractionResult.empty();
        }
        BoundaryOracle resolvedOracle = oracle == null ? BoundaryOracle.UNKNOWN : oracle;
        int bucketSize = fallbackBucketSize > 0 ? fallbackBucketSize : DEFAULT_FALLBACK_BUCKET_SIZE;

        Map<TraceFlowKey, FlowAccumulator> accumulators = new LinkedHashMap<>();
        Map<String, Integer> fallbackTupleCounts = new LinkedHashMap<>();
        int explicitCount = 0;
        int fallbackCount = 0;
        int failedCount = 0;

        for (TraceEntry entry : entries) {
            if (entry == null || entry.eventType == TraceEntry.EventType.RECV_END) {
                continue;
            }
            String srcRole = extractSrcRole(entry);
            String dstRole = extractDstRole(entry);
            ProtocolFamily family = entry.protocolFamily();
            CorrelationChoice choice = resolveCorrelation(entry, family, stage, srcRole, dstRole,
                    fallbackTupleCounts, bucketSize);

            TraceFlowKey key = new TraceFlowKey(stage, srcRole, dstRole, family, choice.token,
                    choice.source);
            FlowAccumulator acc = accumulators.get(key);
            if (acc == null) {
                acc = new FlowAccumulator(key);
                accumulators.put(key, acc);
                switch (choice.source) {
                    case EXPLICIT_LOGICAL_ID :
                    case EXPLICIT_DELIVERY_ID :
                        explicitCount++;
                        break;
                    case DETERMINISTIC_FALLBACK :
                        fallbackCount++;
                        break;
                    case GROUPING_FAILED :
                    default :
                        failedCount++;
                        break;
                }
            }
            acc.eventCount++;
            if (entry.eventType == TraceEntry.EventType.SEND) {
                acc.hasSend = true;
            } else if (entry.eventType == TraceEntry.EventType.RECV_BEGIN) {
                acc.hasReceive = true;
            }
            acc.boundaryStatus = acc.boundaryStatus.combineWith(resolvedOracle.classify(entry));

            String detailLabel = TraceFlowSummary.buildDetailLabel(entry.rpcService,
                    entry.rpcMethod, entry.messageType, entry.messageKind);
            if (detailLabel != null) {
                acc.detailLabels.merge(detailLabel, 1, Integer::sum);
            }
        }

        List<TraceFlowSummary> flows = new ArrayList<>(accumulators.size());
        for (FlowAccumulator acc : accumulators.values()) {
            flows.add(acc.build());
        }
        return new FlowExtractionResult(flows, explicitCount, fallbackCount, failedCount);
    }

    // --- correlation resolution ----------------------------------------------

    private static CorrelationChoice resolveCorrelation(TraceEntry entry, ProtocolFamily family,
            String stage, String srcRole, String dstRole, Map<String, Integer> fallbackTupleCounts,
            int bucketSize) {
        String logicalId = normalizeOrNull(entry.logicalMessageId);
        if (logicalId != null) {
            return new CorrelationChoice(CorrelationSource.EXPLICIT_LOGICAL_ID, logicalId);
        }
        String deliveryId = normalizeOrNull(entry.deliveryId);
        if (deliveryId != null && !isSynthesizedDeliveryAlias(deliveryId)) {
            return new CorrelationChoice(CorrelationSource.EXPLICIT_DELIVERY_ID, deliveryId);
        }

        String tuple = fallbackTuple(stage, srcRole, dstRole, family, entry.rpcService,
                entry.rpcMethod, entry.messageKind);
        Integer next = fallbackTupleCounts.get(tuple);
        int count = next == null ? 0 : next;
        fallbackTupleCounts.put(tuple, count + 1);
        int bucketIdx = bucketSize > 0 ? count / bucketSize : 0;

        boolean hasSomeMetadata = !isBlank(entry.rpcMethod) || !isBlank(entry.rpcService)
                || !isBlank(entry.messageType) || !isBlank(entry.messageKind);
        CorrelationSource source = hasSomeMetadata
                ? CorrelationSource.DETERMINISTIC_FALLBACK
                : CorrelationSource.GROUPING_FAILED;
        String token = tuple + "|bucket=" + bucketIdx;
        return new CorrelationChoice(source, token);
    }

    private static boolean isSynthesizedDeliveryAlias(String deliveryId) {
        // The instrumentation bridge falls back to
        // {@code <logicalId>@<peer>} when the wire protocol does not
        // expose a native delivery token. That synthesized value is
        // peer-decorated and therefore diverges across SEND / RECV even
        // for the same logical exchange, so it cannot act as a
        // correlation token. Demoting it to deterministic-fallback here
        // is how the extractor honors the Phase 2 rule that "a
        // synthesized or peer-decorated deliveryId does not outrank a
        // stable request-level key by default".
        return deliveryId.contains("@");
    }

    private static String fallbackTuple(String stage, String srcRole, String dstRole,
            ProtocolFamily family, String rpcService, String rpcMethod, String messageKind) {
        return stage + "|" + srcRole + "->" + dstRole + "|" + family.token() + "|svc="
                + normalizeForKey(rpcService) + "|method=" + normalizeForKey(rpcMethod) + "|kind="
                + normalizeForKey(messageKind);
    }

    private static String normalizeForKey(String value) {
        String trimmed = normalizeOrNull(value);
        return trimmed == null ? "-" : trimmed;
    }

    // --- role extraction -----------------------------------------------------

    private static String extractSrcRole(TraceEntry entry) {
        if (entry.eventType == TraceEntry.EventType.SEND) {
            return bestRole(entry.nodeRole, entry.nodeId);
        }
        return bestRole(entry.peerRole, entry.peerId);
    }

    private static String extractDstRole(TraceEntry entry) {
        if (entry.eventType == TraceEntry.EventType.SEND) {
            return bestRole(entry.peerRole, entry.peerId);
        }
        return bestRole(entry.nodeRole, entry.nodeId);
    }

    private static String bestRole(String role, String rawId) {
        String trimmedRole = normalizeOrNull(role);
        if (trimmedRole != null) {
            return trimmedRole;
        }
        return normalizeRole(rawId);
    }

    private static String normalizeRole(String rawId) {
        if (isBlank(rawId)) {
            return "UNKNOWN";
        }
        int dashN = rawId.lastIndexOf("-N");
        if (dashN >= 0 && dashN + 2 < rawId.length()) {
            String suffix = rawId.substring(dashN + 1);
            if (suffix.matches("N\\d+")) {
                return suffix;
            }
        }
        return rawId;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isEmpty() || "null".equals(value) || value.trim().isEmpty();
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

    // --- helpers -------------------------------------------------------------

    private static final class CorrelationChoice {
        final CorrelationSource source;
        final String token;

        CorrelationChoice(CorrelationSource source, String token) {
            this.source = source;
            this.token = token;
        }
    }

    private static final class FlowAccumulator {
        final TraceFlowKey key;
        int eventCount;
        boolean hasSend;
        boolean hasReceive;
        FlowBoundaryStatus boundaryStatus = FlowBoundaryStatus.NONE;
        final Map<String, Integer> detailLabels = new LinkedHashMap<>();

        FlowAccumulator(TraceFlowKey key) {
            this.key = key;
        }

        TraceFlowSummary build() {
            return new TraceFlowSummary(key, eventCount, hasSend, hasReceive, boundaryStatus,
                    detailLabels);
        }
    }
}
