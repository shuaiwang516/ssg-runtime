package org.zlab.net.tracker.classifier;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * First-cut protocol-family classifier for rolling-upgrade online guidance.
 *
 * <p>
 * Phase 1 deliberately keeps this classifier small — it only commits to the
 * dominant upgrade-critical families per system and routes everything else to
 * {@link ProtocolFamily#BACKGROUND} or {@link ProtocolFamily#UNKNOWN}.
 * Long-tail verb and method coverage is the job of Phase 5 profile generation,
 * not this file.
 *
 * <p>
 * The classifier consumes the raw attributes populated by the instrumentation
 * bridge:
 * <ul>
 * <li>{@code protocol}: system identifier ({@code cassandra}, {@code hdfs-rpc},
 * {@code hbase}).</li>
 * <li>{@code messageType}: Cassandra internode verb / HBase simple type / HDFS
 * request class name.</li>
 * <li>{@code rpcService}: HDFS protocol interface or HBase protobuf service
 * (when the bridge can resolve it).</li>
 * <li>{@code rpcMethod}: the method within {@code rpcService}.</li>
 * <li>{@code messageKind}: optional subtype qualifier (HBase mutate subtype,
 * etc.).</li>
 * <li>{@code payloadType}: simple payload class name (useful as a last-resort
 * fallback).</li>
 * </ul>
 *
 * <p>
 * Classification priority within a system matches the Phase 1 plan:
 * <ol>
 * <li>Cassandra: internode verb → {@code messageKind} / channel → payload
 * type.</li>
 * <li>HDFS: {@code rpcService} + {@code rpcMethod} → payload wrapper
 * class.</li>
 * <li>HBase: {@code rpcService} + {@code rpcMethod} → {@code messageKind} for
 * mutate subtype → request class.</li>
 * </ol>
 */
public final class ProtocolFamilyClassifier {

    private ProtocolFamilyClassifier() {
    }

    // --- Cassandra verb sets -------------------------------------------------

    private static final Set<String> CASSANDRA_SCHEMA_VERBS = buildVerbSet(
            // 3.x verb names (MessagingService.Verb)
            "SCHEMA_CHECK", "MIGRATION_REQUEST", "MIGRATION_RESPONSE", "DEFINITIONS_ANNOUNCE",
            "DEFINITIONS_UPDATE",
            // 4.x / 5.x verb enum names (Verb)
            "SCHEMA_PULL_REQ", "SCHEMA_PULL_RSP", "SCHEMA_PUSH_REQ", "SCHEMA_PUSH_RSP",
            "SCHEMA_VERSION_REQ", "SCHEMA_VERSION_RSP");

    private static final Set<String> CASSANDRA_PAXOS_VERBS = buildVerbSet("PAXOS_PREPARE",
            "PAXOS_PROPOSE", "PAXOS_COMMIT", "PAXOS_PREPARE_REQ", "PAXOS_PREPARE_RSP",
            "PAXOS_PROPOSE_REQ", "PAXOS_PROPOSE_RSP", "PAXOS_COMMIT_REQ", "PAXOS_COMMIT_RSP",
            "PAXOS2_PREPARE_REQ", "PAXOS2_PREPARE_RSP", "PAXOS2_PROPOSE_REQ", "PAXOS2_PROPOSE_RSP",
            "PAXOS2_COMMIT_REMOTE_REQ", "PAXOS2_COMMIT_REMOTE_RSP", "PAXOS2_REPAIR_REQ",
            "PAXOS2_REPAIR_RSP");

    private static final Set<String> CASSANDRA_REPAIR_STREAM_VERBS = buildVerbSet("REPAIR_MESSAGE",
            "REPAIR_REQ", "REPAIR_RSP", "PREPARE_MESSAGE", "VALIDATION_REQ", "VALIDATION_RSP",
            "SYNC_REQ", "SYNC_RSP", "ASYMMETRIC_SYNC_REQ", "STREAM_REQUEST", "STREAM_INITIATE",
            "STREAM_INIT_MESSAGE", "STREAM_REPLY", "STREAM_COMPLETE", "SNAPSHOT", "SNAPSHOT_MSG",
            "SNAPSHOT_REQ", "SNAPSHOT_RSP", "PREPARE_CONSISTENT_REQ", "PREPARE_CONSISTENT_RSP",
            "FINALIZE_PROPOSE_MSG", "FINALIZE_COMMIT_MSG", "FAILED_SESSION_MSG", "STATUS_REQ",
            "STATUS_RSP", "CLEANUP_MSG", "ANTICOMPACTION_REQ");

    private static final Set<String> CASSANDRA_READ_REPAIR_HINT_VERBS = buildVerbSet("READ_REPAIR",
            "READ_REPAIR_REQ", "READ_REPAIR_RSP", "HINT", "HINT_REQ", "HINT_RSP", "HINTED_HANDOFF",
            "BATCH_REMOVE", "BATCH_REMOVE_REQ", "BATCH_REMOVE_RSP", "BATCH_STORE",
            "BATCH_STORE_REQ", "BATCH_STORE_RSP");

    private static final Set<String> CASSANDRA_BACKGROUND_VERBS = buildVerbSet("GOSSIP_DIGEST_SYN",
            "GOSSIP_DIGEST_ACK", "GOSSIP_DIGEST_ACK2", "GOSSIP_SHUTDOWN", "GOSSIP_SYN",
            "GOSSIP_ACK", "GOSSIP_ACK2", "ECHO", "ECHO_MSG", "ECHO_REQ", "ECHO_RSP", "PING",
            "PING_REQ", "PING_RSP", "PING_MSG", "_TRACE", "_TEST_1", "_TEST_2", "REQUEST_RESPONSE",
            "INTERNAL_RESPONSE", "COUNTER_MUTATION_REQ", "COUNTER_MUTATION_RSP");

    // Payload-type suffixes used as last-resort classification when the
    // Cassandra bridge only recorded a payload class name (for instance
    // generic MessageOut wrappers on 3.x). Kept intentionally narrow:
    // general "Mutation" payloads are ambiguous between schema mutations,
    // client writes, hint mutations, and repair mutations, so they stay
    // in UNKNOWN until a more specific signal is available.
    private static final Set<String> CASSANDRA_SCHEMA_PAYLOAD_SUFFIXES = buildVerbSet(
            "SchemaPullVerbHandler", "SchemaPushVerbHandler", "MigrationTask",
            "MigrationManager$MigrationTask");
    private static final Set<String> CASSANDRA_GOSSIP_PAYLOAD_SUFFIXES = buildVerbSet(
            "GossipDigestSyn", "GossipDigestAck", "GossipDigestAck2", "GossipDigest",
            "EndpointState", "HeartBeatState", "EchoMessage", "PingMessage");

    // --- HDFS rpcService / rpcMethod sets ------------------------------------

    private static final Set<String> HDFS_NAMESPACE_MUTATION_METHODS = buildVerbSet("create",
            "mkdirs", "rename", "rename2", "delete", "append", "truncate", "concat",
            "setReplication", "setStoragePolicy", "unsetStoragePolicy", "satisfyStoragePolicy",
            "setErasureCodingPolicy", "unsetErasureCodingPolicy", "setOwner", "setPermission",
            "setTimes", "setXAttr", "removeXAttr", "setAcl", "modifyAclEntries", "removeAclEntries",
            "removeDefaultAcl", "removeAcl", "createSymlink", "createEncryptionZone",
            "reencryptEncryptionZone", "addCacheDirective", "modifyCacheDirective",
            "removeCacheDirective", "addCachePool", "modifyCachePool", "removeCachePool",
            "setQuota", "setQuotaByStorageType", "allowSnapshot", "disallowSnapshot",
            "createSnapshot", "deleteSnapshot", "renameSnapshot", "getSnapshotDiffReport",
            "getSnapshotDiffReportListing", "enableErasureCodingPolicy",
            "disableErasureCodingPolicy", "addErasureCodingPolicies", "removeErasureCodingPolicy");

    private static final Set<String> HDFS_BLOCK_PIPELINE_METHODS = buildVerbSet("addBlock",
            "abandonBlock", "complete", "getAdditionalDatanode", "updateBlockForPipeline",
            "updatePipeline", "getBlockLocations", "reportBadBlocks", "getDataEncryptionKey",
            "getListing", "getBlockLocationsForPath",
            // DatanodeProtocol
            "blockReport", "blockReceivedAndDeleted", "commitBlockSynchronization", "errorReport",
            "cacheReport", "blocksBeingWrittenReport", "slowDisksReport", "slowPeersReport",
            "blockReportForAllStorages");

    private static final Set<String> HDFS_JOURNAL_METHODS = buildVerbSet("journal",
            "startLogSegment", "finalizeLogSegment", "getEditLogManifest", "getJournalState",
            "newEpoch", "prepareRecovery", "acceptRecovery", "getEditLogManifestFromJournal",
            "getEditLogStreams", "getJournalCTime", "heartbeat", "canRollBack", "discardSegments",
            "doPreUpgrade", "doUpgrade", "doFinalize", "doRollback");

    private static final Set<String> HDFS_HA_METHODS = buildVerbSet("monitorHealth",
            "transitionToActive", "transitionToStandby", "transitionToObserver", "getServiceStatus",
            "cedeActive", "gracefulFailover", "checkAccess");

    private static final Set<String> HDFS_BACKGROUND_METHODS = buildVerbSet("sendHeartbeat",
            "registerDatanode", "versionRequest", "getStats", "getDatanodeReport",
            "getDatanodeStorageReport", "getPreferredBlockSize", "rollingUpgrade", "getFileInfo",
            "getFileLinkInfo", "getContentSummary", "getServerDefaults", "getDelegationToken",
            "renewDelegationToken", "cancelDelegationToken", "rollEditLog", "saveNamespace",
            "restoreFailedStorage", "refreshNodes", "getListEncryptionZones", "listCacheDirectives",
            "listCachePools");

    // --- HBase rpcService / rpcMethod sets -----------------------------------

    private static final Set<String> HBASE_MUTATION_MULTI_METHODS = buildVerbSet("Mutate", "mutate",
            "Multi", "multi", "bulkLoadHFile", "BulkLoadHFile", "prepareBulkLoad",
            "PrepareBulkLoad", "cleanupBulkLoad", "CleanupBulkLoad");

    private static final Set<String> HBASE_REGION_ADMIN_METHODS = buildVerbSet("OpenRegion",
            "openRegion", "CloseRegion", "closeRegion", "OpenRegions", "openRegions",
            "CloseRegions", "closeRegions", "SplitRegion", "splitRegion", "CompactRegion",
            "compactRegion", "FlushRegion", "flushRegion", "MergeRegions", "mergeRegions",
            "WarmupRegion", "warmupRegion", "GetRegionInfo", "getRegionInfo", "StopServer",
            "stopServer", "RollWALWriter", "rollWALWriter", "ExecService", "execService",
            "ExecRegionServerService", "execRegionServerService", "ClearRegionBlockCache",
            "clearRegionBlockCache", "Replay", "replay", "UpdateFavoredNodes", "updateFavoredNodes",
            "CompactionSwitch", "compactionSwitch", "ExecuteProcedures", "executeProcedures",
            "ReplicateWALEntry", "replicateWALEntry");

    private static final Set<String> HBASE_MASTER_DDL_METHODS = buildVerbSet("CreateTable",
            "createTable", "DeleteTable", "deleteTable", "ModifyTable", "modifyTable",
            "TruncateTable", "truncateTable", "EnableTable", "enableTable", "DisableTable",
            "disableTable", "CreateNamespace", "createNamespace", "DeleteNamespace",
            "deleteNamespace", "ModifyNamespace", "modifyNamespace", "AddColumn", "addColumn",
            "DeleteColumn", "deleteColumn", "ModifyColumn", "modifyColumn", "AddColumnFamily",
            "addColumnFamily", "DeleteColumnFamily", "deleteColumnFamily", "ModifyColumnFamily",
            "modifyColumnFamily", "MoveRegion", "moveRegion", "AssignRegion", "assignRegion",
            "UnassignRegion", "unassignRegion", "OfflineRegion", "offlineRegion", "RestoreSnapshot",
            "restoreSnapshot", "CloneSnapshot", "cloneSnapshot", "SnapshotTable", "snapshotTable",
            "DeleteSnapshot", "deleteSnapshot", "GrantPermission", "grantPermission",
            "RevokePermission", "revokePermission", "SetSplitOrMergeEnabled",
            "setSplitOrMergeEnabled");

    private static final Set<String> HBASE_BACKGROUND_METHODS = buildVerbSet("RegionServerReport",
            "regionServerReport", "RegionServerStartup", "regionServerStartup",
            "ReportRSFatalError", "reportRSFatalError", "GetLastFlushedSequenceId",
            "getLastFlushedSequenceId", "ReportProcedureDone", "reportProcedureDone", "Heartbeat",
            "heartbeat", "GetClusterStatus", "getClusterStatus", "IsMasterRunning",
            "isMasterRunning", "IsBalancerEnabled", "isBalancerEnabled", "IsNormalizerEnabled",
            "isNormalizerEnabled", "IsCatalogJanitorEnabled", "isCatalogJanitorEnabled",
            "RunCatalogScan", "runCatalogScan", "RunCleanerChore", "runCleanerChore",
            "ListProcedures", "listProcedures", "Get", "get", "Scan", "scan", "CoprocessorService",
            "coprocessorService", "ExecMasterService", "execMasterService");

    /**
     * Classify the message described by the raw attributes. Returns
     * {@link ProtocolFamily#UNKNOWN} when the first-cut rules do not match.
     */
    public static ProtocolFamily classify(String protocol, String messageType, String rpcService,
            String rpcMethod, String messageKind, String payloadType) {
        String system = normalizeSystem(protocol, messageType, rpcService, rpcMethod, payloadType);
        if ("cassandra".equals(system)) {
            return classifyCassandra(messageType, messageKind, payloadType);
        }
        if ("hdfs".equals(system)) {
            return classifyHdfs(rpcService, rpcMethod, messageType, payloadType);
        }
        if ("hbase".equals(system)) {
            return classifyHbase(rpcService, rpcMethod, messageKind, messageType, payloadType);
        }
        return ProtocolFamily.UNKNOWN;
    }

    /**
     * Convenience overload that returns the {@link ProtocolFamilyClass} directly.
     */
    public static ProtocolFamilyClass classifyClass(String protocol, String messageType,
            String rpcService, String rpcMethod, String messageKind, String payloadType) {
        return classify(protocol, messageType, rpcService, rpcMethod, messageKind, payloadType)
                .familyClass();
    }

    private static ProtocolFamily classifyCassandra(String messageType, String messageKind,
            String payloadType) {
        String verb = upperOrNull(messageType);
        if (verb != null) {
            if (CASSANDRA_SCHEMA_VERBS.contains(verb)) {
                return ProtocolFamily.CASSANDRA_SCHEMA_SYNC;
            }
            if (CASSANDRA_PAXOS_VERBS.contains(verb) || verb.startsWith("PAXOS")) {
                return ProtocolFamily.CASSANDRA_PAXOS;
            }
            if (CASSANDRA_REPAIR_STREAM_VERBS.contains(verb)) {
                return ProtocolFamily.CASSANDRA_REPAIR_OR_STREAM;
            }
            if (CASSANDRA_READ_REPAIR_HINT_VERBS.contains(verb)) {
                return ProtocolFamily.CASSANDRA_READ_REPAIR_OR_HINT;
            }
            if (CASSANDRA_BACKGROUND_VERBS.contains(verb) || verb.startsWith("GOSSIP_")
                    || verb.startsWith("ECHO")) {
                return ProtocolFamily.BACKGROUND;
            }
        }
        String kind = upperOrNull(messageKind);
        if (kind != null) {
            if (kind.startsWith("SCHEMA")) {
                return ProtocolFamily.CASSANDRA_SCHEMA_SYNC;
            }
            if (kind.startsWith("PAXOS")) {
                return ProtocolFamily.CASSANDRA_PAXOS;
            }
            if (kind.startsWith("REPAIR") || kind.startsWith("STREAM")) {
                return ProtocolFamily.CASSANDRA_REPAIR_OR_STREAM;
            }
            if (kind.startsWith("HINT") || kind.startsWith("READ_REPAIR")) {
                return ProtocolFamily.CASSANDRA_READ_REPAIR_OR_HINT;
            }
            if (kind.startsWith("GOSSIP") || kind.equals("HEARTBEAT") || kind.equals("ECHO")
                    || kind.equals("PING")) {
                return ProtocolFamily.BACKGROUND;
            }
        }
        String payload = shortClassName(payloadType);
        if (payload != null) {
            if (CASSANDRA_SCHEMA_PAYLOAD_SUFFIXES.contains(payload)) {
                return ProtocolFamily.CASSANDRA_SCHEMA_SYNC;
            }
            if (CASSANDRA_GOSSIP_PAYLOAD_SUFFIXES.contains(payload)) {
                return ProtocolFamily.BACKGROUND;
            }
            if (payload.startsWith("Paxos")) {
                return ProtocolFamily.CASSANDRA_PAXOS;
            }
            if (payload.startsWith("Repair") || payload.startsWith("Stream")
                    || payload.startsWith("Sync") || payload.startsWith("Validation")) {
                return ProtocolFamily.CASSANDRA_REPAIR_OR_STREAM;
            }
            if (payload.startsWith("Hint") || payload.startsWith("ReadRepair")
                    || payload.startsWith("BatchRemove")) {
                return ProtocolFamily.CASSANDRA_READ_REPAIR_OR_HINT;
            }
        }
        return ProtocolFamily.UNKNOWN;
    }

    private static ProtocolFamily classifyHdfs(String rpcService, String rpcMethod,
            String messageType, String payloadType) {
        String method = trimToNull(rpcMethod);
        String service = trimToNull(rpcService);
        if (method != null) {
            if (HDFS_NAMESPACE_MUTATION_METHODS.contains(method)) {
                return ProtocolFamily.HDFS_CLIENT_NAMESPACE_MUTATION;
            }
            if (HDFS_BLOCK_PIPELINE_METHODS.contains(method)) {
                return ProtocolFamily.HDFS_CLIENT_BLOCK_PIPELINE;
            }
            if (HDFS_JOURNAL_METHODS.contains(method)) {
                if (service != null && service.contains("QJournal")) {
                    return ProtocolFamily.HDFS_JOURNAL_QJM;
                }
                // heartbeat is shared between many services; without the
                // QJournal hint we treat it as background chatter.
                if ("heartbeat".equals(method)) {
                    return ProtocolFamily.BACKGROUND;
                }
                return ProtocolFamily.HDFS_JOURNAL_QJM;
            }
            if (HDFS_HA_METHODS.contains(method)) {
                return ProtocolFamily.HDFS_HA_COORDINATION;
            }
            if (HDFS_BACKGROUND_METHODS.contains(method)) {
                return ProtocolFamily.BACKGROUND;
            }
        }
        // Without rpcMethod, lean on request class name conventions
        String simpleType = shortClassName(messageType);
        String simplePayload = shortClassName(payloadType);
        String candidate = simpleType != null ? simpleType : simplePayload;
        if (candidate != null) {
            String upper = candidate.toUpperCase();
            if (upper.contains("JOURNAL") || upper.contains("EDITLOG")) {
                return ProtocolFamily.HDFS_JOURNAL_QJM;
            }
            if (upper.contains("HEARTBEAT") || upper.contains("REGISTER")
                    || upper.contains("VERSIONREQUEST") || upper.contains("GETFILEINFO")) {
                return ProtocolFamily.BACKGROUND;
            }
            if (upper.contains("BLOCK") || upper.contains("PIPELINE")) {
                return ProtocolFamily.HDFS_CLIENT_BLOCK_PIPELINE;
            }
            if (upper.contains("CREATE") || upper.contains("RENAME") || upper.contains("DELETE")
                    || upper.contains("MKDIR") || upper.contains("TRUNCATE")
                    || upper.contains("APPEND") || upper.contains("SETPERMISSION")) {
                return ProtocolFamily.HDFS_CLIENT_NAMESPACE_MUTATION;
            }
        }
        return ProtocolFamily.UNKNOWN;
    }

    private static ProtocolFamily classifyHbase(String rpcService, String rpcMethod,
            String messageKind, String messageType, String payloadType) {
        String method = trimToNull(rpcMethod);
        String service = trimToNull(rpcService);
        if (method != null) {
            if (HBASE_MUTATION_MULTI_METHODS.contains(method)) {
                return ProtocolFamily.HBASE_CLIENT_MUTATION_OR_MULTI;
            }
            if (HBASE_REGION_ADMIN_METHODS.contains(method)) {
                return ProtocolFamily.HBASE_REGION_ADMIN_LIFECYCLE;
            }
            if (HBASE_MASTER_DDL_METHODS.contains(method)) {
                return ProtocolFamily.HBASE_MASTER_SCHEMA_DDL;
            }
            if (HBASE_BACKGROUND_METHODS.contains(method)) {
                return ProtocolFamily.BACKGROUND;
            }
        }
        if (service != null) {
            // Service-only fallbacks are limited to BACKGROUND families.
            // MasterService and AdminService both carry a mix of
            // upgrade-critical and background methods, so claiming
            // upgrade-critical from the service name alone (without the
            // matching method) would over-promote status / heartbeat
            // traffic. Those cases stay UNKNOWN until a method signal
            // arrives.
            String upperService = service.toUpperCase();
            if (upperService.contains("REGIONSERVERSTATUS")
                    || upperService.contains("LOCKSERVICE")) {
                return ProtocolFamily.BACKGROUND;
            }
        }
        String kind = upperOrNull(messageKind);
        if (kind != null) {
            if (kind.equals("PUT") || kind.equals("DELETE") || kind.equals("INCREMENT")
                    || kind.equals("APPEND") || kind.equals("CHECK_AND_MUTATE")) {
                return ProtocolFamily.HBASE_CLIENT_MUTATION_OR_MULTI;
            }
        }
        String simpleType = shortClassName(messageType);
        String simplePayload = shortClassName(payloadType);
        String candidate = simpleType != null ? simpleType : simplePayload;
        if (candidate != null) {
            String upper = candidate.toUpperCase();
            if (upper.contains("MUTATE") || upper.contains("MUTATION")
                    || upper.contains("MULTIREQUEST")) {
                return ProtocolFamily.HBASE_CLIENT_MUTATION_OR_MULTI;
            }
            if (upper.contains("ADMIN") || upper.contains("OPENREGION")
                    || upper.contains("CLOSEREGION") || upper.contains("FLUSHREGION")
                    || upper.contains("COMPACTREGION") || upper.contains("REPLICATEWAL")) {
                return ProtocolFamily.HBASE_REGION_ADMIN_LIFECYCLE;
            }
            if (upper.contains("CREATETABLE") || upper.contains("DELETETABLE")
                    || upper.contains("MODIFYTABLE") || upper.contains("NAMESPACE")
                    || upper.contains("DDL")) {
                return ProtocolFamily.HBASE_MASTER_SCHEMA_DDL;
            }
            if (upper.contains("HEARTBEAT") || upper.contains("REPORT")) {
                return ProtocolFamily.BACKGROUND;
            }
        }
        return ProtocolFamily.UNKNOWN;
    }

    private static String normalizeSystem(String protocol, String messageType, String rpcService,
            String rpcMethod, String payloadType) {
        String protoTrim = trimToNull(protocol);
        if (protoTrim != null) {
            String lower = protoTrim.toLowerCase();
            if (lower.startsWith("cassandra")) {
                return "cassandra";
            }
            if (lower.startsWith("hdfs") || lower.contains("hadoop.ipc")
                    || lower.contains("hadoop.hdfs")) {
                return "hdfs";
            }
            if (lower.startsWith("hbase")) {
                return "hbase";
            }
        }
        // Infer from other attributes when protocol is missing
        String anyText = anyOf(messageType, rpcService, rpcMethod, payloadType);
        if (anyText == null) {
            return null;
        }
        String lower = anyText.toLowerCase();
        if (lower.contains("cassandra")) {
            return "cassandra";
        }
        if (lower.contains("hbase")) {
            return "hbase";
        }
        if (lower.contains("hadoop") || lower.contains("hdfs") || lower.contains("namenode")
                || lower.contains("datanode") || lower.contains("qjournal")) {
            return "hdfs";
        }
        return null;
    }

    private static String anyOf(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String trimmed = trimToNull(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || "null".equals(trimmed)) {
            return null;
        }
        return trimmed;
    }

    private static String upperOrNull(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase();
    }

    private static String shortClassName(String fqcn) {
        String trimmed = trimToNull(fqcn);
        if (trimmed == null) {
            return null;
        }
        int dollar = trimmed.lastIndexOf('$');
        if (dollar >= 0 && dollar + 1 < trimmed.length()) {
            return trimmed.substring(dollar + 1);
        }
        int dot = trimmed.lastIndexOf('.');
        if (dot >= 0 && dot + 1 < trimmed.length()) {
            return trimmed.substring(dot + 1);
        }
        return trimmed;
    }

    private static Set<String> buildVerbSet(String... verbs) {
        Set<String> set = new HashSet<>();
        for (String verb : verbs) {
            set.add(verb);
        }
        return Collections.unmodifiableSet(set);
    }
}
