package org.zlab.net.tracker.flow;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.zlab.net.tracker.classifier.ProtocolFamily;

/**
 * Per-window result of {@link TraceFlowExtractor#extract}.
 *
 * <p>
 * Exposes the artifacts the plan calls out in Section 4 of the Phase 2
 * document:
 * <ul>
 * <li>family multiset — {@link #familyMultiset()}</li>
 * <li>flow multiset — {@link #flowMultiset()}</li>
 * <li>per-role-pair compressed family order —
 * {@link #perRolePairCompressedFamilyOrder()} — Phase 3 replaces the earlier
 * single global list with a map keyed on {@code "srcRole->dstRole"} so the
 * scorer can compare within matched local contexts instead of penalizing
 * interleavings between unrelated async flows.</li>
 * <li>boundary-involved flow count — {@link #boundaryInvolvedFlowCount()}</li>
 * <li>role-ambiguous boundary flow count —
 * {@link #roleAmbiguousBoundaryFlowCount()}</li>
 * <li>top service or method labels inside each family —
 * {@link #detailLabelCounts(ProtocolFamily)}</li>
 * </ul>
 *
 * <p>
 * Most views are derived from the {@link TraceFlowSummary} list at construction
 * time. The per-role-pair compressed family order is produced by the extractor
 * during its event-order walk and passed in alongside the flow list —
 * re-deriving it from the flow multiset would be wrong because the flow map
 * orders entries by insertion, not by event timestamp.
 */
public final class FlowExtractionResult implements Serializable {
    private static final long serialVersionUID = 20260420L;

    private final List<TraceFlowSummary> flows;
    private final int flowsGroupedWithExplicitId;
    private final int flowsGroupedWithDeterministicFallback;
    private final int flowsGroupingFailed;
    private final Map<ProtocolFamily, Integer> familyMultiset;
    private final Map<TraceFlowKey, Integer> flowMultiset;
    private final Map<String, List<ProtocolFamily>> perRolePairCompressedFamilyOrder;
    private final int boundaryInvolvedFlowCount;
    private final int roleAmbiguousBoundaryFlowCount;
    private final int unresolvedBoundaryFlowCount;
    private final EnumMap<ProtocolFamily, Map<String, Integer>> detailLabelCountsByFamily;

    FlowExtractionResult(List<TraceFlowSummary> flows, int flowsGroupedWithExplicitId,
            int flowsGroupedWithDeterministicFallback, int flowsGroupingFailed,
            Map<String, List<ProtocolFamily>> perRolePairCompressedFamilyOrder) {
        this.flows = Collections.unmodifiableList(
                new ArrayList<>(flows == null ? Collections.<TraceFlowSummary>emptyList() : flows));
        this.flowsGroupedWithExplicitId = flowsGroupedWithExplicitId;
        this.flowsGroupedWithDeterministicFallback = flowsGroupedWithDeterministicFallback;
        this.flowsGroupingFailed = flowsGroupingFailed;

        Map<ProtocolFamily, Integer> families = new LinkedHashMap<>();
        Map<TraceFlowKey, Integer> multiset = new LinkedHashMap<>();
        EnumMap<ProtocolFamily, Map<String, Integer>> labelsByFamily = new EnumMap<>(
                ProtocolFamily.class);
        int boundary = 0;
        int roleAmbiguous = 0;
        int unresolved = 0;
        for (TraceFlowSummary flow : this.flows) {
            ProtocolFamily family = flow.protocolFamily();
            families.merge(family, flow.eventCount(), Integer::sum);
            multiset.merge(flow.key(), flow.eventCount(), Integer::sum);
            switch (flow.boundaryStatus()) {
                case CROSSING :
                    boundary++;
                    break;
                case ROLE_AMBIGUOUS :
                    roleAmbiguous++;
                    break;
                case UNRESOLVED :
                    unresolved++;
                    break;
                default :
                    break;
            }
            Map<String, Integer> families2 = labelsByFamily.get(family);
            if (families2 == null) {
                families2 = new LinkedHashMap<>();
                labelsByFamily.put(family, families2);
            }
            for (Map.Entry<String, Integer> detail : flow.detailLabelCounts().entrySet()) {
                families2.merge(detail.getKey(), detail.getValue(), Integer::sum);
            }
        }
        this.familyMultiset = Collections.unmodifiableMap(families);
        this.flowMultiset = Collections.unmodifiableMap(multiset);
        this.boundaryInvolvedFlowCount = boundary;
        this.roleAmbiguousBoundaryFlowCount = roleAmbiguous;
        this.unresolvedBoundaryFlowCount = unresolved;
        EnumMap<ProtocolFamily, Map<String, Integer>> sealed = new EnumMap<>(ProtocolFamily.class);
        for (Map.Entry<ProtocolFamily, Map<String, Integer>> e : labelsByFamily.entrySet()) {
            sealed.put(e.getKey(), Collections.unmodifiableMap(e.getValue()));
        }
        this.detailLabelCountsByFamily = sealed;

        // Phase 3: seal the per-role-pair compressed family order that
        // the extractor produced during its event-order walk. Defensive
        // copy keeps the result immutable even if the caller mutates
        // its intermediate map after hand-off.
        if (perRolePairCompressedFamilyOrder == null
                || perRolePairCompressedFamilyOrder.isEmpty()) {
            this.perRolePairCompressedFamilyOrder = Collections.emptyMap();
        } else {
            Map<String, List<ProtocolFamily>> sealedPerPair = new LinkedHashMap<>();
            for (Map.Entry<String, List<ProtocolFamily>> entry : perRolePairCompressedFamilyOrder
                    .entrySet()) {
                List<ProtocolFamily> seq = entry.getValue();
                if (seq == null || seq.isEmpty()) {
                    continue;
                }
                sealedPerPair.put(entry.getKey(),
                        Collections.unmodifiableList(new ArrayList<>(seq)));
            }
            this.perRolePairCompressedFamilyOrder = Collections.unmodifiableMap(sealedPerPair);
        }
    }

    public static FlowExtractionResult empty() {
        return new FlowExtractionResult(Collections.<TraceFlowSummary>emptyList(), 0, 0, 0,
                Collections.<String, List<ProtocolFamily>>emptyMap());
    }

    public List<TraceFlowSummary> flows() {
        return flows;
    }

    public int flowCount() {
        return flows.size();
    }

    public int flowsGroupedWithExplicitId() {
        return flowsGroupedWithExplicitId;
    }

    public int flowsGroupedWithDeterministicFallback() {
        return flowsGroupedWithDeterministicFallback;
    }

    public int flowsGroupingFailed() {
        return flowsGroupingFailed;
    }

    public Map<ProtocolFamily, Integer> familyMultiset() {
        return familyMultiset;
    }

    public Map<TraceFlowKey, Integer> flowMultiset() {
        return flowMultiset;
    }

    /**
     * Per-role-pair compressed family order. Key is {@code "srcRole->dstRole"};
     * value is the ordered list of families observed on that role pair, with
     * consecutive duplicates collapsed.
     *
     * <p>
     * Phase 3 replaces the earlier single global compressed list because the global
     * view conflated unrelated async flows: two independent role pairs could
     * produce a very different global interleaving without any real order
     * divergence. The per-role-pair view lets the scorer average LCS only over
     * pairs present in both lanes, so independent flows do not penalize each other.
     *
     * <p>
     * Sequences are built during the extractor's event-order walk so within-pair
     * ordering reflects actual message timestamps, not insertion order of the flow
     * map.
     */
    public Map<String, List<ProtocolFamily>> perRolePairCompressedFamilyOrder() {
        return perRolePairCompressedFamilyOrder;
    }

    public int boundaryInvolvedFlowCount() {
        return boundaryInvolvedFlowCount;
    }

    public int roleAmbiguousBoundaryFlowCount() {
        return roleAmbiguousBoundaryFlowCount;
    }

    public int unresolvedBoundaryFlowCount() {
        return unresolvedBoundaryFlowCount;
    }

    /**
     * Per-family detail label histogram. Returns an empty map when the family
     * produced no flows or no entry in the family carried a usable detail token.
     */
    public Map<String, Integer> detailLabelCounts(ProtocolFamily family) {
        Map<String, Integer> labels = detailLabelCountsByFamily.get(family);
        return labels == null ? Collections.<String, Integer>emptyMap() : labels;
    }

    /**
     * Read-only map covering every family that produced at least one detail label.
     */
    public Map<ProtocolFamily, Map<String, Integer>> detailLabelCountsByFamily() {
        return Collections.unmodifiableMap(detailLabelCountsByFamily);
    }
}
