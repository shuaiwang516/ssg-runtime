package org.zlab.net.tracker;

import org.apache.commons.lang3.NotImplementedException;

import java.util.List;

public class DiffComputeEditDistance {
    public static int compute(Trace trace0, Trace trace1) {
        // diff 0 and 1
        // How to compute? suppose for a sequential list with multiple nodes...
        throw new NotImplementedException("Not implemented yet");
    }

    public static int[] compute(Trace trace0, Trace trace1, Trace trace2) {
        // diff 0 and 1
        // diff 1 and 2
        return new int[]{compute(trace0, trace1), compute(trace1, trace2)};
    }
}
