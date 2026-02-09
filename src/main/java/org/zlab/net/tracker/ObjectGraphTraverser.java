package org.zlab.net.tracker;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class ObjectGraphTraverser {
    private final Set<String> visitedTypes = new HashSet<>();
    private final Set<Object> visitedObjects = Collections
            .newSetFromMap(new IdentityHashMap<Object, Boolean>());

    private static final int maxArrayLength = 32;
    private static final int arraySampleSize = 24;
    private static final int maxDepth = 12;
    private static final int maxVisitedObjects = 4096;

    public String payloadType = null;

    public Set<String> getVisitedTypes() {
        return visitedTypes;
    }

    public void traverse(Object obj) {
        traverse(obj, 0);
    }

    private void traverse(Object obj, int depth) {
        if (obj == null || depth > maxDepth || visitedObjects.size() > maxVisitedObjects
                || isSkippedType(obj.getClass().getName())) {
            return;
        }
        if (!visitedObjects.add(obj)) {
            return;
        }
        visitedTypes.add(obj.getClass().getName());

        Class<?> clazz = obj.getClass();

        if (clazz.isArray()) {
            int length = Array.getLength(obj);
            List<Integer> sampleIdxs;
            if (length > maxArrayLength) {
                sampleIdxs = Utils.sampleIdxFromSize(length, arraySampleSize);
            } else {
                sampleIdxs = new ArrayList<>();
                for (int i = 0; i < length; i++)
                    sampleIdxs.add(i);
            }
            for (int i : sampleIdxs) {
                Object arrayElement = Array.get(obj, i);
                traverse(arrayElement, depth + 1);
            }
        } else if (obj instanceof Map<?, ?>) {
            int length = ((Map<?, ?>) obj).entrySet().size();;
            List<Integer> sampleIdxs;
            if (length > maxArrayLength) {
                sampleIdxs = Utils.sampleIdxFromSize(length, arraySampleSize);
            } else {
                sampleIdxs = new ArrayList<>();
                for (int i = 0; i < length; i++)
                    sampleIdxs.add(i);
            }
            for (int i : sampleIdxs) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) ((Map) obj).entrySet().toArray()[i];
                if (entry == null) {
                    continue;
                }
                traverse(entry.getKey(), depth + 1);
                traverse(entry.getValue(), depth + 1);
            }
        } else if (obj instanceof Collection) {
            int length = ((Collection<?>) obj).size();
            List<Integer> sampleIdxs;
            if (length > maxArrayLength) {
                sampleIdxs = Utils.sampleIdxFromSize(length, arraySampleSize);
            } else {
                sampleIdxs = new ArrayList<>();
                for (int i = 0; i < length; i++)
                    sampleIdxs.add(i);
            }
            for (int i : sampleIdxs) {
                Object collectionElement = ((Collection<?>) obj).toArray()[i];
                traverse(collectionElement, depth + 1);
            }
        } else {
            try {
                Class<?> currentClass = obj.getClass();
                while (currentClass != Object.class) {
                    Field[] fields = currentClass.getDeclaredFields();
                    for (Field field : fields) {
                        int modifiers = field.getModifiers();
                        if (field.isSynthetic() || Modifier.isStatic(modifiers)
                                || Modifier.isTransient(modifiers)) {
                            continue;
                        }
                        field.setAccessible(true);
                        Object fieldValue = field.get(obj);
                        if (field.getName().equals("payload") && fieldValue != null) {
                            payloadType = fieldValue.getClass().getName();
                        }
                        if (fieldValue != null) {
                            traverse(fieldValue, depth + 1);
                        }
                    }
                    currentClass = currentClass.getSuperclass();
                }
            } catch (IllegalAccessException | RuntimeException ignored) {
            }
        }
    }

    private boolean isSkippedType(String className) {
        return Utils.isPrimitiveType(className) || Utils.isStringType(className);
    }
}
