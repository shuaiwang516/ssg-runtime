package org.zlab.net.tracker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Normalizes {@link TraceEntry#messageSummary} strings into stable hashed
 * buckets for use in {@link CanonicalKeyMode#SEMANTIC_SHAPE_SUMMARY}.
 *
 * <p>
 * The raw summary produced by {@link MessageFingerprint} is a
 * {@code '|'}-delimited list of tokens mixing class simple-names, field values,
 * container sizes, and scalar literals. Using it directly as a key fragment
 * would explode the key space and break cross-version matching.
 * {@link SummaryBucket} tokenizes the summary, drops volatile wrapper names and
 * context-dependent sizes, replaces numbers/UUIDs/hex/long type names with
 * placeholder tokens, truncates to a bounded token count, and returns a
 * hex-encoded 64-bit FNV-1a hash of the normalized sequence.
 *
 * <p>
 * Properties callers rely on:
 * <ul>
 * <li>Stable across versions when the bucketed token sequence matches — numeric
 * field values, container sizes, and hex ids do not affect the bucket.</li>
 * <li>Non-empty / non-null inputs never hash to the sentinel {@code "-"}.</li>
 * <li>Token limit protects the hash domain from unbounded summary growth.</li>
 * </ul>
 */
final class SummaryBucket {

    private static final int TOKEN_LIMIT = 12;

    private static final Pattern NUMBER_TOKEN_PATTERN = Pattern.compile("^-?\\d+(?:\\.\\d+)?$");
    private static final Pattern UUID_TOKEN_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final Pattern HEX_TOKEN_PATTERN = Pattern.compile("^[0-9a-fA-F]{8,}$");

    private static final Set<String> VOLATILE_TOKENS = new HashSet<>(
            Arrays.asList("Message", "Header", "InetAddressAndPort", "byte[]", "AtomicReference",
                    "CachedSerialization", "Serialization[]", "HeapByteBuffer"));

    private SummaryBucket() {
    }

    /**
     * Returns the hashed bucket for a message summary. Returns the sentinel
     * {@code "-"} for null or empty inputs and for inputs whose tokens all
     * normalize away.
     */
    static String bucketHash(String summary) {
        if (summary == null || summary.isEmpty()) {
            return "-";
        }

        String[] rawTokens = summary.split("\\|");
        List<String> normalized = new ArrayList<>();
        for (String rawToken : rawTokens) {
            String token = normalizeToken(rawToken);
            if (token == null || token.isEmpty()) {
                continue;
            }
            if (!normalized.isEmpty() && token.equals(normalized.get(normalized.size() - 1))) {
                continue;
            }
            normalized.add(token);
            if (normalized.size() >= TOKEN_LIMIT) {
                break;
            }
        }

        if (normalized.isEmpty()) {
            return "-";
        }

        String canonical = String.join("|", normalized);
        return Long.toHexString(fnv1a64(canonical));
    }

    private static String normalizeToken(String rawToken) {
        if (rawToken == null) {
            return null;
        }
        String token = rawToken.trim();
        if (token.isEmpty()) {
            return null;
        }
        if (VOLATILE_TOKENS.contains(token)) {
            return null;
        }
        if (token.startsWith("[len=")) {
            return "[len=*]";
        }
        if (token.startsWith("(size=")) {
            return "(size=*)";
        }
        if (token.startsWith("{size=")) {
            return "{size=*}";
        }
        if (token.startsWith("<value:")) {
            return "<value>";
        }
        if (UUID_TOKEN_PATTERN.matcher(token).matches()) {
            return "<uuid>";
        }
        if (NUMBER_TOKEN_PATTERN.matcher(token).matches()) {
            return "<n>";
        }
        if (HEX_TOKEN_PATTERN.matcher(token).matches()) {
            return "<hex>";
        }
        if (token.indexOf('.') >= 0 && token.length() > 24) {
            token = shortTypeName(token);
        }
        // Pass class-simple-name tokens through the cross-version alias table so
        // wrapper renames (e.g. SingletonList <-> ArrayList) collapse to the same
        // bucket content. The alias table is a no-op for non-wrapper tokens.
        return SemanticAliasTable.canonicalize(token);
    }

    private static String shortTypeName(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        int idx = value.lastIndexOf('.');
        if (idx < 0 || idx + 1 >= value.length()) {
            return value;
        }
        return value.substring(idx + 1);
    }

    private static long fnv1a64(String value) {
        long hash = 0xcbf29ce484222325L;
        for (int i = 0; i < value.length(); i++) {
            hash ^= value.charAt(i);
            hash *= 0x100000001b3L;
        }
        return hash;
    }
}
