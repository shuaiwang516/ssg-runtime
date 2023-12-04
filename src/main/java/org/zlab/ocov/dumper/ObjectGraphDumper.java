package org.zlab.ocov.dumper;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

public class ObjectGraphDumper {
    private Set<Object> visited = new HashSet<>();

    public void dump(Object obj) throws IllegalAccessException {
        dump(obj, 0);
    }

    private void dump(Object obj, int depth) throws IllegalAccessException {
        if (obj == null || visited.contains(obj)) {
            return;
        }
        visited.add(obj);

        Field[] fields = obj.getClass().getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            Object value = field.get(obj);

            printIndent(depth);
            System.out.println(field.getName() + ": " + value);

            if (!isPrimitiveOrWrapper(value)) {
                dump(value, depth + 1);
            }
        }
    }

    private boolean isPrimitiveOrWrapper(Object obj) {
        if (obj instanceof Integer || obj instanceof Float || obj instanceof Double
                || obj instanceof Long || obj instanceof Boolean || obj instanceof Character
                || obj instanceof Byte || obj instanceof Short || obj instanceof String) {
            return true;
        }
        return false;
    }

    private void printIndent(int depth) {
        for (int i = 0; i < depth; i++) {
            System.out.print("  ");
        }
    }

}
