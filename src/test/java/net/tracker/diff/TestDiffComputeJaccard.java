package net.tracker.diff;

import org.junit.jupiter.api.Test;
import org.zlab.net.tracker.Trace;
import org.zlab.net.tracker.diff.DiffComputeJaccardSimilarity;

public class TestDiffComputeJaccard {
    @Test
    public void testJaccard() {
        // When useNGrams: 0.33
        // When !useNGrams: 1.0
        Trace trace0 = new Trace();
        Trace trace1 = new Trace();

        trace0.record("sendRR", 1);
        trace0.record("sendRR", 2);
        trace0.record("sendAll", 3);

        trace1.record("sendRR", 1);
        trace1.record("sendAll", 2);
        trace1.record("sendRR", 3);

        boolean log = true;
        if (log) {
            trace0.print();
            System.out.println("----");
            trace1.print();
        }

        double similarity = DiffComputeJaccardSimilarity.compute(trace0, trace1);
        // System.out.println("Jaccard similarity: " + similarity);
    }
}
