package org.zlab.net.tracker;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Tests for Phase 2/3 canonical key infrastructure on TraceEntry.
 */
public class TraceEntryCanonicalKeyTest {

    private static TraceEntry entry(TraceEntry.EventType eventType, String nodeId, String peerId,
            String nodeRole, String peerRole, String messageType, String payloadType) {
        return new TraceEntry(1, "test", 1, eventType, false, System.currentTimeMillis(),
                System.nanoTime(), nodeId, peerId, nodeRole, peerRole, null, null, messageType,
                null, null, null, null, -1, 0L, 0L, null, null, false, 0L, null, 0L, null,
                payloadType);
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

    // --- canonicalMessageKey ---

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
}
