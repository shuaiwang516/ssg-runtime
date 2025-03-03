package org.zlab.net.tracker.diff;

import org.apache.datasketches.theta.CompactSketch;
import org.apache.datasketches.theta.UpdateSketch;
import org.zlab.net.tracker.Trace;

import static org.apache.datasketches.theta.JaccardSimilarity.jaccard;

public class DiffComputeJaccardSimilarity {
    public static double compute(Trace trace0, Trace trace1) {
        // Create a sketch for each trace
        UpdateSketch sketch0 = UpdateSketch.builder().build();
        UpdateSketch sketch1 = UpdateSketch.builder().build();

        // Update sketches with the trace values
        trace0.getHashCodes().forEach(sketch0::update);
        trace1.getHashCodes().forEach(sketch1::update);

        // Compact the sketches (this makes them ready for similarity computation)
        CompactSketch cs1 = sketch0.compact();
        CompactSketch cs2 = sketch1.compact();

        // Compute the approximate Jaccard similarity
        double[] result = jaccard(cs1, cs2);
        double similarity = result[1];
        System.out.println("Jaccard similarity: " + similarity);
        return similarity;
    }

    public static double[] compute(Trace trace0, Trace trace1, Trace trace2) {
        // diff between 0 and 1
        // diff between 1 and 2
        return new double[]{compute(trace0, trace1), compute(trace1, trace2)};
    }
}
