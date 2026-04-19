package org.zlab.net.tracker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.zlab.net.tracker.diff.DiffComputeSemanticSimilarity;

/**
 * Synthetic replay that checks the Phase 1 acceptance claim: the new
 * {@link CanonicalKeyMode#GUIDANCE} identity produces higher same-version
 * Jaccard agreement than the pre-Phase-1 {@link CanonicalKeyMode#SEMANTIC_SHAPE_SUMMARY}
 * tier when the two runs differ only in benign, within-version noise
 * (shape hashes drift because payload objects live at different addresses,
 * summaries drift because timestamps / counters differ, etc.).
 *
 * <p>The test is deterministic — no real cluster is involved. It also
 * writes a machine-readable CSV so the report / results tree can cite the
 * exact scores without re-running the test.
 */
public class GuidanceSameVersionAgreementTest {

    /**
     * Realistic Cassandra 4.x workload. Each entry describes one logical
     * event: verb, endpoint, and a pair of (shapeHash, summary) values —
     * lane A and lane B — representing the benign drift that appears
     * between two runs of the same version. Family classification is
     * independent of those drifting fields.
     */
    private enum Event {
        SCHEMA_PULL("SCHEMA_PULL_REQ", "node0", "node1", 0x11AAL, 0x11BBL,
                "SchemaPullRequest|cluster|1710001000", "SchemaPullRequest|cluster|1710001099"),
        SCHEMA_PULL_RSP("SCHEMA_PULL_RSP", "node1", "node0", 0x22AAL, 0x22BBL,
                "SchemaPullResponse|cluster|1710001100", "SchemaPullResponse|cluster|1710001188"),
        GOSSIP_SYN1("GOSSIP_DIGEST_SYN", "node0", "node1", 0x33AAL, 0x33BBL,
                "GossipDigestSyn|node0|ep=0xaaaa", "GossipDigestSyn|node0|ep=0xbbbb"),
        GOSSIP_SYN2("GOSSIP_DIGEST_SYN", "node1", "node2", 0x34AAL, 0x34BBL,
                "GossipDigestSyn|node1|ep=0xcccc", "GossipDigestSyn|node1|ep=0xdddd"),
        GOSSIP_SYN3("GOSSIP_DIGEST_SYN", "node0", "node2", 0x35AAL, 0x35BBL,
                "GossipDigestSyn|node0|ep=0xeeee", "GossipDigestSyn|node0|ep=0xffff"),
        HINT_REQ("HINT_REQ", "node0", "node1", 0x44AAL, 0x44BBL,
                "HintMessage|keyspace1|mutation-id=0xaaaa", "HintMessage|keyspace1|mutation-id=0xbbbb"),
        HINT_RSP("HINT_RSP", "node1", "node0", 0x45AAL, 0x45BBL,
                "HintResponse|keyspace1|ack=0xaaaa", "HintResponse|keyspace1|ack=0xbbbb"),
        PAXOS_PREPARE("PAXOS_PREPARE_REQ", "node0", "node1", 0x55AAL, 0x55BBL,
                "PaxosPrepare|ballot=0xdeadbeef", "PaxosPrepare|ballot=0xcafebabe"),
        PAXOS_PROPOSE("PAXOS_PROPOSE_REQ", "node0", "node1", 0x56AAL, 0x56BBL,
                "PaxosPropose|ballot=0xdeadbeef", "PaxosPropose|ballot=0xcafebabe"),
        REPAIR_SYNC("SYNC_REQ", "node0", "node1", 0x66AAL, 0x66BBL,
                "SyncRequest|session=0xaa11", "SyncRequest|session=0xbb22");

        final String verb;
        final String srcRole;
        final String dstRole;
        final long shapeA;
        final long shapeB;
        final String summaryA;
        final String summaryB;

        Event(String verb, String srcRole, String dstRole, long shapeA, long shapeB,
                String summaryA, String summaryB) {
            this.verb = verb;
            this.srcRole = srcRole;
            this.dstRole = dstRole;
            this.shapeA = shapeA;
            this.shapeB = shapeB;
            this.summaryA = summaryA;
            this.summaryB = summaryB;
        }
    }

    private static final Event[] WORKLOAD = Event.values();

    private static Trace buildLane(boolean laneB) {
        Trace trace = new Trace();
        for (Event event : WORKLOAD) {
            long shape = laneB ? event.shapeB : event.shapeA;
            String summary = laneB ? event.summaryB : event.summaryA;
            trace.addEntry(new TraceEntry(1, "synthetic-send", 1, TraceEntry.EventType.SEND,
                    /*changedMessage*/ false, System.currentTimeMillis(), System.nanoTime(),
                    /*nodeId*/ null, /*peerId*/ null, event.srcRole, event.dstRole,
                    /*channel*/ null, "cassandra", event.verb, /*messageVersion*/ null,
                    /*rpcService*/ null, /*rpcMethod*/ null, /*messageKind*/ null,
                    /*logicalMessageId*/ null, /*deliveryId*/ null, /*fanoutType*/ null, -1, shape,
                    0L, /*messageKey*/ null, summary, false, 0L, null, 0L, null,
                    /*payloadType*/ null));
        }
        return trace;
    }

    @Test
    public void guidanceAgreementExceedsSummaryAgreement() throws IOException {
        Trace laneA = buildLane(false);
        Trace laneB = buildLane(true);

        double guidanceJaccard = DiffComputeSemanticSimilarity.multisetJaccard(
                laneA.getCanonicalMultiset(CanonicalKeyMode.GUIDANCE),
                laneB.getCanonicalMultiset(CanonicalKeyMode.GUIDANCE));
        double summaryJaccard = DiffComputeSemanticSimilarity.multisetJaccard(
                laneA.getCanonicalMultiset(CanonicalKeyMode.SEMANTIC_SHAPE_SUMMARY),
                laneB.getCanonicalMultiset(CanonicalKeyMode.SEMANTIC_SHAPE_SUMMARY));

        // Acceptance bar: GUIDANCE collapses the benign shape / summary
        // drift to the same family-level identity, so the two same-version
        // lanes must agree perfectly. SEMANTIC_SHAPE_SUMMARY is sensitive
        // to those fields and must drift at least below GUIDANCE to
        // demonstrate the redesign is doing something load-bearing.
        assertEquals(1.0, guidanceJaccard, 1e-9,
                "GUIDANCE same-version Jaccard must be 1.0 under benign drift");
        assertTrue(summaryJaccard < guidanceJaccard,
                "SEMANTIC_SHAPE_SUMMARY must agree strictly less than GUIDANCE under shape/summary drift, got summary="
                        + summaryJaccard + " vs guidance=" + guidanceJaccard);

        // Emit a tiny results artifact so the Phase 1 report can cite the
        // numbers. Stored under the build tree so it does not leak into
        // the repo unless explicitly collected.
        Path out = Paths.get("build", "phase1-evidence", "same_version_agreement.csv");
        Files.createDirectories(out.getParent());
        try (BufferedWriter w = Files.newBufferedWriter(out)) {
            w.write("tier,same_version_jaccard");
            w.newLine();
            w.write(String.format(Locale.ROOT, "GUIDANCE,%.6f", guidanceJaccard));
            w.newLine();
            w.write(String.format(Locale.ROOT, "SEMANTIC_SHAPE_SUMMARY,%.6f", summaryJaccard));
            w.newLine();
        }
    }
}
