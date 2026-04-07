package org.zlab.net.tracker;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic message fingerprint used by network trace identity diffing.
 */
public final class MessageFingerprint {
    private static final int MAX_DEPTH = 8;
    private static final int MAX_OBJECTS = 4096;
    private static final int MAX_CONTAINER_ELEMENTS = 16;
    private static final int MAX_SUMMARY_CHARS = 320;
    private static final int MAX_SCALAR_CHARS = 96;

    private MessageFingerprint() {
    }

    public static Fingerprint fingerprint(Object message, Object... contextArgs) {
        Object target = message != null ? message : firstNonNull(contextArgs);
        Walker walker = new Walker();
        walker.walk(target, 0);
        String summary = walker.summary.toString();
        if (summary.isEmpty()) {
            summary = target == null ? "null" : target.getClass().getName();
        }
        return new Fingerprint(walker.hasher.value(), summary, walker.payloadType);
    }

    public static final class Fingerprint {
        public final long valueHash;
        public final String summary;
        public final String payloadType;

        private Fingerprint(long valueHash, String summary, String payloadType) {
            this.valueHash = valueHash;
            this.summary = summary;
            this.payloadType = payloadType;
        }
    }

    private static final class Walker {
        private final IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();
        private final Fnv64 hasher = new Fnv64();
        private final StringBuilder summary = new StringBuilder();
        private String payloadType;

        private void walk(Object value, int depth) {
            if (value == null) {
                hasher.addToken("null");
                appendSummary("null");
                return;
            }
            if (depth > MAX_DEPTH) {
                hasher.addToken("max-depth");
                appendSummary("...");
                return;
            }
            if (visited.size() >= MAX_OBJECTS) {
                hasher.addToken("max-objects");
                appendSummary("<max-objects>");
                return;
            }

            Class<?> clazz = value.getClass();
            if (isScalar(clazz)) {
                appendScalar(clazz, value);
                return;
            }

            if (visited.put(value, Boolean.TRUE) != null) {
                hasher.addToken("cycle");
                appendSummary("<cycle>");
                return;
            }

            hasher.addToken("class");
            hasher.addToken(clazz.getName());
            appendSummary(clazz.getSimpleName());

            if (clazz.isArray()) {
                walkArray(value, depth);
            } else if (value instanceof Map<?, ?>) {
                walkMap((Map<?, ?>) value, depth);
            } else if (value instanceof Collection<?>) {
                walkCollection((Collection<?>) value, depth);
            } else {
                walkObjectFields(value, clazz, depth);
            }
        }

        private void walkArray(Object arrayObj, int depth) {
            int length = Array.getLength(arrayObj);
            hasher.addToken("array-len");
            hasher.addLong(length);
            appendSummary("[len=" + length + "]");
            int limit = Math.min(length, MAX_CONTAINER_ELEMENTS);
            for (int i = 0; i < limit; i++) {
                hasher.addToken("idx");
                hasher.addLong(i);
                walk(Array.get(arrayObj, i), depth + 1);
            }
            if (length > limit) {
                hasher.addToken("array-truncated");
            }
        }

        private void walkCollection(Collection<?> collection, int depth) {
            hasher.addToken("collection-size");
            hasher.addLong(collection.size());
            appendSummary("(size=" + collection.size() + ")");

            List<Object> values = new ArrayList<>(collection);
            if (!(collection instanceof List<?>)) {
                Collections.sort(values, new Comparator<Object>() {
                    @Override
                    public int compare(Object left, Object right) {
                        return safeSortKey(left).compareTo(safeSortKey(right));
                    }
                });
            }

            int limit = Math.min(values.size(), MAX_CONTAINER_ELEMENTS);
            for (int i = 0; i < limit; i++) {
                hasher.addToken("elem");
                walk(values.get(i), depth + 1);
            }
            if (values.size() > limit) {
                hasher.addToken("collection-truncated");
            }
        }

        private void walkMap(Map<?, ?> map, int depth) {
            hasher.addToken("map-size");
            hasher.addLong(map.size());
            appendSummary("{size=" + map.size() + "}");

            List<Map.Entry<?, ?>> entries = new ArrayList<>(map.entrySet());
            Collections.sort(entries, new Comparator<Map.Entry<?, ?>>() {
                @Override
                public int compare(Map.Entry<?, ?> left, Map.Entry<?, ?> right) {
                    return safeSortKey(left.getKey()).compareTo(safeSortKey(right.getKey()));
                }
            });

            int limit = Math.min(entries.size(), MAX_CONTAINER_ELEMENTS);
            for (int i = 0; i < limit; i++) {
                Map.Entry<?, ?> entry = entries.get(i);
                hasher.addToken("key");
                walk(entry.getKey(), depth + 1);
                hasher.addToken("value");
                walk(entry.getValue(), depth + 1);
            }
            if (entries.size() > limit) {
                hasher.addToken("map-truncated");
            }
        }

        private void walkObjectFields(Object value, Class<?> clazz, int depth) {
            List<Field> fields = getAllInstanceFields(clazz);
            for (Field field : fields) {
                hasher.addToken("field");
                hasher.addToken(field.getDeclaringClass().getName() + "#" + field.getName());
                try {
                    field.setAccessible(true);
                    Object fieldValue = field.get(value);
                    if ("payload".equals(field.getName()) && fieldValue != null
                            && payloadType == null) {
                        payloadType = fieldValue.getClass().getName();
                    }
                    walk(fieldValue, depth + 1);
                } catch (Throwable ignored) {
                    hasher.addToken("field-access-error");
                    appendSummary("<" + field.getName() + ":err>");
                }
            }
        }

        private void appendScalar(Class<?> clazz, Object value) {
            String typeName = clazz.getName();
            String text = normalizeScalar(value);
            hasher.addToken("scalar-type");
            hasher.addToken(typeName);
            hasher.addToken("scalar-value");
            hasher.addToken(text);
            appendSummary(text);
        }

        private void appendSummary(String token) {
            if (token == null || token.isEmpty()) {
                return;
            }
            if (summary.length() >= MAX_SUMMARY_CHARS) {
                return;
            }
            if (summary.length() > 0) {
                summary.append('|');
            }
            int remaining = MAX_SUMMARY_CHARS - summary.length();
            if (token.length() > remaining) {
                summary.append(token, 0, Math.max(0, remaining));
            } else {
                summary.append(token);
            }
        }
    }

    private static boolean isScalar(Class<?> clazz) {
        return clazz.isPrimitive() || clazz.isEnum() || Number.class.isAssignableFrom(clazz)
                || Boolean.class == clazz || Character.class == clazz
                || CharSequence.class.isAssignableFrom(clazz);
    }

    private static String normalizeScalar(Object value) {
        if (value == null) {
            return "null";
        }
        String text = String.valueOf(value);
        if (text.length() <= MAX_SCALAR_CHARS) {
            return text;
        }
        return text.substring(0, MAX_SCALAR_CHARS);
    }

    private static List<Field> getAllInstanceFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> cur = clazz;
        while (cur != null && cur != Object.class) {
            Field[] declared = cur.getDeclaredFields();
            for (Field field : declared) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)
                        || field.isSynthetic()) {
                    continue;
                }
                fields.add(field);
            }
            cur = cur.getSuperclass();
        }
        Collections.sort(fields, new Comparator<Field>() {
            @Override
            public int compare(Field left, Field right) {
                String lk = left.getDeclaringClass().getName() + "#" + left.getName();
                String rk = right.getDeclaringClass().getName() + "#" + right.getName();
                return lk.compareTo(rk);
            }
        });
        return fields;
    }

    private static String safeSortKey(Object value) {
        if (value == null) {
            return "null";
        }
        if (isScalar(value.getClass())) {
            return value.getClass().getName() + ":" + normalizeScalar(value);
        }
        return value.getClass().getName() + "@"
                + Integer.toHexString(System.identityHashCode(value));
    }

    private static Object firstNonNull(Object[] args) {
        if (args == null) {
            return null;
        }
        for (Object arg : args) {
            if (arg != null) {
                return arg;
            }
        }
        return null;
    }

    private static final class Fnv64 {
        private long h = 0xcbf29ce484222325L;

        private void addToken(String token) {
            if (token == null) {
                token = "<null>";
            }
            for (int i = 0; i < token.length(); i++) {
                h ^= token.charAt(i);
                h *= 0x100000001b3L;
            }
            h ^= '|';
            h *= 0x100000001b3L;
        }

        private void addLong(long value) {
            addToken(Long.toString(value));
        }

        private long value() {
            return h;
        }
    }
}
