package org.zlab.net.tracker.diff;

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
     * Three-way comparison: returns {sim(t0,t1), sim(t1,t2), sim(t0,t2)}. Third
     * element is baseline drift reference.
     */
    public static double[] compute(Trace trace0, Trace trace1, Trace trace2) {
        Map<String, Integer> ms0 = safeMultiset(trace0);
        Map<String, Integer> ms1 = safeMultiset(trace1);
        Map<String, Integer> ms2 = safeMultiset(trace2);
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

    private static Map<String, Integer> safeMultiset(Trace trace) {
        return trace != null ? trace.getCanonicalMultiset() : Collections.emptyMap();
    }
}
