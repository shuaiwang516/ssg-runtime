package org.zlab.net.tracker.diff;

import org.zlab.net.tracker.Trace;
import org.zlab.net.tracker.TraceEntry;

import java.util.*;

/**
 * Best-effort order-preserving similarity metric for diagnostic purposes. This
 * does NOT gate corpus admission.
 *
 * Algorithm: 1. Partition trace entries by flow key
 * (canonicalSrcRole->canonicalDstRole). 2. Build per-flow sequence of
 * semanticType() values. 3. Collapse consecutive duplicates. 4. Compare
 * per-flow sequences across traces via normalized LCS. 5. Aggregate: average
 * across shared flows.
 */
public final class DiffComputeCompressedOrder {
    private DiffComputeCompressedOrder() {
    }

    /**
     * Compute 3-way compressed per-flow order similarity. Returns double[3]:
     * [OO-RO, RO-NN, OO-NN].
     */
    public static double[] compute(Trace trace0, Trace trace1, Trace trace2) {
        return new double[]{compute(trace0, trace1), compute(trace1, trace2),
                compute(trace0, trace2)};
    }

    /**
     * Compute compressed per-flow order similarity between two traces. Returns
     * average normalized LCS across shared flows.
     */
    public static double compute(Trace trace0, Trace trace1) {
        Map<String, List<String>> flows0 = buildCompressedFlows(trace0);
        Map<String, List<String>> flows1 = buildCompressedFlows(trace1);

        Set<String> sharedFlows = new HashSet<>(flows0.keySet());
        sharedFlows.retainAll(flows1.keySet());

        if (sharedFlows.isEmpty()) {
            // Two empty traces are identical — score 1.0.
            // Only score 0.0 when one side has flows the other lacks.
            return flows0.isEmpty() && flows1.isEmpty() ? 1.0 : 0.0;
        }

        double totalSim = 0;
        for (String flow : sharedFlows) {
            List<String> seq0 = flows0.get(flow);
            List<String> seq1 = flows1.get(flow);
            int lcsLen = lcsLength(seq0, seq1);
            int maxLen = Math.max(seq0.size(), seq1.size());
            totalSim += maxLen > 0 ? (double) lcsLen / maxLen : 1.0;
        }
        return totalSim / sharedFlows.size();
    }

    static Map<String, List<String>> buildCompressedFlows(Trace trace) {
        Map<String, List<String>> flows = new LinkedHashMap<>();
        if (trace == null)
            return flows;

        for (TraceEntry entry : trace.getTraceEntries()) {
            if (entry.eventType == TraceEntry.EventType.RECV_END)
                continue;
            String flowKey = entry.canonicalEndpointKey();
            String type = entry.semanticType();

            flows.computeIfAbsent(flowKey, k -> new ArrayList<>());
            List<String> seq = flows.get(flowKey);

            // Collapse consecutive duplicates
            if (seq.isEmpty() || !seq.get(seq.size() - 1).equals(type)) {
                seq.add(type);
            }
        }
        return flows;
    }

    static int lcsLength(List<String> a, List<String> b) {
        int m = a.size(), n = b.size();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (a.get(i - 1).equals(b.get(j - 1))) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        return dp[m][n];
    }
}
