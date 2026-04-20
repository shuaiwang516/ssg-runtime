package org.zlab.net.tracker.classifier;

/**
 * First-cut protocol family taxonomy used as the online guidance identity.
 *
 * <p>
 * The taxonomy is intentionally small — Phase 1 only commits to the dominant
 * upgrade-critical families for each system plus two catch-all buckets
 * ({@link #BACKGROUND}, {@link #UNKNOWN}). Long-tail verb / method coverage is
 * deferred to Phase 5 version-aware profile generation. See
 * {@code cloudlab-fix-plans/apr16/phases/2026-04-17-phase-1-online-identity
 * -split-and-family-canonicalization.md} for the decision record.
 *
 * <p>
 * Each family is tagged with a {@link ProtocolFamilyClass} so the scorer can
 * distinguish upgrade-critical traffic from background chatter without
 * re-running the classifier. {@code BACKGROUND} and {@code UNKNOWN} are shared
 * across systems; all other families are system-specific.
 */
public enum ProtocolFamily {
    // --- Cassandra ---
    /** Cassandra schema sync / migration traffic. */
    CASSANDRA_SCHEMA_SYNC(ProtocolFamilyClass.UPGRADE_CRITICAL),
    /** Cassandra Paxos (v1 and v2) prepare / propose / commit verbs. */
    CASSANDRA_PAXOS(ProtocolFamilyClass.UPGRADE_CRITICAL),
    /** Cassandra repair, streaming, validation, snapshot verbs. */
    CASSANDRA_REPAIR_OR_STREAM(ProtocolFamilyClass.UPGRADE_CRITICAL),
    /** Cassandra read-repair, hint delivery, batch-remove verbs. */
    CASSANDRA_READ_REPAIR_OR_HINT(ProtocolFamilyClass.UPGRADE_CRITICAL),

    // --- HDFS ---
    /** HDFS client namespace mutations (create, rename, delete, etc.). */
    HDFS_CLIENT_NAMESPACE_MUTATION(ProtocolFamilyClass.UPGRADE_CRITICAL),
    /** HDFS client block pipeline and block reports. */
    HDFS_CLIENT_BLOCK_PIPELINE(ProtocolFamilyClass.UPGRADE_CRITICAL),
    /** HDFS QJournal (journal-write / log-segment / recovery) traffic. */
    HDFS_JOURNAL_QJM(ProtocolFamilyClass.UPGRADE_CRITICAL),
    /** HDFS HA coordination (HAServiceProtocol transitions, ZKFC). */
    HDFS_HA_COORDINATION(ProtocolFamilyClass.UPGRADE_CRITICAL),

    // --- HBase ---
    /** HBase client mutations (Mutate, Multi) including subtype refinement. */
    HBASE_CLIENT_MUTATION_OR_MULTI(ProtocolFamilyClass.UPGRADE_CRITICAL),
    /**
     * HBase region-admin lifecycle (open/close/split/merge/compact, WAL
     * replication).
     */
    HBASE_REGION_ADMIN_LIFECYCLE(ProtocolFamilyClass.UPGRADE_CRITICAL),
    /** HBase master schema DDL (create/delete/modify table, namespace, assign). */
    HBASE_MASTER_SCHEMA_DDL(ProtocolFamilyClass.UPGRADE_CRITICAL),

    // --- Shared catch-alls ---
    /**
     * Gossip / heartbeat / status-report chatter. Visible but not
     * strong-window-eligible on its own.
     */
    BACKGROUND(ProtocolFamilyClass.BACKGROUND),
    /** Nothing confident to say; Phase 5 profiles will narrow this tail. */
    UNKNOWN(ProtocolFamilyClass.UNKNOWN);

    private final ProtocolFamilyClass familyClass;

    ProtocolFamily(ProtocolFamilyClass familyClass) {
        this.familyClass = familyClass;
    }

    public ProtocolFamilyClass familyClass() {
        return familyClass;
    }

    /**
     * Stable short token for use in canonical keys. Returns the enum name directly
     * — keys embed it, so renaming an enum constant changes the key and invalidates
     * diff history.
     */
    public String token() {
        return name();
    }
}
