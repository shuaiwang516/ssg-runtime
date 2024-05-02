package org.zlab.ocov.tracker;

import org.zlab.ocov.Utils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;

public class ObjectGraphTraverser {
    private final Map<String, Map<String, String>> classInfoOri;
    private final Set<Integer> visited = new HashSet<>();

    private static final int maxArrayLength = 20;
    private static final int arraySampleSize = 20;

    public ObjectGraphTraverser(Map<String, Map<String, String>> classInfoOri) {
        this.classInfoOri = classInfoOri;
    }

    public Set<Integer> getVisited() {
        return visited;
    }

    public void traverse(Object obj) {
        if (obj == null || visited.contains(System.identityHashCode(obj))
                || isSkippedType(obj.getClass().getName())) {
            return;
        }
        visited.add(System.identityHashCode(obj));

        Class<?> clazz = obj.getClass();

        Runtime.log("Traversing object: " + clazz.getName() + " " + System.identityHashCode(obj));
        // Check for and handle Arrays
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
                if (arrayElement == null || isSkippedType(arrayElement.getClass().getName())) {
                    continue;
                }
                traverse(arrayElement);
            }
        } else if (obj instanceof Map<?, ?>) {
            int length = ((java.util.Map<?, ?>) obj).entrySet().size();;
            List<Integer> sampleIdxs;
            if (length > maxArrayLength) {
                sampleIdxs = Utils.sampleIdxFromSize(length, arraySampleSize);
            } else {
                sampleIdxs = new ArrayList<>();
                for (int i = 0; i < length; i++)
                    sampleIdxs.add(i);
            }
            Runtime.log("Traversing map: " + clazz.getName() + ", sampleIdxs: " + sampleIdxs);
            for (int i : sampleIdxs) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) ((java.util.Map) obj).entrySet()
                        .toArray()[i];
                if (entry == null) {
                    continue;
                }
                if (entry.getKey() != null && !isSkippedType(entry.getKey().getClass().getName()))
                    traverse(entry.getKey());
                if (entry.getValue() != null
                        && !isSkippedType(entry.getValue().getClass().getName()))
                    traverse(entry.getValue());
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
                if (collectionElement == null
                        || isSkippedType(collectionElement.getClass().getName())) {
                    continue;
                }
                traverse(collectionElement);
            }
        } else {
            // Handle all other Object types via their fields
            if (!classInfoOri.containsKey(clazz.getName())) {
                Runtime.log("Class not found in classInfoOri: " + clazz.getName());
                return;
            }
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                if (!classInfoOri.get(clazz.getName()).containsKey(field.getName()))
                    continue;
                field.setAccessible(true);
                try {
                    Object fieldValue = field.get(obj);
                    if (fieldValue != null && !isSkippedType(field.getType().getName())) {
                        Runtime.log("Traversing: " + clazz.getName() + "." + field.getName());
                        traverse(fieldValue);
                    }
                } catch (IllegalAccessException e) {
                    System.err.println("Error accessing field: " + field.getName());
                }
            }
        }
    }

    private boolean isSkippedType(String className) {
        return Utils.isPrimitiveType(className) || Utils.isStringType(className);
    }
}
