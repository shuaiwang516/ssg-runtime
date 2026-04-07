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
        // Example aliases (populate from pilot data):
        // ALIASES.put("OldTypeName", "CanonicalTypeName");
        // ALIASES.put("RenamedRequest", "OriginalRequest");
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
