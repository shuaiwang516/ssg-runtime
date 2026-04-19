package org.zlab.net.tracker.classifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests for the Phase 1 first-cut {@link ProtocolFamilyClassifier}. The
 * classifier only commits to dominant upgrade-critical families per system
 * plus {@code BACKGROUND} and {@code UNKNOWN}; long-tail verb coverage is
 * deferred to Phase 5 profiles.
 */
public class ProtocolFamilyClassifierTest {

    // --- Cassandra -----------------------------------------------------------

    @Test
    public void cassandra_schemaPullVerbMapsToSchemaSync() {
        assertEquals(ProtocolFamily.CASSANDRA_SCHEMA_SYNC,
                ProtocolFamilyClassifier.classify("cassandra", "SCHEMA_PULL_REQ",
                        null, null, null, null));
    }

    @Test
    public void cassandra_migrationRequestVerbMapsToSchemaSync() {
        // 3.x verb name — should collapse into the same family as 4.x SCHEMA_PULL_REQ.
        assertEquals(ProtocolFamily.CASSANDRA_SCHEMA_SYNC,
                ProtocolFamilyClassifier.classify("cassandra", "MIGRATION_REQUEST",
                        null, null, null, null));
    }

    @Test
    public void cassandra_paxosPrepareMapsToPaxos() {
        assertEquals(ProtocolFamily.CASSANDRA_PAXOS, ProtocolFamilyClassifier.classify("cassandra",
                "PAXOS_PREPARE_REQ", null, null, null, null));
    }

    @Test
    public void cassandra_paxosV2FallsIntoPaxosViaPrefix() {
        assertEquals(ProtocolFamily.CASSANDRA_PAXOS,
                ProtocolFamilyClassifier.classify("cassandra", "PAXOS2_COMMIT_REMOTE_REQ",
                        null, null, null, null));
    }

    @Test
    public void cassandra_repairReqMapsToRepairOrStream() {
        assertEquals(ProtocolFamily.CASSANDRA_REPAIR_OR_STREAM,
                ProtocolFamilyClassifier.classify("cassandra", "REPAIR_REQ", null, null, null,
                        null));
    }

    @Test
    public void cassandra_streamingInitiateMapsToRepairOrStream() {
        assertEquals(ProtocolFamily.CASSANDRA_REPAIR_OR_STREAM,
                ProtocolFamilyClassifier.classify("cassandra", "STREAM_INIT_MESSAGE",
                        null, null, null, null));
    }

    @Test
    public void cassandra_hintReqMapsToReadRepairOrHint() {
        assertEquals(ProtocolFamily.CASSANDRA_READ_REPAIR_OR_HINT,
                ProtocolFamilyClassifier.classify("cassandra", "HINT_REQ", null, null, null, null));
    }

    @Test
    public void cassandra_gossipDigestSynMapsToBackground() {
        assertEquals(ProtocolFamily.BACKGROUND, ProtocolFamilyClassifier.classify("cassandra",
                "GOSSIP_DIGEST_SYN", null, null, null, null));
    }

    @Test
    public void cassandra_gossipShutdownMapsToBackground() {
        assertEquals(ProtocolFamily.BACKGROUND, ProtocolFamilyClassifier.classify("cassandra",
                "GOSSIP_SHUTDOWN", null, null, null, null));
    }

    @Test
    public void cassandra_unknownVerbMapsToUnknown() {
        assertEquals(ProtocolFamily.UNKNOWN, ProtocolFamilyClassifier.classify("cassandra",
                "SOME_UNCHARTED_VERB", null, null, null, null));
    }

    @Test
    public void cassandra_gossipPayloadFallbackMapsToBackground() {
        // Some Cassandra 3.x paths wrap gossip via MessageOut, so messageType
        // may arrive blank. The classifier should still fall back to the
        // payload class name for obvious gossip messages.
        assertEquals(ProtocolFamily.BACKGROUND, ProtocolFamilyClassifier.classify("cassandra",
                null, null, null, null, "org.apache.cassandra.gms.GossipDigestSyn"));
    }

    @Test
    public void cassandra_mutationPayloadAloneMapsToUnknown() {
        // Generic Mutation payloads are ambiguous — they can be client
        // writes, schema mutations, hint mutations, or repair mutations.
        // Without a verb or RPC method, the classifier must leave them
        // as UNKNOWN rather than over-promote to any upgrade-critical
        // family (historical bug: the payload-suffix set incorrectly
        // mapped them to CASSANDRA_SCHEMA_SYNC).
        assertEquals(ProtocolFamily.UNKNOWN, ProtocolFamilyClassifier.classify("cassandra", null,
                null, null, null, "org.apache.cassandra.db.Mutation"));
    }

    // --- HDFS ----------------------------------------------------------------

    @Test
    public void hdfs_createMapsToNamespaceMutation() {
        assertEquals(ProtocolFamily.HDFS_CLIENT_NAMESPACE_MUTATION,
                ProtocolFamilyClassifier.classify("hdfs-rpc", null, "ClientNamenodeProtocol",
                        "create", null, null));
    }

    @Test
    public void hdfs_deleteMapsToNamespaceMutation() {
        assertEquals(ProtocolFamily.HDFS_CLIENT_NAMESPACE_MUTATION,
                ProtocolFamilyClassifier.classify("hdfs-rpc", null, "ClientNamenodeProtocol",
                        "delete", null, null));
    }

    @Test
    public void hdfs_addBlockMapsToBlockPipeline() {
        assertEquals(ProtocolFamily.HDFS_CLIENT_BLOCK_PIPELINE,
                ProtocolFamilyClassifier.classify("hdfs-rpc", null, "ClientNamenodeProtocol",
                        "addBlock", null, null));
    }

    @Test
    public void hdfs_blockReportMapsToBlockPipeline() {
        assertEquals(ProtocolFamily.HDFS_CLIENT_BLOCK_PIPELINE,
                ProtocolFamilyClassifier.classify("hdfs-rpc", null, "DatanodeProtocol",
                        "blockReport", null, null));
    }

    @Test
    public void hdfs_journalMapsToJournalQjm() {
        assertEquals(ProtocolFamily.HDFS_JOURNAL_QJM,
                ProtocolFamilyClassifier.classify("hdfs-rpc", null, "QJournalProtocol",
                        "journal", null, null));
    }

    @Test
    public void hdfs_qjournalHeartbeatStaysInJournalWhenServiceHints() {
        assertEquals(ProtocolFamily.HDFS_JOURNAL_QJM,
                ProtocolFamilyClassifier.classify("hdfs-rpc", null, "QJournalProtocol",
                        "newEpoch", null, null));
    }

    @Test
    public void hdfs_haTransitionMapsToHaCoordination() {
        assertEquals(ProtocolFamily.HDFS_HA_COORDINATION,
                ProtocolFamilyClassifier.classify("hdfs-rpc", null, "HAServiceProtocol",
                        "transitionToActive", null, null));
    }

    @Test
    public void hdfs_heartbeatMapsToBackground() {
        assertEquals(ProtocolFamily.BACKGROUND, ProtocolFamilyClassifier.classify("hdfs-rpc",
                null, "DatanodeProtocol", "sendHeartbeat", null, null));
    }

    @Test
    public void hdfs_getFileInfoMapsToBackground() {
        // getFileInfo is a read-mostly query and dominates heartbeat-like traffic.
        assertEquals(ProtocolFamily.BACKGROUND, ProtocolFamilyClassifier.classify("hdfs-rpc",
                null, "ClientNamenodeProtocol", "getFileInfo", null, null));
    }

    @Test
    public void hdfs_unknownMethodMapsToUnknown() {
        assertEquals(ProtocolFamily.UNKNOWN, ProtocolFamilyClassifier.classify("hdfs-rpc", null,
                "ClientNamenodeProtocol", "someUnchartedMethod", null, null));
    }

    // --- HBase ---------------------------------------------------------------

    @Test
    public void hbase_mutateMapsToClientMutation() {
        assertEquals(ProtocolFamily.HBASE_CLIENT_MUTATION_OR_MULTI,
                ProtocolFamilyClassifier.classify("hbase", null, "ClientService", "Mutate", null,
                        null));
    }

    @Test
    public void hbase_multiMapsToClientMutation() {
        assertEquals(ProtocolFamily.HBASE_CLIENT_MUTATION_OR_MULTI,
                ProtocolFamilyClassifier.classify("hbase", null, "ClientService", "Multi", null,
                        null));
    }

    @Test
    public void hbase_mutateKindOnlyMapsToClientMutation() {
        // When the bridge only resolves the mutate subtype but not the
        // rpcMethod, the classifier should still recognise the family.
        assertEquals(ProtocolFamily.HBASE_CLIENT_MUTATION_OR_MULTI,
                ProtocolFamilyClassifier.classify("hbase", null, null, null, "PUT", null));
    }

    @Test
    public void hbase_openRegionMapsToRegionAdminLifecycle() {
        assertEquals(ProtocolFamily.HBASE_REGION_ADMIN_LIFECYCLE,
                ProtocolFamilyClassifier.classify("hbase", null, "AdminService", "OpenRegion",
                        null, null));
    }

    @Test
    public void hbase_replicateWalEntryMapsToRegionAdminLifecycle() {
        assertEquals(ProtocolFamily.HBASE_REGION_ADMIN_LIFECYCLE,
                ProtocolFamilyClassifier.classify("hbase", null, "AdminService",
                        "ReplicateWALEntry", null, null));
    }

    @Test
    public void hbase_createTableMapsToMasterSchemaDdl() {
        assertEquals(ProtocolFamily.HBASE_MASTER_SCHEMA_DDL,
                ProtocolFamilyClassifier.classify("hbase", null, "MasterService", "CreateTable",
                        null, null));
    }

    @Test
    public void hbase_createNamespaceMapsToMasterSchemaDdl() {
        assertEquals(ProtocolFamily.HBASE_MASTER_SCHEMA_DDL,
                ProtocolFamilyClassifier.classify("hbase", null, "MasterService", "CreateNamespace",
                        null, null));
    }

    @Test
    public void hbase_regionServerReportMapsToBackground() {
        assertEquals(ProtocolFamily.BACKGROUND, ProtocolFamilyClassifier.classify("hbase", null,
                "RegionServerStatusService", "regionServerReport", null, null));
    }

    @Test
    public void hbase_unknownMethodMapsToUnknown() {
        assertEquals(ProtocolFamily.UNKNOWN, ProtocolFamilyClassifier.classify("hbase", null,
                "ClientService", "SomeUnchartedMethod", null, null));
    }

    @Test
    public void hbase_masterServiceWithoutMethodMapsToUnknown() {
        // Historical bug: service-name contains "MASTER" would mark every
        // MasterService call as HBASE_MASTER_SCHEMA_DDL, even read-only
        // RunCatalogScan / IsBalancerEnabled chatter. Service-only
        // classification must not promote anything to upgrade-critical.
        assertEquals(ProtocolFamily.UNKNOWN,
                ProtocolFamilyClassifier.classify("hbase", null, "MasterService", null, null, null));
    }

    @Test
    public void hbase_adminServiceWithoutMethodMapsToUnknown() {
        assertEquals(ProtocolFamily.UNKNOWN,
                ProtocolFamilyClassifier.classify("hbase", null, "AdminService", null, null, null));
    }

    @Test
    public void hbase_regionServerStatusServiceWithoutMethodMapsToBackground() {
        // Services that are entirely background still route to BACKGROUND
        // even without a method, because every method on them is chatter.
        assertEquals(ProtocolFamily.BACKGROUND, ProtocolFamilyClassifier.classify("hbase", null,
                "RegionServerStatusService", null, null, null));
    }

    // --- Family-class invariants --------------------------------------------

    @Test
    public void upgradeCriticalFamiliesAdvertiseUpgradeCriticalClass() {
        ProtocolFamily[] upgradeCritical = {
                ProtocolFamily.CASSANDRA_SCHEMA_SYNC, ProtocolFamily.CASSANDRA_PAXOS,
                ProtocolFamily.CASSANDRA_REPAIR_OR_STREAM,
                ProtocolFamily.CASSANDRA_READ_REPAIR_OR_HINT,
                ProtocolFamily.HDFS_CLIENT_NAMESPACE_MUTATION,
                ProtocolFamily.HDFS_CLIENT_BLOCK_PIPELINE, ProtocolFamily.HDFS_JOURNAL_QJM,
                ProtocolFamily.HDFS_HA_COORDINATION,
                ProtocolFamily.HBASE_CLIENT_MUTATION_OR_MULTI,
                ProtocolFamily.HBASE_REGION_ADMIN_LIFECYCLE,
                ProtocolFamily.HBASE_MASTER_SCHEMA_DDL };
        for (ProtocolFamily family : upgradeCritical) {
            assertEquals(ProtocolFamilyClass.UPGRADE_CRITICAL, family.familyClass(),
                    "expected " + family + " to be UPGRADE_CRITICAL");
        }
    }

    @Test
    public void backgroundFamilyIsBackgroundClass() {
        assertEquals(ProtocolFamilyClass.BACKGROUND, ProtocolFamily.BACKGROUND.familyClass());
    }

    @Test
    public void unknownFamilyIsUnknownClass() {
        assertEquals(ProtocolFamilyClass.UNKNOWN, ProtocolFamily.UNKNOWN.familyClass());
    }

    @Test
    public void classifyClass_matchesClassifyFamilyClass() {
        assertEquals(ProtocolFamily.CASSANDRA_SCHEMA_SYNC.familyClass(),
                ProtocolFamilyClassifier.classifyClass("cassandra", "SCHEMA_PULL_REQ", null, null,
                        null, null));
    }

    @Test
    public void classifier_missingSystemInfoFallsBackToUnknown() {
        assertEquals(ProtocolFamily.UNKNOWN,
                ProtocolFamilyClassifier.classify(null, null, null, null, null, null));
    }
}
