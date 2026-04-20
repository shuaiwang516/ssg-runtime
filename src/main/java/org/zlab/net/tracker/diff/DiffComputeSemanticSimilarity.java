package org.zlab.net.tracker.diff;

import org.zlab.net.tracker.CanonicalKeyMode;
import org.zlab.net.tracker.Trace;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Computes semantic similarity between traces using weighted multiset Jaccard
 * on canonical message keys.
 */
public final class DiffComputeSemanticSimilarity {
    private DiffComputeSemanticSimilarity() {
    }

    /**
     * Convenience entry point using the production default
     * {@link CanonicalKeyMode#GUIDANCE} identity. Prefer
     * {@link #compute(Trace, Trace, Trace, CanonicalKeyMode)} when the caller needs
     * to select the tier explicitly.
     */
    public static double[] compute(Trace trace0, Trace trace1, Trace trace2) {
        return compute(trace0, trace1, trace2, CanonicalKeyMode.GUIDANCE);
    }

    /**
     * Three-way comparison at the requested {@link CanonicalKeyMode} tier. Returns
     * {sim(t0,t1), sim(t1,t2), sim(t0,t2)}. Third element is baseline drift
     * reference.
     */
    public static double[] compute(Trace trace0, Trace trace1, Trace trace2,
            CanonicalKeyMode mode) {
        CanonicalKeyMode resolved = mode != null ? mode : CanonicalKeyMode.GUIDANCE;
        Map<String, Integer> ms0 = safeMultiset(trace0, resolved);
        Map<String, Integer> ms1 = safeMultiset(trace1, resolved);
        Map<String, Integer> ms2 = safeMultiset(trace2, resolved);
        return new double[]{multisetJaccard(ms0, ms1), multisetJaccard(ms1, ms2),
                multisetJaccard(ms0, ms2) // baseline drift
        };
    }

    /**
     * Weighted multiset Jaccard: sum(min(a_i,b_i)) / sum(max(a_i,b_i)). Returns 1.0
     * if both multisets are empty.
     */
    public static double multisetJaccard(Map<String, Integer> a, Map<String, Integer> b) {
        Set<String> allKeys = new HashSet<>(a.keySet());
        allKeys.addAll(b.keySet());
        long intersectionSum = 0, unionSum = 0;
        for (String key : allKeys) {
            int ca = a.getOrDefault(key, 0);
            int cb = b.getOrDefault(key, 0);
            intersectionSum += Math.min(ca, cb);
            unionSum += Math.max(ca, cb);
        }
        return unionSum == 0 ? 1.0 : (double) intersectionSum / unionSum;
    }

    private static Map<String, Integer> safeMultiset(Trace trace, CanonicalKeyMode mode) {
        return trace != null ? trace.getCanonicalMultiset(mode) : Collections.emptyMap();
    }
}
