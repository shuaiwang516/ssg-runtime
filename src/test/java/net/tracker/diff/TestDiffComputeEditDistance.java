package net.tracker.diff;

import org.junit.jupiter.api.Test;
import org.zlab.net.tracker.diff.DiffComputeEditDistance;
import org.zlab.net.tracker.Trace;

public class TestDiffComputeEditDistance {
    @Test
    public void testEditDistance() {
        Trace trace0 = new Trace();
        Trace trace1 = new Trace();

        trace0.record("sendRR", 1);
        trace0.record("sendRR", 2);
        trace0.record("sendAll", 3);

        trace1.record("sendRR", 1);
        trace1.record("sendAll", 2);
        trace1.record("sendRR", 3);

        assert DiffComputeEditDistance.compute(trace0, trace1) == 2;
    }
}
