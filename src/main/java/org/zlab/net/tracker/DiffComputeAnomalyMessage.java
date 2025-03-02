package org.zlab.net.tracker;

import org.apache.commons.lang3.NotImplementedException;

public class DiffComputeAnomalyMessage {
    public static void compute(Trace trace0, Trace trace1) {
        // identify messages that only exist in trace1
        throw new NotImplementedException("Not implemented yet");
    }

    public static void compute(Trace trace0, Trace trace1, Trace trace2) {
        // messages that only exist in trace0, not in others
        // messages that only exist in trace1, not in others
        // messages that only exist in trace2, not in others
        // message that exist in 0 and 2 but not in 1
        throw new NotImplementedException("Not implemented yet");
    }
}
