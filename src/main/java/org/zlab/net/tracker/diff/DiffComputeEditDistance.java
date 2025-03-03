package org.zlab.net.tracker.diff;

import org.zlab.net.tracker.Trace;
import org.zlab.net.tracker.Utils;

import java.util.List;

public class DiffComputeEditDistance {
    public static int compute(Trace trace0, Trace trace1) {
        List<String> list0 = trace0.getHashCodes();
        List<String> list1 = trace1.getHashCodes();
        return Utils.computeEditDistance(list0, list1);
    }

    public static int[] compute(Trace trace0, Trace trace1, Trace trace2) {
        // diff between 0 and 1
        // diff between 1 and 2
        return new int[]{compute(trace0, trace1), compute(trace1, trace2)};
    }
}
