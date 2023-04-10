package org.zlab.dinv.runtimechecker;

import java.util.Collection;

public class Quant {

    /**
     * Returns the size of the array or collection. If the argument is null or not an array or
     * collection, returns a default value (Integer.MAX_VALUE). Thus, for an array a, this never
     * throws an exception, though a.length may.
     */
    // Not called from Quant; provided only for external use.
    public static int size(Object o) {
        if (o == null) {
            return Integer.MAX_VALUE; // return default value
        }
        java.lang.Class<?> c = o.getClass();
        if (c.isArray()) {
            return java.lang.reflect.Array.getLength(o);
        } else if (o instanceof Collection<?>) {
            return ((Collection<?>)o).size();
        } else {
            return Integer.MAX_VALUE; // return default value
        }
    }
}
