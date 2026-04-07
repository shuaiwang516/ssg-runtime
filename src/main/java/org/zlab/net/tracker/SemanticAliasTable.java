package org.zlab.net.tracker;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps raw semantic type strings to canonical families. Collapses
 * renamed-but-equivalent message wrappers across versions.
 *
 * Start empty in v1. Populate from pilot observations where identical
 * operations produce different raw type names.
 */
public final class SemanticAliasTable {
    private SemanticAliasTable() {
    }

    private static final Map<String, String> ALIASES = new HashMap<>();

    static {
        // Cassandra 3.x -> 4.x: read-response inner class renamed
        // 3.x: ReadResponse$LocalDataResponse (inner class)
        // 4.x: same name but may appear as ReadResponse in some paths
        // Both should canonicalize to the same type.

        // Cassandra 3.x -> 4.x: mutation send wrappers
        // 3.x sends Mutation in Collections$SingletonList
        // 4.x sends Mutation in ArrayList
        // Both stripped to payloadType by isGenericWrapper(); if fallback
        // hits the wrapper, canonicalize to same name.
        ALIASES.put("SingletonList", "COLLECTION_WRAPPER");
        ALIASES.put("ArrayList", "COLLECTION_WRAPPER");
        ALIASES.put("Values", "COLLECTION_WRAPPER");
        ALIASES.put("UnmodifiableCollection", "COLLECTION_WRAPPER");

        // Cassandra 3.x generic message wrappers (should not reach here
        // because isGenericWrapper filters them, but safety net)
        ALIASES.put("MessageOut", "CASSANDRA_MSG_WRAPPER");
        ALIASES.put("MessageIn", "CASSANDRA_MSG_WRAPPER");

        // HDFS: protobuf request class names are stable across 2.x -> 3.x.
        // No aliases needed from pilot data.

        // HBase: request names are stable across 2.x.
        // No aliases needed from pilot data.
    }

    /**
     * Returns the canonical family for the given raw type. If no alias exists,
     * returns the raw type unchanged.
     */
    public static String canonicalize(String rawType) {
        if (rawType == null)
            return "UNKNOWN_TYPE";
        String alias = ALIASES.get(rawType);
        return alias != null ? alias : rawType;
    }
}
