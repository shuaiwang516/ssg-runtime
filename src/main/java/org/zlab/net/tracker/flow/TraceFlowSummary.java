package org.zlab.net.tracker.flow;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.zlab.net.tracker.classifier.ProtocolFamily;
import org.zlab.net.tracker.classifier.ProtocolFamilyClass;

/**
 * Per-window logical-flow summary. One instance describes a single flow grouped
 * by {@link TraceFlowKey}: which direction it moved, how many events it
 * carried, whether the events involved an upgraded-boundary crossing, and the
 * service/method detail the classifier was able to extract.
 *
 * <p>
 * {@link #detailLabelCounts} keeps the full histogram of per-entry detail
 * labels (see {@link #buildDetailLabel}) so the server can surface the top
 * divergent service or method names inside each diverging flow family. The
 * histogram uses insertion order so tests can rely on stable ordering without
 * sorting.
 */
public final class TraceFlowSummary implements Serializable {
    private static final long serialVersionUID = 20260419L;

    private final TraceFlowKey key;
    private final int eventCount;
    private final boolean hasSend;
    private final boolean hasReceive;
    private final FlowBoundaryStatus boundaryStatus;
    private final Map<String, Integer> detailLabelCounts;

    public TraceFlowSummary(TraceFlowKey key, int eventCount, boolean hasSend, boolean hasReceive,
            FlowBoundaryStatus boundaryStatus, Map<String, Integer> detailLabelCounts) {
        this.key = key;
        this.eventCount = eventCount;
        this.hasSend = hasSend;
        this.hasReceive = hasReceive;
        this.boundaryStatus = boundaryStatus == null ? FlowBoundaryStatus.NONE : boundaryStatus;
        Map<String, Integer> copy = new LinkedHashMap<>();
        if (detailLabelCounts != null) {
            copy.putAll(detailLabelCounts);
        }
        this.detailLabelCounts = Collections.unmodifiableMap(copy);
    }

    public TraceFlowKey key() {
        return key;
    }

    public int eventCount() {
        return eventCount;
    }

    public boolean hasSend() {
        return hasSend;
    }

    public boolean hasReceive() {
        return hasReceive;
    }

    public FlowBoundaryStatus boundaryStatus() {
        return boundaryStatus;
    }

    public boolean boundaryInvolved() {
        return boundaryStatus == FlowBoundaryStatus.CROSSING;
    }

    public boolean roleAmbiguousBoundary() {
        return boundaryStatus == FlowBoundaryStatus.ROLE_AMBIGUOUS;
    }

    public ProtocolFamily protocolFamily() {
        return key.protocolFamily;
    }

    public ProtocolFamilyClass protocolFamilyClass() {
        return key.protocolFamily.familyClass();
    }

    /** Read-only view of the detail-label histogram (insertion order). */
    public Map<String, Integer> detailLabelCounts() {
        return detailLabelCounts;
    }

    /**
     * Build the detail label for a single event. Precedence:
     * <ol>
     * <li>{@code rpcService#rpcMethod[messageKind]} when an RPC method is available
     * (HBase / HDFS path, including mutate subtype refinement when
     * {@code messageKind} is present).</li>
     * <li>{@code rpcMethod[messageKind]} when only the method is available.</li>
     * <li>{@code verb:messageType} for Cassandra-style verbs.</li>
     * <li>{@code kind:messageKind} as a last resort to preserve the subtype hint
     * when every other field is missing.</li>
     * </ol>
     * Returns {@code null} when no useful detail could be built — the extractor
     * omits such entries from the label histogram so the "missing detail" case
     * stays explicit instead of polluting the counts with an empty string.
     */
    public static String buildDetailLabel(String rpcService, String rpcMethod, String messageType,
            String messageKind) {
        String service = normalizeOrNull(rpcService);
        String method = normalizeOrNull(rpcMethod);
        String type = normalizeOrNull(messageType);
        String kind = normalizeOrNull(messageKind);
        if (method != null) {
            StringBuilder sb = new StringBuilder();
            if (service != null) {
                sb.append(service).append('#');
            }
            sb.append(method);
            if (kind != null) {
                sb.append('[').append(kind).append(']');
            }
            return sb.toString();
        }
        if (type != null) {
            if (kind != null) {
                return "verb:" + type + '[' + kind + ']';
            }
            return "verb:" + type;
        }
        if (kind != null) {
            return "kind:" + kind;
        }
        return null;
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

    /**
     * Shallow helper: return the detail label with the highest count, ties broken
     * by insertion order. Returns {@code null} when no detail was captured.
     */
    public String topDetailLabel() {
        String best = null;
        int bestCount = -1;
        for (Map.Entry<String, Integer> e : detailLabelCounts.entrySet()) {
            if (e.getValue() > bestCount) {
                best = e.getKey();
                bestCount = e.getValue();
            }
        }
        return best;
    }

    /**
     * List view of the detail labels in insertion order. Callers that want the full
     * label set (e.g. HBase mutate subtype detail preserved across a flow) can
     * iterate this instead of probing the histogram map directly.
     */
    public List<String> detailLabels() {
        if (detailLabelCounts.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new java.util.ArrayList<>(detailLabelCounts.keySet()));
    }

    @Override
    public String toString() {
        return "TraceFlowSummary{" + key.token() + ", events=" + eventCount + ", send=" + hasSend
                + ", recv=" + hasReceive + ", boundary=" + boundaryStatus + ", details="
                + detailLabelCounts + '}';
    }
}
