package org.zlab.net.tracker.diff;

import org.apache.datasketches.theta.CompactSketch;
import org.apache.datasketches.theta.UpdateSketch;
import org.zlab.net.tracker.Trace;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.datasketches.theta.JaccardSimilarity.jaccard;

public class DiffComputeJaccardSimilarity {
    private static final boolean useNGrams = true;
    private static final int N_NGram = 2;

    // Diff-RU testing
    public static double[] compute(Trace trace0, Trace trace1, Trace trace2) {
        // diff between 0 and 1
        // diff between 1 and 2
        return new double[]{compute(trace0, trace1), compute(trace1, trace2)};
    }

    public static double compute(Trace trace0, Trace trace1) {
        List<String> trace0List;
        List<String> trace1List;

        if (useNGrams) {
            trace0List = generateNGrams(trace0.getHashCodes(), N_NGram);
            trace1List = generateNGrams(trace1.getHashCodes(), N_NGram);
        } else {
            trace0List = trace0.getHashCodes();
            trace1List = trace1.getHashCodes();
        }

        return jaccardSimilarity(trace0List, trace1List);
    }

    public static double jaccardSimilarity(List<String> trace0, List<String> trace1) {
        // Create a sketch for each trace
        UpdateSketch sketch0 = UpdateSketch.builder().build();
        UpdateSketch sketch1 = UpdateSketch.builder().build();

        // Update sketches with the trace values
        trace0.forEach(sketch0::update);
        trace1.forEach(sketch1::update);

        // Compact the sketches (this makes them ready for similarity computation)
        CompactSketch cs1 = sketch0.compact();
        CompactSketch cs2 = sketch1.compact();

        // Compute the approximate Jaccard similarity
        double[] result = jaccard(cs1, cs2);
        double similarity = result[1];
        System.out.println("Jaccard similarity: " + similarity);
        return similarity;
    }

    // Create n-grams from the input list
    public static List<String> generateNGrams(List<String> trace, int n) {
        List<String> ngrams = new ArrayList<>();
        if (trace.size() < n) {
            return ngrams; // Not enough tokens to form an n-gram
        }
        for (int i = 0; i <= trace.size() - n; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < n; j++) {
                if (j > 0)
                    sb.append("-");
                sb.append(trace.get(i + j));
            }
            ngrams.add(sb.toString());
        }
        return ngrams;
    }

    // Compute Jaccard similarity for two sets of n-grams
    public static double jaccardSimilarity(Set<String> set1, Set<String> set2) {
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
}
