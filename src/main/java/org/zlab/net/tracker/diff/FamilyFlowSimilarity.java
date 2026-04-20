package org.zlab.net.tracker.diff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.zlab.net.tracker.classifier.ProtocolFamily;
import org.zlab.net.tracker.classifier.ProtocolFamilyClass;
import org.zlab.net.tracker.flow.CorrelationSource;
import org.zlab.net.tracker.flow.TraceFlowKey;

/**
 * Phase 3 similarity helpers operating on the Phase 1 family taxonomy and the
 * Phase 2 logical-flow extraction.
 *
 * <p>
 * Three metric families live here:
 * <ul>
 * <li>family-level multiset Jaccard (plain and
 * {@link ProtocolFamilyClass}-weighted),</li>
 * <li>flow-level multiset Jaccard (all flows or explicit-ID flows only — the
 * latter honors the Phase 2 rule that deterministic-fallback buckets must not
 * be the sole cross-lane identity),</li>
 * <li>normalized LCS on compressed family-order sequences for the bounded order
 * signal that the scorer uses as a secondary component.</li>
 * </ul>
 *
 * <p>
 * All similarity helpers return {@code 1.0} when both inputs are empty so that
 * "no traffic on either side" counts as perfect agreement instead of an
 * implicit divergence.
 */
public final class FamilyFlowSimilarity {

    private FamilyFlowSimilarity() {
    }

    /**
     * Plain weighted multiset Jaccard on a family multiset. Every family
     * contributes its raw count to numerator and denominator.
     */
    public static double familyJaccard(Map<ProtocolFamily, Integer> a,
            Map<ProtocolFamily, Integer> b) {
        if (a == null && b == null) {
            return 1.0;
        }
        Map<ProtocolFamily, Integer> left = a == null
                ? Collections.<ProtocolFamily, Integer>emptyMap()
                : a;
        Map<ProtocolFamily, Integer> right = b == null
                ? Collections.<ProtocolFamily, Integer>emptyMap()
                : b;
        Set<ProtocolFamily> keys = new HashSet<>();
        keys.addAll(left.keySet());
        keys.addAll(right.keySet());
        long intersection = 0;
        long union = 0;
        for (ProtocolFamily key : keys) {
            int ca = left.getOrDefault(key, 0);
            int cb = right.getOrDefault(key, 0);
            intersection += Math.min(ca, cb);
            union += Math.max(ca, cb);
        }
        return union == 0 ? 1.0 : (double) intersection / union;
    }

    /**
     * {@link ProtocolFamilyClass}-weighted multiset Jaccard. Each bucket's
     * contribution to numerator and denominator is scaled by the weight of its
     * class so {@code UPGRADE_CRITICAL} overlap dominates and {@code BACKGROUND}
     * overlap cannot single-handedly push the score to 1.0. All weights must be
     * non-negative; passing zero for a class erases that class from the score
     * entirely.
     */
    public static double familyClassWeightedJaccard(Map<ProtocolFamily, Integer> a,
            Map<ProtocolFamily, Integer> b, double upgradeCriticalWeight, double backgroundWeight,
            double unknownWeight) {
        if (a == null && b == null) {
            return 1.0;
        }
        Map<ProtocolFamily, Integer> left = a == null
                ? Collections.<ProtocolFamily, Integer>emptyMap()
                : a;
        Map<ProtocolFamily, Integer> right = b == null
                ? Collections.<ProtocolFamily, Integer>emptyMap()
                : b;
        Set<ProtocolFamily> keys = new HashSet<>();
        keys.addAll(left.keySet());
        keys.addAll(right.keySet());
        double intersection = 0.0;
        double union = 0.0;
        for (ProtocolFamily key : keys) {
            double weight = classWeight(key.familyClass(), upgradeCriticalWeight, backgroundWeight,
                    unknownWeight);
            if (weight <= 0.0) {
                continue;
            }
            int ca = left.getOrDefault(key, 0);
            int cb = right.getOrDefault(key, 0);
            intersection += weight * Math.min(ca, cb);
            union += weight * Math.max(ca, cb);
        }
        return union == 0.0 ? 1.0 : intersection / union;
    }

    /**
     * Weighted multiset Jaccard over every flow, including deterministic-fallback
     * ones. Callers should treat the result as "bounded grouping overlap" rather
     * than request-level equivalence — fallback keys only match when adjacent
     * events happen to produce the same bucket index, which is a weaker signal than
     * explicit-ID matching.
     */
    public static double flowJaccard(Map<TraceFlowKey, Integer> a, Map<TraceFlowKey, Integer> b) {
        return jaccard(a, b);
    }

    /**
     * Multiset Jaccard restricted to flows grouped by an explicit correlation
     * identifier (see {@link CorrelationSource#isExplicit()}). Fallback and
     * grouping-failed flows contribute neither to the numerator nor to the
     * denominator, so a window whose rolling lane collapsed into fallback buckets
     * registers as low explicit overlap even when the bucket shapes happen to
     * align.
     */
    public static double explicitFlowJaccard(Map<TraceFlowKey, Integer> a,
            Map<TraceFlowKey, Integer> b) {
        return jaccard(filterExplicit(a), filterExplicit(b));
    }

    /**
     * Normalized longest-common-subsequence ratio over two compressed family-order
     * sequences inside a single local context. Both inputs must already have
     * consecutive duplicates collapsed.
     *
     * <p>
     * Exposed as a package helper because the Phase 3 live scorer never calls this
     * directly on whole-window sequences — use
     * {@link #perRolePairCompressedOrderSimilarity(Map, Map, ProtocolFamilyClass)}
     * to compare one role-pair's sequence at a time. This form is kept for reuse by
     * per-pair aggregation and by unit tests that want to assert the LCS math in
     * isolation.
     *
     * <p>
     * Returns {@code 1.0} when both sequences are empty so "no ordered traffic on
     * either side" is treated as perfect ordering agreement.
     */
    public static double singleContextOrderSimilarity(List<ProtocolFamily> a,
            List<ProtocolFamily> b) {
        List<ProtocolFamily> left = a == null ? Collections.<ProtocolFamily>emptyList() : a;
        List<ProtocolFamily> right = b == null ? Collections.<ProtocolFamily>emptyList() : b;
        if (left.isEmpty() && right.isEmpty()) {
            return 1.0;
        }
        int lcs = lcsLength(left, right);
        int denom = Math.max(left.size(), right.size());
        if (denom == 0) {
            return 1.0;
        }
        return (double) lcs / denom;
    }

    /**
     * Per-role-pair order similarity across two lanes, filtered to a single family
     * class. The Phase 3 scorer calls this with
     * {@link ProtocolFamilyClass#UPGRADE_CRITICAL} so background chatter's exact
     * interleaving does not affect the score.
     *
     * <p>
     * For each role-pair key present in both inputs:
     * <ol>
     * <li>Filter each lane's family sequence to {@code keepClass} and collapse
     * consecutive duplicates (via {@link #filterByClass}).</li>
     * <li>Compute normalized LCS between the filtered sequences.</li>
     * </ol>
     * The return value is the arithmetic mean of the per-pair similarities.
     *
     * <p>
     * Role-pairs present in only one lane are <em>skipped</em> — the plan
     * explicitly requires that "reorderings among obviously independent
     * asynchronous flows" are not penalized, and a pair appearing on one side only
     * is by definition independent of the other lane.
     *
     * <p>
     * Returns {@code 1.0} when no role-pair is present in both lanes, so "no shared
     * local context" is treated as perfect ordering agreement rather than invented
     * divergence.
     */
    public static double perRolePairCompressedOrderSimilarity(Map<String, List<ProtocolFamily>> a,
            Map<String, List<ProtocolFamily>> b, ProtocolFamilyClass keepClass) {
        if (a == null || a.isEmpty() || b == null || b.isEmpty()) {
            return 1.0;
        }
        int sharedPairs = 0;
        double total = 0.0;
        for (Map.Entry<String, List<ProtocolFamily>> entry : a.entrySet()) {
            List<ProtocolFamily> bSeq = b.get(entry.getKey());
            if (bSeq == null) {
                continue;
            }
            sharedPairs++;
            List<ProtocolFamily> aFiltered = keepClass == null
                    ? entry.getValue()
                    : filterByClass(entry.getValue(), keepClass);
            List<ProtocolFamily> bFiltered = keepClass == null
                    ? bSeq
                    : filterByClass(bSeq, keepClass);
            total += singleContextOrderSimilarity(aFiltered, bFiltered);
        }
        if (sharedPairs == 0) {
            return 1.0;
        }
        return total / sharedPairs;
    }

    /**
     * Compressed family-order sequence filtered to a predicate on the family class.
     * Phase 3 uses this to evaluate order only over upgrade-critical families —
     * background chatter's exact interleaving is defined as noise by the plan.
     */
    public static List<ProtocolFamily> filterByClass(List<ProtocolFamily> sequence,
            ProtocolFamilyClass keepClass) {
        if (sequence == null || sequence.isEmpty()) {
            return Collections.emptyList();
        }
        List<ProtocolFamily> result = new ArrayList<>();
        ProtocolFamily previous = null;
        for (ProtocolFamily family : sequence) {
            if (family == null || family.familyClass() != keepClass) {
                continue;
            }
            if (previous == null || previous != family) {
                result.add(family);
                previous = family;
            }
        }
        return Collections.unmodifiableList(result);
    }

    /** Share of total count that lives in families of the given class. */
    public static double classShare(Map<ProtocolFamily, Integer> counts,
            ProtocolFamilyClass targetClass) {
        if (counts == null || counts.isEmpty()) {
            return 0.0;
        }
        long total = 0;
        long targeted = 0;
        for (Map.Entry<ProtocolFamily, Integer> entry : counts.entrySet()) {
            int count = entry.getValue();
            total += count;
            if (entry.getKey().familyClass() == targetClass) {
                targeted += count;
            }
        }
        if (total == 0) {
            return 0.0;
        }
        return (double) targeted / total;
    }

    /** Sum of all counts in a family multiset. */
    public static int totalCount(Map<ProtocolFamily, Integer> counts) {
        if (counts == null) {
            return 0;
        }
        int total = 0;
        for (int count : counts.values()) {
            total += count;
        }
        return total;
    }

    private static Map<TraceFlowKey, Integer> filterExplicit(Map<TraceFlowKey, Integer> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        java.util.LinkedHashMap<TraceFlowKey, Integer> filtered = new java.util.LinkedHashMap<>();
        for (Map.Entry<TraceFlowKey, Integer> entry : source.entrySet()) {
            TraceFlowKey key = entry.getKey();
            if (key != null && key.hasExplicitCorrelation()) {
                filtered.put(key, entry.getValue());
            }
        }
        return filtered;
    }

    private static <K> double jaccard(Map<K, Integer> a, Map<K, Integer> b) {
        if ((a == null || a.isEmpty()) && (b == null || b.isEmpty())) {
            return 1.0;
        }
        Set<K> keys = new HashSet<>();
        if (a != null) {
            keys.addAll(a.keySet());
        }
        if (b != null) {
            keys.addAll(b.keySet());
        }
        long intersection = 0;
        long union = 0;
        for (K key : keys) {
            int ca = a == null ? 0 : a.getOrDefault(key, 0);
            int cb = b == null ? 0 : b.getOrDefault(key, 0);
            intersection += Math.min(ca, cb);
            union += Math.max(ca, cb);
        }
        return union == 0 ? 1.0 : (double) intersection / union;
    }

    private static double classWeight(ProtocolFamilyClass clazz, double upgradeCriticalWeight,
            double backgroundWeight, double unknownWeight) {
        if (clazz == null) {
            return Math.max(0.0, unknownWeight);
        }
        switch (clazz) {
            case UPGRADE_CRITICAL :
                return Math.max(0.0, upgradeCriticalWeight);
            case BACKGROUND :
                return Math.max(0.0, backgroundWeight);
            case UNKNOWN :
            default :
                return Math.max(0.0, unknownWeight);
        }
    }

    private static int lcsLength(List<ProtocolFamily> a, List<ProtocolFamily> b) {
        int m = a.size();
        int n = b.size();
        if (m == 0 || n == 0) {
            return 0;
        }
        int[] prev = new int[n + 1];
        int[] curr = new int[n + 1];
        for (int i = 1; i <= m; i++) {
            ProtocolFamily ai = a.get(i - 1);
            for (int j = 1; j <= n; j++) {
                if (ai == b.get(j - 1)) {
                    curr[j] = prev[j - 1] + 1;
                } else {
                    curr[j] = Math.max(prev[j], curr[j - 1]);
                }
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
            for (int k = 0; k < curr.length; k++) {
                curr[k] = 0;
            }
        }
        return prev[n];
    }
}
