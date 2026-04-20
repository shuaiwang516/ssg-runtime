package org.zlab.net.tracker.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.zlab.net.tracker.Trace;
import org.zlab.net.tracker.TraceEntry;
import org.zlab.net.tracker.classifier.ProtocolFamily;

/**
 * Phase 2 acceptance check: logical-flow overlap is preserved even when the raw
 * message keys drift between versions. Two synthetic lanes describe the same
 * HDFS {@code mkdirs} / {@code delete} workload — lane A carries v3.3.6 payload
 * classes and value hashes, lane B carries v2.10.2 equivalents. Raw canonical
 * keys diverge (they still embed the payload type / shape), but the
 * logical-flow view — keyed on stage, roles, family, and logical message id —
 * matches exactly.
 *
 * <p>
 * The test writes a CSV under
 * {@code ../agent/result/2026-04-19-phase-2-flow-smoke/} so the Phase 2 report
 * can cite the exact overlap numbers without re-running the test.
 */
public class LogicalFlowCrossVersionOverlapTest {

    private static final String STAGE = "POST_STAGE_1";

    private enum Event {
        MKDIRS("ClientNamenodeProtocol", "mkdirs", "call-1", "MkdirsRequestProto-v3",
                "MkdirsRequestProto-v2"), DELETE("ClientNamenodeProtocol", "delete", "call-2",
                        "DeleteRequestProto-v3", "DeleteRequestProto-v2"), RENAME(
                                "ClientNamenodeProtocol", "rename2", "call-3",
                                "RenameRequestProto-v3", "RenameRequestProto-v2"), HEARTBEAT(
                                        "DatanodeProtocol", "sendHeartbeat", null,
                                        "HeartbeatRequestProto-v3", "HeartbeatRequestProto-v2");

        final String service;
        final String method;
        final String logicalId;
        final String payloadA;
        final String payloadB;

        Event(String service, String method, String logicalId, String a, String b) {
            this.service = service;
            this.method = method;
            this.logicalId = logicalId;
            this.payloadA = a;
            this.payloadB = b;
        }
    }

    private static TraceEntry build(String payloadType, String logicalId, String service,
            String method) {
        return new TraceEntry(1, "rpc", 1, TraceEntry.EventType.SEND, false,
                System.currentTimeMillis(), System.nanoTime(), "client", "namenode", "client",
                "namenode", null, "hdfs-rpc", null, null, service, method, null, logicalId, null,
                null, -1, 0L, 0L, null, null, false, 0L, null, 0L, null, payloadType);
    }

    private static Trace trace(boolean newVersion) {
        Trace t = new Trace();
        for (Event e : Event.values()) {
            String payload = newVersion ? e.payloadA : e.payloadB;
            t.addEntry(build(payload, e.logicalId, e.service, e.method));
        }
        return t;
    }

    @Test
    public void flowOverlap_isPreserved_acrossDriftingPayloadHashes() throws IOException {
        Trace laneA = trace(true);
        Trace laneB = trace(false);

        FlowExtractionResult flowsA = TraceFlowExtractor.extract(STAGE, laneA,
                BoundaryOracle.NO_BOUNDARY);
        FlowExtractionResult flowsB = TraceFlowExtractor.extract(STAGE, laneB,
                BoundaryOracle.NO_BOUNDARY);

        // Both lanes produce the same flow multiset: 3 explicit-ID
        // request/response flows plus 1 heartbeat fallback flow.
        assertEquals(4, flowsA.flowCount());
        assertEquals(4, flowsB.flowCount());
        assertEquals(flowsA.flowMultiset(), flowsB.flowMultiset(),
                "explicit-ID and deterministic fallback keys match across drifting versions");

        // Family-level agreement: namespace-mutation family count is
        // identical, heartbeat drops into BACKGROUND on both lanes.
        Map<ProtocolFamily, Integer> famA = flowsA.familyMultiset();
        Map<ProtocolFamily, Integer> famB = flowsB.familyMultiset();
        assertEquals(famA, famB);
        assertEquals(Integer.valueOf(3), famA.get(ProtocolFamily.HDFS_CLIENT_NAMESPACE_MUTATION));
        assertEquals(Integer.valueOf(1), famA.get(ProtocolFamily.BACKGROUND));

        // Counters land in the explicit-ID tier for the 3 RPCs and the
        // deterministic-fallback tier for the 1 heartbeat.
        assertEquals(3, flowsA.flowsGroupedWithExplicitId());
        assertEquals(1, flowsA.flowsGroupedWithDeterministicFallback());

        // Jaccard agreement across the same-lane flow multisets: 1.0.
        double jaccard = jaccard(flowsA.flowMultiset(), flowsB.flowMultiset());
        assertEquals(1.0, jaccard);

        // Persist a small CSV for the Phase 2 report.
        Path outDir = Paths.get("..", "agent", "result", "2026-04-19-phase-2-flow-smoke");
        Files.createDirectories(outDir);
        Path out = outDir.resolve("cross_version_flow_overlap.csv");
        try (BufferedWriter w = Files.newBufferedWriter(out)) {
            w.write("lane,total_flows,explicit_id_flows,fallback_flows,family_namespace_mutation,family_background");
            w.newLine();
            writeRow(w, "hdfs_3_3_6", flowsA);
            writeRow(w, "hdfs_2_10_2", flowsB);
            w.write(String.format(Locale.ROOT, "%n# flow_multiset_jaccard=%.4f", jaccard));
            w.newLine();
        }
        assertTrue(Files.exists(out));
    }

    private static void writeRow(BufferedWriter w, String lane, FlowExtractionResult r)
            throws IOException {
        Map<ProtocolFamily, Integer> fam = r.familyMultiset();
        w.write(String.format(Locale.ROOT, "%s,%d,%d,%d,%d,%d", lane, r.flowCount(),
                r.flowsGroupedWithExplicitId(), r.flowsGroupedWithDeterministicFallback(),
                fam.getOrDefault(ProtocolFamily.HDFS_CLIENT_NAMESPACE_MUTATION, 0),
                fam.getOrDefault(ProtocolFamily.BACKGROUND, 0)));
        w.newLine();
    }

    private static double jaccard(Map<TraceFlowKey, Integer> a, Map<TraceFlowKey, Integer> b) {
        long inter = 0;
        long union = 0;
        for (TraceFlowKey key : a.keySet()) {
            int aa = a.getOrDefault(key, 0);
            int bb = b.getOrDefault(key, 0);
            inter += Math.min(aa, bb);
            union += Math.max(aa, bb);
        }
        for (TraceFlowKey key : b.keySet()) {
            if (!a.containsKey(key)) {
                union += b.get(key);
            }
        }
        return union == 0 ? 1.0 : (double) inter / union;
    }
}
