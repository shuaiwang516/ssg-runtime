package org.zlab.net.tracker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Tests for Phase 2/3 canonical key infrastructure on TraceEntry.
 */
public class TraceEntryCanonicalKeyTest {

    private static TraceEntry entry(TraceEntry.EventType eventType, String nodeId, String peerId,
            String nodeRole, String peerRole, String messageType, String payloadType) {
        return entry(eventType, nodeId, peerId, nodeRole, peerRole, messageType, payloadType, 0L,
                0L, null);
    }

    private static TraceEntry entry(TraceEntry.EventType eventType, String nodeId, String peerId,
            String nodeRole, String peerRole, String messageType, String payloadType,
            long shapeHash, long valueHash, String messageSummary) {
        return new TraceEntry(1, "test", 1, eventType, false, System.currentTimeMillis(),
                System.nanoTime(), nodeId, peerId, nodeRole, peerRole, null, null, messageType,
                null, null, null, null, -1, shapeHash, valueHash, null, messageSummary, false, 0L,
                null, 0L, null, payloadType);
    }

    // --- rawSemanticType / semanticType ---

    @Test
    public void rawSemanticType_prefersPayloadOverMessageType() {
        TraceEntry e = entry(TraceEntry.EventType.SEND, "N0", null, null, null, "MessageOut",
                "org.apache.cassandra.gms.GossipDigestSyn");
        assertEquals("GossipDigestSyn", e.rawSemanticType());
    }

    @Test
    public void rawSemanticType_fallsToMessageType_whenPayloadNull() {
        TraceEntry e = entry(TraceEntry.EventType.SEND, "N0", null, null, null, "GOSSIP_DIGEST_SYN",
                null);
        assertEquals("GOSSIP_DIGEST_SYN", e.rawSemanticType());
    }

    @Test
    public void rawSemanticType_skipsGenericWrapper_messageType() {
        TraceEntry e = entry(TraceEntry.EventType.SEND, "N0", null, null, null, "MessageOut", null);
        assertEquals("UNKNOWN_TYPE", e.rawSemanticType());
    }

    @Test
    public void semanticType_appliesAlias_collectionWrapper() {
        // messageType="ArrayList" should NOT be filtered by isGenericWrapper
        // and should be aliased to COLLECTION_WRAPPER by SemanticAliasTable
        TraceEntry e = entry(TraceEntry.EventType.SEND, "N0", null, null, null, "ArrayList", null);
        assertEquals("ArrayList", e.rawSemanticType());
        assertEquals("COLLECTION_WRAPPER", e.semanticType());
    }

    @Test
    public void semanticType_appliesAlias_singletonList() {
        // payloadType as full class name: shortClassName strips to SingletonList
        TraceEntry e = entry(TraceEntry.EventType.SEND, "N0", null, null, null, null,
                "java.util.Collections$SingletonList");
        assertEquals("SingletonList", e.rawSemanticType());
        assertEquals("COLLECTION_WRAPPER", e.semanticType());
    }

    @Test
    public void semanticType_noAlias_passesThrough() {
        TraceEntry e = entry(TraceEntry.EventType.SEND, "N0", null, null, null, null,
                "org.apache.cassandra.gms.GossipDigestSyn");
        assertEquals("GossipDigestSyn", e.semanticType());
    }

    // --- canonicalEndpointKey ---

    @Test
    public void canonicalEndpointKey_send_usesRoles() {
        TraceEntry e = entry(TraceEntry.EventType.SEND, "execID-N0", "192.168.1.5", "node0",
                "node1", null, null);
        assertEquals("node0->node1", e.canonicalEndpointKey());
    }

    @Test
    public void canonicalEndpointKey_recvBegin_reversesDirection() {
        TraceEntry e = entry(TraceEntry.EventType.RECV_BEGIN, "execID-N1", "192.168.1.2", "node1",
                "node0", null, null);
        // RECV_BEGIN: peer is src, self is dst
        assertEquals("node0->node1", e.canonicalEndpointKey());
    }

    @Test
    public void canonicalEndpointKey_send_fallsBackToNormalizeRole() {
        TraceEntry e = entry(TraceEntry.EventType.SEND, "execID-N0", null, "client", null, null,
                null);
        assertEquals("client->UNKNOWN", e.canonicalEndpointKey());
    }

    @Test
    public void canonicalEndpointKey_nullRoles_usesRawIdFallback() {
        TraceEntry e = entry(TraceEntry.EventType.SEND, "execID-N0", "192.168.1.5", null, null,
                null, null);
        // normalizeRole("execID-N0") -> "N0", normalizeRole("192.168.1.5") ->
        // "192.168.1.5"
        assertEquals("N0->192.168.1.5", e.canonicalEndpointKey());
    }

    // --- canonicalMessageKey: SEMANTIC (default / no-arg) ---

    @Test
    public void canonicalMessageKey_fullExample() {
        TraceEntry e = entry(TraceEntry.EventType.SEND, "execID-N0", "192.168.1.5", "client",
                "namenode", null, "org.apache.hadoop.hdfs.protocol.proto"
                        + ".ClientNamenodeProtocolProtos$GetFileInfoRequestProto");
        assertEquals("SEND|client->namenode|GetFileInfoRequestProto", e.canonicalMessageKey());
    }

    @Test
    public void canonicalMessageKey_omitsEndpoint_whenBothUnknown() {
        TraceEntry e = entry(TraceEntry.EventType.SEND, null, null, null, null, null,
                "org.apache.cassandra.gms.GossipDigestSyn");
        assertEquals("SEND|GossipDigestSyn", e.canonicalMessageKey());
    }

    @Test
    public void canonicalMessageKey_cassandraGossip() {
        TraceEntry e = entry(TraceEntry.EventType.SEND, "execID-N0", "192.168.1.5", "node0",
                "node1", "GOSSIP_DIGEST_SYN", "org.apache.cassandra.gms.GossipDigestSyn");
        assertEquals("SEND|node0->node1|GossipDigestSyn", e.canonicalMessageKey());
    }

    // --- Phase 3: CanonicalKeyMode ---

    @Test
    public void canonicalMessageKey_noArgMatchesSemanticOverload() {
        TraceEntry e = entry(TraceEntry.EventType.SEND, "execID-N0", "192.168.1.5", "node0",
                "node1", null, "org.apache.cassandra.gms.GossipDigestSyn", 0xABCDEF01L, 0x12345678L,
                "GossipDigestSyn|(size=3)|42");
        assertEquals(e.canonicalMessageKey(), e.canonicalMessageKey(CanonicalKeyMode.SEMANTIC));
    }

    @Test
    public void canonicalMessageKey_nullModeDefaultsToSemantic() {
        TraceEntry e = entry(TraceEntry.EventType.SEND, "execID-N0", "192.168.1.5", "node0",
                "node1", null, "org.apache.cassandra.gms.GossipDigestSyn", 0xABCDEF01L, 0x12345678L,
                "GossipDigestSyn|42");
        assertEquals(e.canonicalMessageKey(CanonicalKeyMode.SEMANTIC), e.canonicalMessageKey(null));
    }

    @Test
    public void canonicalMessageKey_semanticShape_appendsShapeFragment() {
        TraceEntry e = entry(TraceEntry.EventType.SEND, "execID-N0", "192.168.1.5", "node0",
                "node1", null, "org.apache.cassandra.gms.GossipDigestSyn", 0xABCDEF01L, 0L, null);
        assertEquals("SEND|node0->node1|GossipDigestSyn|shape=abcdef01",
                e.canonicalMessageKey(CanonicalKeyMode.SEMANTIC_SHAPE));
    }

    @Test
    public void canonicalMessageKey_semanticShape_omitsEndpointWhenBothUnknown() {
        TraceEntry e = entry(TraceEntry.EventType.SEND, null, null, null, null, null,
                "org.apache.cassandra.gms.GossipDigestSyn", 0xABCDEF01L, 0L, null);
        assertEquals("SEND|GossipDigestSyn|shape=abcdef01",
                e.canonicalMessageKey(CanonicalKeyMode.SEMANTIC_SHAPE));
    }

    @Test
    public void canonicalMessageKey_semanticShape_separatesDifferentShapes() {
        // Same semantic type, different payload field sets -> different shape hash
        TraceEntry small = entry(TraceEntry.EventType.SEND, null, null, "node0", "node1", null,
                "org.apache.cassandra.db.Mutation", 0x1111L, 0L, null);
        TraceEntry large = entry(TraceEntry.EventType.SEND, null, null, "node0", "node1", null,
                "org.apache.cassandra.db.Mutation", 0x2222L, 0L, null);

        // SEMANTIC collapses them
        assertEquals(small.canonicalMessageKey(CanonicalKeyMode.SEMANTIC),
                large.canonicalMessageKey(CanonicalKeyMode.SEMANTIC));
        // SEMANTIC_SHAPE separates them
        assertNotEquals(small.canonicalMessageKey(CanonicalKeyMode.SEMANTIC_SHAPE),
                large.canonicalMessageKey(CanonicalKeyMode.SEMANTIC_SHAPE));
    }

    @Test
    public void canonicalMessageKey_semanticShapeSummary_appendsSummaryBucket() {
        TraceEntry e = entry(TraceEntry.EventType.SEND, "execID-N0", "192.168.1.5", "node0",
                "node1", null, "org.apache.cassandra.gms.GossipDigestSyn", 0xABCDEF01L, 0L,
                "GossipDigestSyn|node0|42");
        String key = e.canonicalMessageKey(CanonicalKeyMode.SEMANTIC_SHAPE_SUMMARY);
        assertTrue(key.startsWith("SEND|node0->node1|GossipDigestSyn|shape=abcdef01|sum="),
                "key should start with SEND|node0->node1|GossipDigestSyn|shape=abcdef01|sum=");
        assertFalse(key.endsWith("|sum=-"),
                "summary bucket should not be the sentinel '-' for a non-empty summary");
    }

    @Test
    public void canonicalMessageKey_semanticShapeSummary_nullSummaryHitsSentinel() {
        TraceEntry e = entry(TraceEntry.EventType.SEND, null, null, "node0", "node1", null,
                "org.apache.cassandra.gms.GossipDigestSyn", 0xABCDEF01L, 0L, null);
        assertEquals("SEND|node0->node1|GossipDigestSyn|shape=abcdef01|sum=-",
                e.canonicalMessageKey(CanonicalKeyMode.SEMANTIC_SHAPE_SUMMARY));
    }

    @Test
    public void canonicalMessageKey_semanticShapeSummary_bucketsNumericNoise() {
        // Two messages differ only in numeric field values; the bucketed summary
        // hash should collapse them to the same SEMANTIC_SHAPE_SUMMARY key.
        TraceEntry a = entry(TraceEntry.EventType.SEND, null, null, "node0", "node1", null,
                "org.apache.cassandra.gms.GossipDigestSyn", 0xABCDEF01L, 0L,
                "GossipDigestSyn|node0|42");
        TraceEntry b = entry(TraceEntry.EventType.SEND, null, null, "node0", "node1", null,
                "org.apache.cassandra.gms.GossipDigestSyn", 0xABCDEF01L, 0L,
                "GossipDigestSyn|node0|9999");
        assertEquals(a.canonicalMessageKey(CanonicalKeyMode.SEMANTIC_SHAPE_SUMMARY),
                b.canonicalMessageKey(CanonicalKeyMode.SEMANTIC_SHAPE_SUMMARY));
    }

    @Test
    public void canonicalMessageKey_semanticShapeSummary_bucketsContainerSizeNoise() {
        // Different container sizes (size=N) should bucket to (size=*)
        TraceEntry a = entry(TraceEntry.EventType.SEND, null, null, "node0", "node1", null,
                "org.apache.cassandra.db.Mutation", 0xABCDL, 0L, "Mutation|(size=3)|keyspace1");
        TraceEntry b = entry(TraceEntry.EventType.SEND, null, null, "node0", "node1", null,
                "org.apache.cassandra.db.Mutation", 0xABCDL, 0L, "Mutation|(size=57)|keyspace1");
        assertEquals(a.canonicalMessageKey(CanonicalKeyMode.SEMANTIC_SHAPE_SUMMARY),
                b.canonicalMessageKey(CanonicalKeyMode.SEMANTIC_SHAPE_SUMMARY));
    }

    @Test
    public void canonicalMessageKey_semanticShapeSummary_separatesStructurallyDifferent() {
        // Same semType + shape but structurally different non-numeric tokens in
        // summary -> the bucket must differ.
        TraceEntry a = entry(TraceEntry.EventType.SEND, null, null, "node0", "node1", null,
                "org.apache.cassandra.db.Mutation", 0xABCDL, 0L, "Mutation|keyspace1|table_a");
        TraceEntry b = entry(TraceEntry.EventType.SEND, null, null, "node0", "node1", null,
                "org.apache.cassandra.db.Mutation", 0xABCDL, 0L, "Mutation|keyspace2|table_b");
        assertNotEquals(a.canonicalMessageKey(CanonicalKeyMode.SEMANTIC_SHAPE_SUMMARY),
                b.canonicalMessageKey(CanonicalKeyMode.SEMANTIC_SHAPE_SUMMARY));
    }

    @Test
    public void canonicalMessageKey_semanticShapeValue_appendsRawValueHash() {
        TraceEntry e = entry(TraceEntry.EventType.SEND, "execID-N0", "192.168.1.5", "node0",
                "node1", null, "org.apache.cassandra.gms.GossipDigestSyn", 0xABCDEF01L, 0xCAFEBABEL,
                null);
        assertEquals("SEND|node0->node1|GossipDigestSyn|shape=abcdef01|val=cafebabe",
                e.canonicalMessageKey(CanonicalKeyMode.SEMANTIC_SHAPE_VALUE));
    }

    @Test
    public void canonicalMessageKey_semanticShapeValue_separatesDifferentValueHashes() {
        TraceEntry a = entry(TraceEntry.EventType.SEND, null, null, "node0", "node1", null,
                "org.apache.cassandra.db.Mutation", 0xABCDL, 0x1111L, null);
        TraceEntry b = entry(TraceEntry.EventType.SEND, null, null, "node0", "node1", null,
                "org.apache.cassandra.db.Mutation", 0xABCDL, 0x2222L, null);
        assertNotEquals(a.canonicalMessageKey(CanonicalKeyMode.SEMANTIC_SHAPE_VALUE),
                b.canonicalMessageKey(CanonicalKeyMode.SEMANTIC_SHAPE_VALUE));
    }

    // --- Phase 3: cross-version stability invariants ---

    @Test
    public void canonicalKey_crossVersionAlias_matchesAtAllTiersWhenTraitsMatch() {
        // Cassandra 3.x sends Mutation wrapped in SingletonList; 4.x in ArrayList.
        // SemanticAliasTable canonicalizes both to COLLECTION_WRAPPER. As long as
        // shape+summary match, every tier should collapse them.
        TraceEntry cass3 = entry(TraceEntry.EventType.SEND, null, null, "node0", "node1",
                "SingletonList", null, 0xDEADBEEFL, 0L, "SingletonList|Mutation|keyspace1");
        TraceEntry cass4 = entry(TraceEntry.EventType.SEND, null, null, "node0", "node1",
                "ArrayList", null, 0xDEADBEEFL, 0L, "ArrayList|Mutation|keyspace1");

        // The aliased semanticType collapses them at every non-VALUE tier.
        assertEquals(cass3.canonicalMessageKey(CanonicalKeyMode.SEMANTIC),
                cass4.canonicalMessageKey(CanonicalKeyMode.SEMANTIC));
        assertEquals(cass3.canonicalMessageKey(CanonicalKeyMode.SEMANTIC_SHAPE),
                cass4.canonicalMessageKey(CanonicalKeyMode.SEMANTIC_SHAPE));
        // Even with distinct wrapper tokens in summary, the same bucketed content
        // (Mutation|keyspace1) produces the same bucket hash.
        assertEquals(cass3.canonicalMessageKey(CanonicalKeyMode.SEMANTIC_SHAPE_SUMMARY),
                cass4.canonicalMessageKey(CanonicalKeyMode.SEMANTIC_SHAPE_SUMMARY));
    }

    @Test
    public void canonicalKey_crossVersionSummaryWithNumericDrift_stableAtSummaryTier() {
        // Same semantic type across versions; only numeric tokens (node index,
        // timestamps, counters) drift. SEMANTIC_SHAPE_SUMMARY must remain stable.
        TraceEntry older = entry(TraceEntry.EventType.SEND, null, null, "node0", "node1", null,
                "org.apache.hadoop.hdfs.protocol.proto.ClientNamenodeProtocolProtos"
                        + "$GetFileInfoRequestProto",
                0xFEEDL, 0L, "GetFileInfoRequestProto|/tmp/x|1710000000");
        TraceEntry newer = entry(TraceEntry.EventType.SEND, null, null, "node0", "node1", null,
                "org.apache.hadoop.hdfs.protocol.proto.ClientNamenodeProtocolProtos"
                        + "$GetFileInfoRequestProto",
                0xFEEDL, 0L, "GetFileInfoRequestProto|/tmp/x|1710555555");
        assertEquals(older.canonicalMessageKey(CanonicalKeyMode.SEMANTIC_SHAPE_SUMMARY),
                newer.canonicalMessageKey(CanonicalKeyMode.SEMANTIC_SHAPE_SUMMARY));
    }

    @Test
    public void canonicalKey_crossVersionSummaryWithHexDrift_stableAtSummaryTier() {
        // Message id (8+ hex chars) differs between runs; summary must bucket it
        // to <hex> so identical requests collapse.
        TraceEntry a = entry(TraceEntry.EventType.SEND, null, null, "node0", "node1", null,
                "org.apache.hbase.RegionInfo", 0xBABEL, 0L,
                "RegionInfo|defaulttest|deadbeefcafebabe");
        TraceEntry b = entry(TraceEntry.EventType.SEND, null, null, "node0", "node1", null,
                "org.apache.hbase.RegionInfo", 0xBABEL, 0L,
                "RegionInfo|defaulttest|fedcba9876543210");
        assertEquals(a.canonicalMessageKey(CanonicalKeyMode.SEMANTIC_SHAPE_SUMMARY),
                b.canonicalMessageKey(CanonicalKeyMode.SEMANTIC_SHAPE_SUMMARY));
    }

    @Test
    public void canonicalKey_crossVersionSummaryWithUuidDrift_stableAtSummaryTier() {
        TraceEntry a = entry(TraceEntry.EventType.SEND, null, null, "node0", "node1", null,
                "org.apache.hbase.ScanRequest", 0xCAFEL, 0L,
                "ScanRequest|my_table|550e8400-e29b-41d4-a716-446655440000");
        TraceEntry b = entry(TraceEntry.EventType.SEND, null, null, "node0", "node1", null,
                "org.apache.hbase.ScanRequest", 0xCAFEL, 0L,
                "ScanRequest|my_table|6ba7b810-9dad-11d1-80b4-00c04fd430c8");
        assertEquals(a.canonicalMessageKey(CanonicalKeyMode.SEMANTIC_SHAPE_SUMMARY),
                b.canonicalMessageKey(CanonicalKeyMode.SEMANTIC_SHAPE_SUMMARY));
    }

    // --- Phase 3: tier ordering sanity ---

    @Test
    public void canonicalKey_tiers_strictlyExtendFromCoarseToFine() {
        TraceEntry e = entry(TraceEntry.EventType.SEND, null, null, "node0", "node1", null,
                "org.apache.cassandra.db.Mutation", 0x1234L, 0xABCDL,
                "Mutation|(size=3)|keyspace1");
        String semantic = e.canonicalMessageKey(CanonicalKeyMode.SEMANTIC);
        String shape = e.canonicalMessageKey(CanonicalKeyMode.SEMANTIC_SHAPE);
        String shapeSummary = e.canonicalMessageKey(CanonicalKeyMode.SEMANTIC_SHAPE_SUMMARY);
        String shapeValue = e.canonicalMessageKey(CanonicalKeyMode.SEMANTIC_SHAPE_VALUE);

        assertTrue(shape.startsWith(semantic + "|shape="));
        assertTrue(shapeSummary.startsWith(shape + "|sum="));
        assertTrue(shapeValue.startsWith(shape + "|val="));
    }

    @Test
    public void defaultCanonicalKeyMode_isSemanticShapeSummary() {
        assertEquals(CanonicalKeyMode.SEMANTIC_SHAPE_SUMMARY, CanonicalKeyMode.DEFAULT);
    }

    // --- Phase 3: Trace accessor overloads ---

    @Test
    public void traceAccessors_noArgMatchesSemanticOverload() {
        Trace t = new Trace();
        t.addEntry(entry(TraceEntry.EventType.SEND, null, null, "node0", "node1", null,
                "org.apache.cassandra.db.Mutation", 0xABCDL, 0L, "Mutation|keyspace1"));
        t.addEntry(entry(TraceEntry.EventType.SEND, null, null, "node0", "node1", null,
                "org.apache.cassandra.gms.GossipDigestSyn", 0xBEEFL, 0L, "GossipDigestSyn|node0"));

        assertEquals(t.getCanonicalMultiset(), t.getCanonicalMultiset(CanonicalKeyMode.SEMANTIC));
        assertEquals(t.getCanonicalKeysForDiff(),
                t.getCanonicalKeysForDiff(CanonicalKeyMode.SEMANTIC));
    }

    @Test
    public void traceAccessors_shapeSummaryTierUsesTierSpecificKeys() {
        Trace t = new Trace();
        TraceEntry e = entry(TraceEntry.EventType.SEND, null, null, "node0", "node1", null,
                "org.apache.cassandra.db.Mutation", 0xABCDL, 0L, "Mutation|keyspace1");
        t.addEntry(e);

        Map<String, Integer> semantic = t.getCanonicalMultiset(CanonicalKeyMode.SEMANTIC);
        Map<String, Integer> shapeSummary = t
                .getCanonicalMultiset(CanonicalKeyMode.SEMANTIC_SHAPE_SUMMARY);

        // Same entry, same multiset cardinality, but distinct key strings
        // at the two tiers.
        assertEquals(1, semantic.size());
        assertEquals(1, shapeSummary.size());
        assertNotEquals(semantic.keySet(), shapeSummary.keySet());

        List<String> diffKeys = t.getCanonicalKeysForDiff(CanonicalKeyMode.SEMANTIC_SHAPE_SUMMARY);
        assertEquals(1, diffKeys.size());
        assertEquals(shapeSummary.keySet().iterator().next(), diffKeys.get(0));
    }

    @Test
    public void traceAccessors_separatesWithinSemanticDriftAtShapeTier() {
        // One semantic type, two distinct shapes -> SEMANTIC collapses to size 1,
        // SEMANTIC_SHAPE separates to size 2.
        Trace t = new Trace();
        t.addEntry(entry(TraceEntry.EventType.SEND, null, null, "node0", "node1", null,
                "org.apache.cassandra.db.Mutation", 0x1111L, 0L, null));
        t.addEntry(entry(TraceEntry.EventType.SEND, null, null, "node0", "node1", null,
                "org.apache.cassandra.db.Mutation", 0x2222L, 0L, null));

        assertEquals(1, t.getCanonicalMultiset(CanonicalKeyMode.SEMANTIC).size());
        assertEquals(2, t.getCanonicalMultiset(CanonicalKeyMode.SEMANTIC_SHAPE).size());
    }
}
