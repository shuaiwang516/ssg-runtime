package org.zlab.net.tracker.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.zlab.net.tracker.Trace;
import org.zlab.net.tracker.TraceEntry;
import org.zlab.net.tracker.classifier.ProtocolFamily;

/**
 * Test suite for {@link TraceFlowExtractor}, covering the Phase 2 logical-flow
 * reconstruction rules: explicit-ID grouping, deterministic fallback bucketing,
 * stage-local boundaries, and per-system shape expectations for Cassandra
 * generic responses, HDFS call-id joins, and HBase scan/mutate flows.
 */
public class TraceFlowExtractorTest {

    private static final String STAGE = "POST_STAGE_1";

    // Helpers -----------------------------------------------------------------

    private static TraceEntry sendEntry(String nodeRole, String peerRole, String protocol,
            String messageType, String rpcService, String rpcMethod, String messageKind,
            String logicalMessageId, String deliveryId, long tsNanos) {
        return new TraceEntry(1, "send", 1, TraceEntry.EventType.SEND, false,
                System.currentTimeMillis(), tsNanos, nodeRole, peerRole, nodeRole, peerRole, null,
                protocol, messageType, null, rpcService, rpcMethod, messageKind, logicalMessageId,
                deliveryId, null, -1, 0L, 0L, null, null, false, 0L, null, 0L, null, null);
    }

    private static TraceEntry recvEntry(String nodeRole, String peerRole, String protocol,
            String messageType, String rpcService, String rpcMethod, String messageKind,
            String logicalMessageId, String deliveryId, long tsNanos) {
        return new TraceEntry(1, "recv", 1, TraceEntry.EventType.RECV_BEGIN, false,
                System.currentTimeMillis(), tsNanos, nodeRole, peerRole, nodeRole, peerRole, null,
                protocol, messageType, null, rpcService, rpcMethod, messageKind, logicalMessageId,
                deliveryId, null, -1, 0L, 0L, null, null, false, 0L, null, 0L, null, null);
    }

    private static Trace buildTrace(TraceEntry... entries) {
        Trace trace = new Trace();
        for (TraceEntry entry : entries) {
            trace.addEntry(entry);
        }
        return trace;
    }

    // --- Explicit logical-message-id grouping -------------------------------

    @Test
    public void sendAndReceive_sharingLogicalMessageId_groupIntoOneFlow() {
        TraceEntry send = sendEntry("client", "namenode", "hdfs-rpc", null,
                "ClientNamenodeProtocol", "mkdirs", null, "call-42", null, 1L);
        TraceEntry recv = recvEntry("namenode", "client", "hdfs-rpc", null,
                "ClientNamenodeProtocol", "mkdirs", null, "call-42", null, 2L);
        FlowExtractionResult result = TraceFlowExtractor.extract(STAGE, buildTrace(send, recv),
                BoundaryOracle.NO_BOUNDARY);

        assertEquals(1, result.flowCount(),
                "send+recv with same logicalMessageId must form one flow");
        assertEquals(1, result.flowsGroupedWithExplicitId());
        assertEquals(0, result.flowsGroupedWithDeterministicFallback());

        TraceFlowSummary flow = result.flows().get(0);
        assertTrue(flow.hasSend());
        assertTrue(flow.hasReceive());
        assertEquals(2, flow.eventCount());
        assertEquals(CorrelationSource.EXPLICIT_LOGICAL_ID, flow.key().correlationSource);
        assertEquals("call-42", flow.key().correlationKey);
        assertEquals(ProtocolFamily.HDFS_CLIENT_NAMESPACE_MUTATION, flow.protocolFamily());
    }

    @Test
    public void explicitDeliveryId_groupsSendAndReceive_whenLogicalIdMissing() {
        TraceEntry send = sendEntry("client", "namenode", "hdfs-rpc", null,
                "ClientNamenodeProtocol", "delete", null, null, "delivery-9", 1L);
        TraceEntry recv = recvEntry("namenode", "client", "hdfs-rpc", null,
                "ClientNamenodeProtocol", "delete", null, null, "delivery-9", 2L);
        FlowExtractionResult result = TraceFlowExtractor.extract(STAGE, buildTrace(send, recv),
                BoundaryOracle.NO_BOUNDARY);

        assertEquals(1, result.flowCount());
        TraceFlowSummary flow = result.flows().get(0);
        assertEquals(CorrelationSource.EXPLICIT_DELIVERY_ID, flow.key().correlationSource);
        assertEquals("delivery-9", flow.key().correlationKey);
    }

    // --- Missing IDs fall back deterministically ----------------------------

    @Test
    public void missingIds_fallBackDeterministically() {
        TraceEntry send1 = sendEntry("node0", "node1", "cassandra", "GOSSIP_DIGEST_SYN", null, null,
                null, null, null, 1L);
        TraceEntry send2 = sendEntry("node0", "node1", "cassandra", "GOSSIP_DIGEST_SYN", null, null,
                null, null, null, 2L);
        TraceEntry send3 = sendEntry("node0", "node1", "cassandra", "GOSSIP_DIGEST_SYN", null, null,
                null, null, null, 3L);
        FlowExtractionResult result = TraceFlowExtractor.extract(STAGE,
                buildTrace(send1, send2, send3), BoundaryOracle.NO_BOUNDARY);

        assertEquals(1, result.flowCount(),
                "3 adjacent gossip sends should collapse into one bucket");
        assertEquals(0, result.flowsGroupedWithExplicitId());
        assertEquals(1, result.flowsGroupedWithDeterministicFallback());
        TraceFlowSummary flow = result.flows().get(0);
        assertEquals(CorrelationSource.DETERMINISTIC_FALLBACK, flow.key().correlationSource);
        assertEquals(3, flow.eventCount());
    }

    @Test
    public void deterministicFallbackBucket_opensNewBucketAfterSizeExceeded() {
        Trace trace = new Trace();
        for (int i = 0; i < 5; i++) {
            trace.addEntry(sendEntry("node0", "node1", "cassandra", "GOSSIP_DIGEST_SYN", null, null,
                    null, null, null, i));
        }
        // Force a small bucket size so the bounded property is visible
        // without shipping an artificial 10k-entry synthetic workload.
        FlowExtractionResult result = TraceFlowExtractor.extract(STAGE, trace,
                BoundaryOracle.NO_BOUNDARY, /* fallbackBucketSize */ 2);

        assertEquals(3, result.flowCount(), "5 entries at bucket=2 split into 3 fallback buckets");
        for (TraceFlowSummary flow : result.flows()) {
            assertEquals(CorrelationSource.DETERMINISTIC_FALLBACK, flow.key().correlationSource);
        }
    }

    @Test
    public void fallbackKeys_differInvariantsSeparate() {
        TraceEntry sendA = sendEntry("node0", "node1", "cassandra", "GOSSIP_DIGEST_SYN", null, null,
                null, null, null, 1L);
        TraceEntry sendB = sendEntry("node0", "node2", "cassandra", "GOSSIP_DIGEST_SYN", null, null,
                null, null, null, 2L);
        FlowExtractionResult result = TraceFlowExtractor.extract(STAGE, buildTrace(sendA, sendB),
                BoundaryOracle.NO_BOUNDARY);

        assertEquals(2, result.flowCount(),
                "fallback keys with different dst role must not collapse");
        assertNotEquals(result.flows().get(0).key(), result.flows().get(1).key());
    }

    // --- Synthesized delivery alias must not outrank explicit logical id ----

    @Test
    public void synthesizedDeliveryAlias_isDemoted_evenWhenBothLanesCarryIt() {
        // Bridge fallback emits "<logicalId>@<peer>" when the wire
        // protocol has no native delivery token. Two entries that
        // *would* share the same synthesized alias across SEND / RECV
        // must not rely on that alias for cross-lane matching.
        TraceEntry send = sendEntry("node0", "node1", "cassandra", "SCHEMA_PULL_REQ", null, null,
                null, null, "sig-1@node1", 1L);
        TraceEntry recv = recvEntry("node1", "node0", "cassandra", "SCHEMA_PULL_REQ", null, null,
                null, null, "sig-1@node0", 2L);
        FlowExtractionResult result = TraceFlowExtractor.extract(STAGE, buildTrace(send, recv),
                BoundaryOracle.NO_BOUNDARY);

        // Because the peer-decorated alias is per-side different, both
        // entries fall back to deterministic grouping. They land in the
        // same tuple (same family, same role pair), so they end up in
        // one fallback flow — never in an explicit-id flow.
        assertEquals(0, result.flowsGroupedWithExplicitId());
        assertEquals(1, result.flowsGroupedWithDeterministicFallback());
        assertEquals(1, result.flowCount());
        TraceFlowSummary flow = result.flows().get(0);
        assertEquals(CorrelationSource.DETERMINISTIC_FALLBACK, flow.key().correlationSource);
        assertTrue(flow.hasSend());
        assertTrue(flow.hasReceive());
    }

    @Test
    public void logicalId_alwaysWinsOverDeliveryId() {
        TraceEntry send = sendEntry("node0", "node1", "hdfs-rpc", null, "ClientNamenodeProtocol",
                "create", null, "call-7", "delivery-7@node1", 1L);
        TraceEntry recv = recvEntry("node1", "node0", "hdfs-rpc", null, "ClientNamenodeProtocol",
                "create", null, "call-7", "delivery-7@node0", 2L);
        FlowExtractionResult result = TraceFlowExtractor.extract(STAGE, buildTrace(send, recv),
                BoundaryOracle.NO_BOUNDARY);

        assertEquals(1, result.flowsGroupedWithExplicitId());
        TraceFlowSummary flow = result.flows().get(0);
        assertEquals(CorrelationSource.EXPLICIT_LOGICAL_ID, flow.key().correlationSource);
        assertEquals("call-7", flow.key().correlationKey);
    }

    // --- Boundary propagation -----------------------------------------------

    @Test
    public void boundaryOracle_crossingStampsFlowAsCrossing() {
        TraceEntry send = sendEntry("N0", "N1", "hdfs-rpc", null, "ClientNamenodeProtocol",
                "mkdirs", null, "call-55", null, 1L);
        TraceEntry recv = recvEntry("N1", "N0", "hdfs-rpc", null, "ClientNamenodeProtocol",
                "mkdirs", null, "call-55", null, 2L);
        BoundaryOracle crossing = entry -> FlowBoundaryStatus.CROSSING;
        FlowExtractionResult result = TraceFlowExtractor.extract(STAGE, buildTrace(send, recv),
                crossing);

        assertEquals(1, result.flowCount());
        assertEquals(1, result.boundaryInvolvedFlowCount());
        assertEquals(0, result.roleAmbiguousBoundaryFlowCount());
        TraceFlowSummary flow = result.flows().get(0);
        assertTrue(flow.boundaryInvolved());
        assertFalse(flow.roleAmbiguousBoundary());
    }

    @Test
    public void boundaryOracle_roleAmbiguousPropagatesSeparately() {
        TraceEntry send = sendEntry("node0", "node1", "cassandra", "GOSSIP_DIGEST_SYN", null, null,
                null, null, null, 1L);
        BoundaryOracle ambiguous = entry -> FlowBoundaryStatus.ROLE_AMBIGUOUS;
        FlowExtractionResult result = TraceFlowExtractor.extract(STAGE, buildTrace(send),
                ambiguous);

        assertEquals(1, result.roleAmbiguousBoundaryFlowCount());
        assertEquals(0, result.boundaryInvolvedFlowCount());
        assertEquals(FlowBoundaryStatus.ROLE_AMBIGUOUS, result.flows().get(0).boundaryStatus());
    }

    @Test
    public void boundaryCombination_followsSeverityOrdering() {
        TraceEntry first = sendEntry("node0", "node1", "hdfs-rpc", null, "ClientNamenodeProtocol",
                "mkdirs", null, "call-9", null, 1L);
        TraceEntry second = recvEntry("node1", "node0", "hdfs-rpc", null, "ClientNamenodeProtocol",
                "mkdirs", null, "call-9", null, 2L);
        BoundaryOracle mixed = entry -> entry.eventType == TraceEntry.EventType.SEND
                ? FlowBoundaryStatus.ROLE_AMBIGUOUS
                : FlowBoundaryStatus.CROSSING;
        FlowExtractionResult result = TraceFlowExtractor.extract(STAGE, buildTrace(first, second),
                mixed);

        assertEquals(FlowBoundaryStatus.CROSSING, result.flows().get(0).boundaryStatus(),
                "CROSSING must win over ROLE_AMBIGUOUS when both occur");
    }

    // --- Flow grouping stays stage-local ------------------------------------

    @Test
    public void flowGrouping_isStageLocal_forIdenticalContents() {
        TraceEntry send = sendEntry("client", "namenode", "hdfs-rpc", null,
                "ClientNamenodeProtocol", "mkdirs", null, "call-1", null, 1L);
        FlowExtractionResult stageA = TraceFlowExtractor.extract("POST_STAGE_1", buildTrace(send),
                BoundaryOracle.NO_BOUNDARY);
        FlowExtractionResult stageB = TraceFlowExtractor.extract("POST_STAGE_2", buildTrace(send),
                BoundaryOracle.NO_BOUNDARY);

        assertNotEquals(stageA.flows().get(0).key(), stageB.flows().get(0).key());
        assertEquals("POST_STAGE_1", stageA.flows().get(0).key().stage);
        assertEquals("POST_STAGE_2", stageB.flows().get(0).key().stage);
    }

    // --- Cassandra generic response stays in deterministic fallback ---------

    @Test
    public void cassandraGenericResponse_withoutLogicalId_staysInFallback() {
        TraceEntry resp = sendEntry("node1", "node0", "cassandra", "REQUEST_RESPONSE", null, null,
                null, null, null, 1L);
        FlowExtractionResult result = TraceFlowExtractor.extract(STAGE, buildTrace(resp),
                BoundaryOracle.NO_BOUNDARY);

        TraceFlowSummary flow = result.flows().get(0);
        assertEquals(CorrelationSource.DETERMINISTIC_FALLBACK, flow.key().correlationSource,
                "generic REQUEST_RESPONSE events without ids stay in fallback");
        assertEquals(ProtocolFamily.BACKGROUND, flow.protocolFamily());
    }

    // --- HBase scanner flow uses scanner id when present --------------------

    @Test
    public void hbaseScanTraffic_withScannerId_staysInOneLogicalFlow() {
        TraceEntry scanStart = sendEntry("client", "regionserver", "hbase", null, "ClientService",
                "Scan", null, "scanner-101", null, 1L);
        TraceEntry scanNext1 = sendEntry("client", "regionserver", "hbase", null, "ClientService",
                "Scan", null, "scanner-101", null, 2L);
        TraceEntry scanNext2 = recvEntry("regionserver", "client", "hbase", null, "ClientService",
                "Scan", null, "scanner-101", null, 3L);
        FlowExtractionResult result = TraceFlowExtractor.extract(STAGE,
                buildTrace(scanStart, scanNext1, scanNext2), BoundaryOracle.NO_BOUNDARY);

        assertEquals(1, result.flowCount(),
                "all 3 scanner-101 events must form a single scan flow");
        TraceFlowSummary flow = result.flows().get(0);
        assertEquals(CorrelationSource.EXPLICIT_LOGICAL_ID, flow.key().correlationSource);
        assertEquals("scanner-101", flow.key().correlationKey);
        assertEquals(3, flow.eventCount());
    }

    // --- HBase mutate subtype survives inside Mutate flow summary -----------

    @Test
    public void hbaseMutate_subtypeDetailPropagatesIntoFlowSummary() {
        TraceEntry mutPut = sendEntry("client", "regionserver", "hbase", null, "ClientService",
                "Mutate", "PUT", "call-10", null, 1L);
        TraceEntry mutDel = sendEntry("client", "regionserver", "hbase", null, "ClientService",
                "Mutate", "DELETE", "call-10", null, 2L);
        FlowExtractionResult result = TraceFlowExtractor.extract(STAGE, buildTrace(mutPut, mutDel),
                BoundaryOracle.NO_BOUNDARY);

        assertEquals(1, result.flowCount());
        TraceFlowSummary flow = result.flows().get(0);
        Map<String, Integer> labels = flow.detailLabelCounts();
        assertEquals(Integer.valueOf(1), labels.get("ClientService#Mutate[PUT]"));
        assertEquals(Integer.valueOf(1), labels.get("ClientService#Mutate[DELETE]"));
        assertEquals(ProtocolFamily.HBASE_CLIENT_MUTATION_OR_MULTI, flow.protocolFamily());

        // The Mutate family's aggregate detail histogram on the result
        // should expose the same per-subtype counts so the server can
        // render "top divergent service or method labels inside a
        // family" directly.
        Map<String, Integer> familyLabels = result
                .detailLabelCounts(ProtocolFamily.HBASE_CLIENT_MUTATION_OR_MULTI);
        assertEquals(Integer.valueOf(1), familyLabels.get("ClientService#Mutate[PUT]"));
        assertEquals(Integer.valueOf(1), familyLabels.get("ClientService#Mutate[DELETE]"));
    }

    // --- Result shape: family multiset, flow multiset, order summary --------

    @Test
    public void resultExposesCompositeSummaries() {
        TraceEntry schema1 = sendEntry("node0", "node1", "cassandra", "SCHEMA_PULL_REQ", null, null,
                null, "schema-1", null, 1L);
        TraceEntry schema2 = recvEntry("node1", "node0", "cassandra", "SCHEMA_PULL_REQ", null, null,
                null, "schema-1", null, 2L);
        TraceEntry gossip = sendEntry("node0", "node1", "cassandra", "GOSSIP_DIGEST_SYN", null,
                null, null, null, null, 3L);
        FlowExtractionResult result = TraceFlowExtractor.extract(STAGE,
                buildTrace(schema1, schema2, gossip), BoundaryOracle.NO_BOUNDARY);

        assertEquals(2, result.flowCount());
        Map<ProtocolFamily, Integer> families = result.familyMultiset();
        assertEquals(Integer.valueOf(2), families.get(ProtocolFamily.CASSANDRA_SCHEMA_SYNC));
        assertEquals(Integer.valueOf(1), families.get(ProtocolFamily.BACKGROUND));

        // Phase 3: compressed family order is per-role-pair so the
        // scorer can compare local contexts instead of one global
        // interleaving. The schema exchange shows up on "node0->node1"
        // (the SEND) and on "node0->node1" again via the recv entry's
        // normalized src/dst (peerRole->nodeRole). The gossip SEND lands
        // on the same pair. Consecutive duplicates are collapsed.
        Map<String, List<ProtocolFamily>> perPair = result.perRolePairCompressedFamilyOrder();
        List<ProtocolFamily> forwardPair = perPair.get("node0->node1");
        assertNotNull(forwardPair, "SEND role pair must appear");
        assertEquals(2, forwardPair.size(),
                "schema then gossip collapses to two entries on the forward pair");
        assertEquals(ProtocolFamily.CASSANDRA_SCHEMA_SYNC, forwardPair.get(0));
        assertEquals(ProtocolFamily.BACKGROUND, forwardPair.get(1));
    }

    @Test
    public void recvEnd_isExcludedFromExtraction() {
        TraceEntry send = sendEntry("node0", "node1", "cassandra", "GOSSIP_DIGEST_SYN", null, null,
                null, null, null, 1L);
        TraceEntry recvBegin = recvEntry("node1", "node0", "cassandra", "GOSSIP_DIGEST_SYN", null,
                null, null, null, null, 2L);
        TraceEntry recvEnd = new TraceEntry(1, "recv", 1, TraceEntry.EventType.RECV_END, false,
                System.currentTimeMillis(), 3L, "node1", "node0", "node1", "node0", null,
                "cassandra", "GOSSIP_DIGEST_SYN", null, null, null, null, null, null, null, -1, 0L,
                0L, null, null, false, 0L, null, 0L, null, null);
        FlowExtractionResult result = TraceFlowExtractor.extract(STAGE,
                buildTrace(send, recvBegin, recvEnd), BoundaryOracle.NO_BOUNDARY);

        assertEquals(2, result.flows().get(0).eventCount(), "RECV_END must not be counted");
    }

    @Test
    public void emptyTrace_returnsEmptyResult() {
        FlowExtractionResult result = TraceFlowExtractor.extract(STAGE, new Trace(),
                BoundaryOracle.NO_BOUNDARY);
        assertEquals(0, result.flowCount());
        assertTrue(result.familyMultiset().isEmpty());
        assertEquals(FlowExtractionResult.empty().flowCount(), result.flowCount());
    }

    @Test
    public void nullTrace_returnsEmptyResult() {
        FlowExtractionResult result = TraceFlowExtractor.extract(STAGE, null,
                BoundaryOracle.NO_BOUNDARY);
        assertEquals(0, result.flowCount());
    }

    @Test
    public void unknownBoundaryOracle_stampsFlowsAsUnresolved() {
        TraceEntry send = sendEntry("node0", "node1", "cassandra", "GOSSIP_DIGEST_SYN", null, null,
                null, null, null, 1L);
        FlowExtractionResult result = TraceFlowExtractor.extract(STAGE, buildTrace(send),
                BoundaryOracle.UNKNOWN);
        assertEquals(FlowBoundaryStatus.UNRESOLVED, result.flows().get(0).boundaryStatus());
        assertEquals(1, result.unresolvedBoundaryFlowCount());
    }

    @Test
    public void detailLabel_forCassandraGossip_isVerbPrefix() {
        TraceEntry send = sendEntry("node0", "node1", "cassandra", "GOSSIP_DIGEST_SYN", null, null,
                null, null, null, 1L);
        FlowExtractionResult result = TraceFlowExtractor.extract(STAGE, buildTrace(send),
                BoundaryOracle.NO_BOUNDARY);
        TraceFlowSummary flow = result.flows().get(0);
        assertEquals("verb:GOSSIP_DIGEST_SYN", flow.topDetailLabel());
    }

    @Test
    public void groupingFailure_isCountedSeparately_whenNoMetadataAtAll() {
        // SEND with no verb, no rpc info, no ids — the classifier has
        // absolutely nothing to latch onto.
        TraceEntry blank = new TraceEntry(1, "blank", 1, TraceEntry.EventType.SEND, false,
                System.currentTimeMillis(), 1L, "node0", "node1", "node0", "node1", null, null,
                null, null, null, null, null, null, null, null, -1, 0L, 0L, null, null, false, 0L,
                null, 0L, null, null);
        FlowExtractionResult result = TraceFlowExtractor.extract(STAGE, buildTrace(blank),
                BoundaryOracle.NO_BOUNDARY);
        assertEquals(0, result.flowsGroupedWithExplicitId());
        assertEquals(0, result.flowsGroupedWithDeterministicFallback());
        assertEquals(1, result.flowsGroupingFailed());
        assertEquals(CorrelationSource.GROUPING_FAILED,
                result.flows().get(0).key().correlationSource);
    }

    @Test
    public void explicitFlow_andFallbackFlow_neverMerge_evenWhenFamilyMatches() {
        TraceEntry explicit = sendEntry("node0", "node1", "cassandra", "SCHEMA_PULL_REQ", null,
                null, null, "schema-42", null, 1L);
        TraceEntry fallback = sendEntry("node0", "node1", "cassandra", "SCHEMA_PULL_REQ", null,
                null, null, null, null, 2L);
        FlowExtractionResult result = TraceFlowExtractor.extract(STAGE,
                buildTrace(explicit, fallback), BoundaryOracle.NO_BOUNDARY);

        assertEquals(2, result.flowCount(), "explicit-id and fallback must not share a key");
        assertNotNull(result.flows().get(0));
        assertNotNull(result.flows().get(1));
        boolean sawExplicit = false;
        boolean sawFallback = false;
        for (TraceFlowSummary flow : result.flows()) {
            if (flow.key().correlationSource == CorrelationSource.EXPLICIT_LOGICAL_ID) {
                sawExplicit = true;
            }
            if (flow.key().correlationSource == CorrelationSource.DETERMINISTIC_FALLBACK) {
                sawFallback = true;
            }
        }
        assertTrue(sawExplicit);
        assertTrue(sawFallback);
    }

    @Test
    public void nullBoundaryOracle_degradesToUnknown() {
        TraceEntry send = sendEntry("node0", "node1", "cassandra", "SCHEMA_PULL_REQ", null, null,
                null, "schema-1", null, 1L);
        FlowExtractionResult result = TraceFlowExtractor.extract(STAGE, buildTrace(send), null);
        assertEquals(FlowBoundaryStatus.UNRESOLVED, result.flows().get(0).boundaryStatus());
    }

    @Test
    public void keyToken_and_detailLabel_helpers() {
        TraceFlowKey key = new TraceFlowKey("POST_STAGE_1", "client", "nn",
                ProtocolFamily.HDFS_CLIENT_NAMESPACE_MUTATION, "call-1",
                CorrelationSource.EXPLICIT_LOGICAL_ID);
        assertTrue(key.hasExplicitCorrelation());
        assertEquals(
                "POST_STAGE_1|client->nn|HDFS_CLIENT_NAMESPACE_MUTATION|EXPLICIT_LOGICAL_ID|call-1",
                key.token());

        assertNull(TraceFlowSummary.buildDetailLabel(null, null, null, null));
        assertEquals("svc#method", TraceFlowSummary.buildDetailLabel("svc", "method", null, null));
        assertEquals("method[PUT]", TraceFlowSummary.buildDetailLabel(null, "method", null, "PUT"));
        assertEquals("verb:GOSSIP", TraceFlowSummary.buildDetailLabel(null, null, "GOSSIP", null));
        assertEquals("kind:PUT", TraceFlowSummary.buildDetailLabel(null, null, null, "PUT"));
    }
}
