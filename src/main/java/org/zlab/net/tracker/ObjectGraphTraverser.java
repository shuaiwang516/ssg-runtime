package org.zlab.net.tracker;

import org.zlab.ocov.Utils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;

public class ObjectGraphTraverser {
    private final Set<String> visitedTypes = new HashSet<>();
    // Avoid processing the same object multiple times
    private final Set<Integer> visitedObjects = new HashSet<>();

    private static final int maxArrayLength = 20;
    private static final int arraySampleSize = 20;

    public String payloadType = null;

    public Set<String> getVisitedTypes() {
        return visitedTypes;
    }

    public void traverse(Object obj) {
        if (obj == null || isSkippedType(obj.getClass().getName())) {
            return;
        }
        int objAddr = System.identityHashCode(obj);
        if (visitedObjects.contains(objAddr))
            return;
        visitedObjects.add(objAddr);
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
                traverse(arrayElement);
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
                traverse(entry.getKey());
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
                traverse(collectionElement);
            }
        } else {
            // Iterate all fields of the object
            try {
                Class<?> currentClass = obj.getClass();
                while (currentClass != Object.class) { // Traverse up the class hierarchy
                    Field[] fields = currentClass.getDeclaredFields();
                    for (Field field : fields) {
                        field.setAccessible(true);
                        Object fieldValue = field.get(obj);
                        if (field.getName().equals("payload") && fieldValue != null) {
                            payloadType = fieldValue.getClass().getName();
                        }
                        if (fieldValue != null) {
                            traverse(fieldValue);
                        }
                    }
                    currentClass = currentClass.getSuperclass(); // Move to the superclass
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private boolean isSkippedType(String className) {
        return Utils.isPrimitiveType(className) || Utils.isStringType(className);
    }
}
