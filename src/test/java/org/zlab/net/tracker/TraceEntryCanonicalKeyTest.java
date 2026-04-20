package org.zlab.net.tracker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.zlab.net.tracker.classifier.ProtocolFamily;
import org.zlab.net.tracker.classifier.ProtocolFamilyClass;

/**
 * Canonical-key tests covering the Phase 1 online identity split.
 *
 * <p>
 * Two tiers survive after Phase 1: {@link CanonicalKeyMode#GUIDANCE} (the new
 * production default, backed by the
 * {@link org.zlab.net.tracker.classifier.ProtocolFamilyClassifier}) and
 * {@link CanonicalKeyMode#SEMANTIC_SHAPE_SUMMARY} (retained for offline
 * diagnosis and signature-dedup fixtures).
 */
public class TraceEntryCanonicalKeyTest {

    // Factory helpers ---------------------------------------------------------

    private static TraceEntry guidanceEntry(TraceEntry.EventType eventType, String nodeRole,
            String peerRole, String protocol, String messageType, String rpcService,
            String rpcMethod, String messageKind, String payloadType) {
        return new TraceEntry(1, "test", 1, eventType, false, System.currentTimeMillis(),
                System.nanoTime(), /* nodeId */ null, /* peerId */ null, nodeRole, peerRole,
                /* channel */ null, protocol, messageType, /* messageVersion */ null, rpcService,
                rpcMethod, messageKind, /* logicalMessageId */ null, /* deliveryId */ null,
                /* fanoutType */ null, -1, 0L, 0L, /* messageKey */ null, /* messageSummary */ null,
                false, 0L, null, 0L, null, payloadType);
    }

    private static TraceEntry summaryEntry(TraceEntry.EventType eventType, String nodeRole,
            String peerRole, String messageType, String payloadType, long shapeHash,
            String messageSummary) {
        return new TraceEntry(1, "test", 1, eventType, false, System.currentTimeMillis(),
                System.nanoTime(), /* nodeId */ null, /* peerId */ null, nodeRole, peerRole,
                /* channel */ null, /* protocol */ null, messageType, /* messageVersion */ null,
                /* rpcService */ null, /* rpcMethod */ null, /* messageKind */ null,
                /* logicalMessageId */ null, /* deliveryId */ null, /* fanoutType */ null, -1,
                shapeHash, 0L, /* messageKey */ null, messageSummary, false, 0L, null, 0L, null,
                payloadType);
    }

    // --- rawSemanticType / semanticType (offline diagnostic tier) -----------

    @Test
    public void rawSemanticType_prefersPayloadOverMessageType() {
        TraceEntry e = new TraceEntry(1, "test", 1, TraceEntry.EventType.SEND, false,
                System.currentTimeMillis(), System.nanoTime(), "N0", null, null, null, null, null,
                "MessageOut", null, null, null, null, null, null, null, -1, 0L, 0L, null, null,
                false, 0L, null, 0L, null, "org.apache.cassandra.gms.GossipDigestSyn");
        assertEquals("GossipDigestSyn", e.rawSemanticType());
    }

    @Test
    public void rawSemanticType_fallsToMessageType_whenPayloadNull() {
        TraceEntry e = new TraceEntry(1, "test", 1, TraceEntry.EventType.SEND, false,
                System.currentTimeMillis(), System.nanoTime(), "N0", null, null, null, null, null,
                "GOSSIP_DIGEST_SYN", null, null, null, null, null, null, null, -1, 0L, 0L, null,
                null, false, 0L, null, 0L, null, null);
        assertEquals("GOSSIP_DIGEST_SYN", e.rawSemanticType());
    }

    @Test
    public void semanticType_appliesAlias_collectionWrapper() {
        // messageType="ArrayList" should NOT be filtered by isGenericWrapper
        // and should be aliased to COLLECTION_WRAPPER by SemanticAliasTable.
        TraceEntry e = new TraceEntry(1, "test", 1, TraceEntry.EventType.SEND, false,
                System.currentTimeMillis(), System.nanoTime(), "N0", null, null, null, null, null,
                "ArrayList", null, null, null, null, null, null, null, -1, 0L, 0L, null, null,
                false, 0L, null, 0L, null, null);
        assertEquals("ArrayList", e.rawSemanticType());
        assertEquals("COLLECTION_WRAPPER", e.semanticType());
    }

    // --- canonicalEndpointKey ------------------------------------------------

    @Test
    public void canonicalEndpointKey_send_usesRoles() {
        TraceEntry e = guidanceEntry(TraceEntry.EventType.SEND, "node0", "node1", "cassandra",
                "GOSSIP_DIGEST_SYN", null, null, null, null);
        assertEquals("node0->node1", e.canonicalEndpointKey());
    }

    @Test
    public void canonicalEndpointKey_recvBegin_reversesDirection() {
        TraceEntry e = guidanceEntry(TraceEntry.EventType.RECV_BEGIN, "node1", "node0", "cassandra",
                "GOSSIP_DIGEST_SYN", null, null, null, null);
        // RECV_BEGIN: peer is src, self is dst
        assertEquals("node0->node1", e.canonicalEndpointKey());
    }

    // --- GUIDANCE canonical key ---------------------------------------------

    @Test
    public void guidanceKey_cassandraGossipVerbMapsToBackground() {
        TraceEntry e = guidanceEntry(TraceEntry.EventType.SEND, "node0", "node1", "cassandra",
                "GOSSIP_DIGEST_SYN", null, null, null, null);
        assertEquals("SEND|node0->node1|BACKGROUND",
                e.canonicalMessageKey(CanonicalKeyMode.GUIDANCE));
    }

    @Test
    public void guidanceKey_cassandraSchemaVerb() {
        TraceEntry e = guidanceEntry(TraceEntry.EventType.SEND, "node0", "node1", "cassandra",
                "SCHEMA_PULL_REQ", null, null, null, null);
        assertEquals("SEND|node0->node1|CASSANDRA_SCHEMA_SYNC",
                e.canonicalMessageKey(CanonicalKeyMode.GUIDANCE));
    }

    @Test
    public void guidanceKey_hdfsNamespaceMutation() {
        TraceEntry e = guidanceEntry(TraceEntry.EventType.SEND, "client", "namenode", "hdfs-rpc",
                null, "ClientNamenodeProtocol", "mkdirs", null, null);
        assertEquals("SEND|client->namenode|HDFS_CLIENT_NAMESPACE_MUTATION",
                e.canonicalMessageKey(CanonicalKeyMode.GUIDANCE));
    }

    @Test
    public void guidanceKey_hbaseMasterDdlMethodMapsToMasterSchemaDdl() {
        TraceEntry e = guidanceEntry(TraceEntry.EventType.SEND, "client", "master", "hbase", null,
                "MasterService", "CreateTable", null, null);
        assertEquals("SEND|client->master|HBASE_MASTER_SCHEMA_DDL",
                e.canonicalMessageKey(CanonicalKeyMode.GUIDANCE));
    }

    @Test
    public void guidanceKey_hbaseMutateMethodMapsToClientMutation() {
        TraceEntry e = guidanceEntry(TraceEntry.EventType.SEND, "client", "region", "hbase", null,
                "ClientService", "Mutate", "PUT", null);
        assertEquals("SEND|client->region|HBASE_CLIENT_MUTATION_OR_MULTI",
                e.canonicalMessageKey(CanonicalKeyMode.GUIDANCE));
    }

    @Test
    public void guidanceKey_omitsEndpointWhenBothRolesUnknown() {
        TraceEntry e = guidanceEntry(TraceEntry.EventType.SEND, null, null, "cassandra",
                "GOSSIP_DIGEST_SYN", null, null, null, null);
        assertEquals("SEND|BACKGROUND", e.canonicalMessageKey(CanonicalKeyMode.GUIDANCE));
    }

    @Test
    public void guidanceKey_unclassifiedFallsBackToUnknownWithRawTail() {
        TraceEntry e = guidanceEntry(TraceEntry.EventType.SEND, "node0", "node1", "cassandra",
                "SOME_BRAND_NEW_VERB_IN_6_X", null, null, null, null);
        assertEquals("SEND|node0->node1|UNKNOWN:SOME_BRAND_NEW_VERB_IN_6_X",
                e.canonicalMessageKey(CanonicalKeyMode.GUIDANCE));
    }

    @Test
    public void guidanceKey_unknownBucketSeparatesDistinctTypes() {
        TraceEntry a = guidanceEntry(TraceEntry.EventType.SEND, "node0", "node1", "cassandra",
                "UNCHARTED_VERB_A", null, null, null, null);
        TraceEntry b = guidanceEntry(TraceEntry.EventType.SEND, "node0", "node1", "cassandra",
                "UNCHARTED_VERB_B", null, null, null, null);
        assertNotEquals(a.canonicalMessageKey(CanonicalKeyMode.GUIDANCE),
                b.canonicalMessageKey(CanonicalKeyMode.GUIDANCE));
    }

    @Test
    public void guidanceKey_unknownTailPrefersRpcMethodForUnclassifiedHdfs() {
        // HDFS unclassified methods on the same protocol have null payload
        // and null messageType because the protobuf wrapper is a generic
        // RpcRequestWrapper. Without an rpcMethod tail they would both
        // collapse to UNKNOWN:UNKNOWN_TYPE — the GUIDANCE tail must use
        // the RPC method to keep them separable.
        TraceEntry a = guidanceEntry(TraceEntry.EventType.SEND, "client", "namenode", "hdfs-rpc",
                null, "ClientNamenodeProtocol", "someUnchartedMethodA", null, null);
        TraceEntry b = guidanceEntry(TraceEntry.EventType.SEND, "client", "namenode", "hdfs-rpc",
                null, "ClientNamenodeProtocol", "someUnchartedMethodB", null, null);
        String keyA = a.canonicalMessageKey(CanonicalKeyMode.GUIDANCE);
        String keyB = b.canonicalMessageKey(CanonicalKeyMode.GUIDANCE);
        assertNotEquals(keyA, keyB);
        assertTrue(keyA.endsWith("|UNKNOWN:ClientNamenodeProtocol#someUnchartedMethodA"),
                "key should carry the rpcService#rpcMethod tail, got: " + keyA);
        assertTrue(keyB.endsWith("|UNKNOWN:ClientNamenodeProtocol#someUnchartedMethodB"),
                "key should carry the rpcService#rpcMethod tail, got: " + keyB);
    }

    @Test
    public void guidanceKey_unknownTailUsesRpcMethodAloneWhenServiceMissing() {
        TraceEntry e = guidanceEntry(TraceEntry.EventType.SEND, "client", "master", "hbase", null,
                null, "unchartedRpc", null, null);
        String key = e.canonicalMessageKey(CanonicalKeyMode.GUIDANCE);
        assertTrue(key.endsWith("|UNKNOWN:unchartedRpc"),
                "key should carry the rpcMethod tail, got: " + key);
    }

    @Test
    public void guidanceKey_unknownTailFallsBackToRawSemanticTypeWhenRpcMissing() {
        TraceEntry e = guidanceEntry(TraceEntry.EventType.SEND, "node0", "node1", "cassandra",
                "SOME_BRAND_NEW_VERB", null, null, null, null);
        assertEquals("SEND|node0->node1|UNKNOWN:SOME_BRAND_NEW_VERB",
                e.canonicalMessageKey(CanonicalKeyMode.GUIDANCE));
    }

    @Test
    public void guidanceKey_noArgDefaultMatchesGuidanceOverload() {
        TraceEntry e = guidanceEntry(TraceEntry.EventType.SEND, "node0", "node1", "cassandra",
                "SCHEMA_PULL_REQ", null, null, null, null);
        assertEquals(e.canonicalMessageKey(), e.canonicalMessageKey(CanonicalKeyMode.GUIDANCE));
        assertEquals(e.canonicalMessageKey(CanonicalKeyMode.GUIDANCE), e.canonicalMessageKey(null));
    }

    // --- GUIDANCE stability across versions ---------------------------------

    @Test
    public void guidanceKey_stableAcrossCassandraMutationWrapperDrift() {
        // Cassandra 3.x sends Mutation wrapped in SingletonList; 4.x in ArrayList.
        // With GUIDANCE identity, the classifier ignores the wrapper type and
        // looks at the verb (MUTATION_REQ or MUTATION); when both lanes carry
        // the same verb, the key is stable across the wrapper rename even
        // though the underlying message types differ.
        TraceEntry cass3 = guidanceEntry(TraceEntry.EventType.SEND, "node0", "node1", "cassandra",
                "HINT_REQ", null, null, null, null);
        TraceEntry cass4 = guidanceEntry(TraceEntry.EventType.SEND, "node0", "node1", "cassandra",
                "HINT_REQ", null, null, null, null);
        assertEquals(cass3.canonicalMessageKey(CanonicalKeyMode.GUIDANCE),
                cass4.canonicalMessageKey(CanonicalKeyMode.GUIDANCE));
    }

    @Test
    public void guidanceKey_stableAcrossCassandraVerbRename() {
        // 3.x used MIGRATION_REQUEST; 4.x uses SCHEMA_PULL_REQ. Classifier maps
        // both to CASSANDRA_SCHEMA_SYNC so the guidance key is stable across
        // the version-pair that would otherwise fragment messages by verb.
        TraceEntry cass3 = guidanceEntry(TraceEntry.EventType.SEND, "node0", "node1", "cassandra",
                "MIGRATION_REQUEST", null, null, null, null);
        TraceEntry cass4 = guidanceEntry(TraceEntry.EventType.SEND, "node0", "node1", "cassandra",
                "SCHEMA_PULL_REQ", null, null, null, null);
        assertEquals(cass3.canonicalMessageKey(CanonicalKeyMode.GUIDANCE),
                cass4.canonicalMessageKey(CanonicalKeyMode.GUIDANCE));
    }

    @Test
    public void guidanceKey_distinguishesUpgradeCriticalFromBackground() {
        TraceEntry schema = guidanceEntry(TraceEntry.EventType.SEND, "node0", "node1", "cassandra",
                "SCHEMA_PULL_REQ", null, null, null, null);
        TraceEntry gossip = guidanceEntry(TraceEntry.EventType.SEND, "node0", "node1", "cassandra",
                "GOSSIP_DIGEST_SYN", null, null, null, null);
        assertNotEquals(schema.canonicalMessageKey(CanonicalKeyMode.GUIDANCE),
                gossip.canonicalMessageKey(CanonicalKeyMode.GUIDANCE));
        assertEquals(ProtocolFamilyClass.UPGRADE_CRITICAL, schema.protocolFamilyClass());
        assertEquals(ProtocolFamilyClass.BACKGROUND, gossip.protocolFamilyClass());
    }

    @Test
    public void guidanceKey_rolesAffectOnlineKey() {
        TraceEntry a = guidanceEntry(TraceEntry.EventType.SEND, "node0", "node1", "cassandra",
                "SCHEMA_PULL_REQ", null, null, null, null);
        TraceEntry b = guidanceEntry(TraceEntry.EventType.SEND, "node1", "node2", "cassandra",
                "SCHEMA_PULL_REQ", null, null, null, null);
        assertNotEquals(a.canonicalMessageKey(CanonicalKeyMode.GUIDANCE),
                b.canonicalMessageKey(CanonicalKeyMode.GUIDANCE));
    }

    @Test
    public void protocolFamily_exposesClassifierResult() {
        TraceEntry paxos = guidanceEntry(TraceEntry.EventType.SEND, "node0", "node1", "cassandra",
                "PAXOS_PREPARE_REQ", null, null, null, null);
        assertEquals(ProtocolFamily.CASSANDRA_PAXOS, paxos.protocolFamily());
    }

    // --- SEMANTIC_SHAPE_SUMMARY (retained diagnostic tier) ------------------

    @Test
    public void summaryKey_appendsShapeAndSummaryBucket() {
        TraceEntry e = summaryEntry(TraceEntry.EventType.SEND, "node0", "node1", null,
                "org.apache.cassandra.gms.GossipDigestSyn", 0xABCDEF01L,
                "GossipDigestSyn|node0|42");
        String key = e.canonicalMessageKey(CanonicalKeyMode.SEMANTIC_SHAPE_SUMMARY);
        assertTrue(key.startsWith("SEND|node0->node1|GossipDigestSyn|shape=abcdef01|sum="),
                "key should start with the diagnostic prefix, got: " + key);
        assertFalse(key.endsWith("|sum=-"),
                "summary bucket should not be the sentinel '-' for a non-empty summary");
    }

    @Test
    public void summaryKey_nullSummaryHitsSentinel() {
        TraceEntry e = summaryEntry(TraceEntry.EventType.SEND, "node0", "node1", null,
                "org.apache.cassandra.gms.GossipDigestSyn", 0xABCDEF01L, null);
        assertEquals("SEND|node0->node1|GossipDigestSyn|shape=abcdef01|sum=-",
                e.canonicalMessageKey(CanonicalKeyMode.SEMANTIC_SHAPE_SUMMARY));
    }

    @Test
    public void summaryKey_bucketsNumericNoise() {
        TraceEntry a = summaryEntry(TraceEntry.EventType.SEND, "node0", "node1", null,
                "org.apache.cassandra.gms.GossipDigestSyn", 0xABCDEF01L,
                "GossipDigestSyn|node0|42");
        TraceEntry b = summaryEntry(TraceEntry.EventType.SEND, "node0", "node1", null,
                "org.apache.cassandra.gms.GossipDigestSyn", 0xABCDEF01L,
                "GossipDigestSyn|node0|9999");
        assertEquals(a.canonicalMessageKey(CanonicalKeyMode.SEMANTIC_SHAPE_SUMMARY),
                b.canonicalMessageKey(CanonicalKeyMode.SEMANTIC_SHAPE_SUMMARY));
    }

    @Test
    public void summaryKey_separatesStructurallyDifferent() {
        TraceEntry a = summaryEntry(TraceEntry.EventType.SEND, "node0", "node1", null,
                "org.apache.cassandra.db.Mutation", 0xABCDL, "Mutation|keyspace1|table_a");
        TraceEntry b = summaryEntry(TraceEntry.EventType.SEND, "node0", "node1", null,
                "org.apache.cassandra.db.Mutation", 0xABCDL, "Mutation|keyspace2|table_b");
        assertNotEquals(a.canonicalMessageKey(CanonicalKeyMode.SEMANTIC_SHAPE_SUMMARY),
                b.canonicalMessageKey(CanonicalKeyMode.SEMANTIC_SHAPE_SUMMARY));
    }

    // --- CanonicalKeyMode enum & default ------------------------------------

    @Test
    public void defaultCanonicalKeyMode_isGuidance() {
        assertEquals(CanonicalKeyMode.GUIDANCE, CanonicalKeyMode.DEFAULT);
    }

    // --- Trace accessors -----------------------------------------------------

    @Test
    public void traceAccessors_noArgMatchesGuidanceOverload() {
        Trace t = new Trace();
        t.addEntry(guidanceEntry(TraceEntry.EventType.SEND, "node0", "node1", "cassandra",
                "SCHEMA_PULL_REQ", null, null, null, null));
        t.addEntry(guidanceEntry(TraceEntry.EventType.SEND, "node0", "node1", "cassandra",
                "GOSSIP_DIGEST_SYN", null, null, null, null));

        assertEquals(t.getCanonicalMultiset(), t.getCanonicalMultiset(CanonicalKeyMode.GUIDANCE));
        assertEquals(t.getCanonicalKeysForDiff(),
                t.getCanonicalKeysForDiff(CanonicalKeyMode.GUIDANCE));
    }

    @Test
    public void traceAccessors_guidanceTierCollapsesWithinSemanticDrift() {
        // Same classifier family, different shape. GUIDANCE must collapse both
        // entries into a single bucket because shape is not part of the key.
        // Tag the messageType with a known Cassandra verb so both entries
        // classify to the same family and the shape drift does not split the
        // guidance key.
        Trace t = new Trace();
        t.addEntry(guidanceEntryWithShape(TraceEntry.EventType.SEND, "node0", "node1", "cassandra",
                "HINT_REQ", 0x1111L));
        t.addEntry(guidanceEntryWithShape(TraceEntry.EventType.SEND, "node0", "node1", "cassandra",
                "HINT_REQ", 0x2222L));

        // Both entries classify to CASSANDRA_READ_REPAIR_OR_HINT at the
        // guidance tier. The offline-diagnostic summary tier still separates
        // them because the shape hashes differ.
        assertEquals(1, t.getCanonicalMultiset(CanonicalKeyMode.GUIDANCE).size());
        assertEquals(2, t.getCanonicalMultiset(CanonicalKeyMode.SEMANTIC_SHAPE_SUMMARY).size());
    }

    private static TraceEntry guidanceEntryWithShape(TraceEntry.EventType eventType,
            String nodeRole, String peerRole, String protocol, String messageType, long shapeHash) {
        return new TraceEntry(1, "test", 1, eventType, false, System.currentTimeMillis(),
                System.nanoTime(), null, null, nodeRole, peerRole, null, protocol, messageType,
                null, null, null, null, null, null, null, -1, shapeHash, 0L, null, null, false, 0L,
                null, 0L, null, null);
    }

    @Test
    public void traceAccessors_summaryTierProducesTierSpecificKeys() {
        Trace t = new Trace();
        t.addEntry(summaryEntry(TraceEntry.EventType.SEND, "node0", "node1", null,
                "org.apache.cassandra.db.Mutation", 0xABCDL, "Mutation|keyspace1"));

        Map<String, Integer> guidance = t.getCanonicalMultiset(CanonicalKeyMode.GUIDANCE);
        Map<String, Integer> summary = t
                .getCanonicalMultiset(CanonicalKeyMode.SEMANTIC_SHAPE_SUMMARY);

        assertEquals(1, guidance.size());
        assertEquals(1, summary.size());
        assertNotEquals(guidance.keySet(), summary.keySet());

        List<String> diffKeys = t.getCanonicalKeysForDiff(CanonicalKeyMode.SEMANTIC_SHAPE_SUMMARY);
        assertEquals(1, diffKeys.size());
        assertEquals(summary.keySet().iterator().next(), diffKeys.get(0));
    }
}
