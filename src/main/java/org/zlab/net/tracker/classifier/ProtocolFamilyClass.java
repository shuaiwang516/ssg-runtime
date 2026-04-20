package org.zlab.net.tracker.classifier;

/**
 * Coarse weight class applied on top of a {@link ProtocolFamily}. Phase 1 uses
 * this to let the scorer downweight gossip / heartbeat / status chatter while
 * still keeping the families visible for observability.
 *
 * <ul>
 * <li>{@link #UPGRADE_CRITICAL}: families whose semantics are directly
 * implicated by rolling upgrades — schema changes, mutation pipelines, journal
 * / HA coordination, master DDL, region-admin lifecycle.</li>
 * <li>{@link #BACKGROUND}: families that produce steady chatter regardless of
 * test activity (gossip, heartbeats, status reports, catalog scans). Visible
 * but cannot be the main reason a window is promoted to strong.</li>
 * <li>{@link #UNKNOWN}: everything the first-cut classifier cannot confidently
 * place. Kept distinct from {@code BACKGROUND} so Phase 5 profiles can narrow
 * the unclassified tail without losing signal from confidently-background
 * traffic.</li>
 * </ul>
 */
public enum ProtocolFamilyClass {
    UPGRADE_CRITICAL, BACKGROUND, UNKNOWN
}
