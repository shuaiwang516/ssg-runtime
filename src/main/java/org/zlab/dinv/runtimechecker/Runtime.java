package org.zlab.dinv.runtimechecker;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Runtime {
    // maintain the violated invariants

    // a large array which represents the id

    // when instrumenting the inv, we also need to add a single identifier to it.

    private static ConcurrentMap<Integer, Integer> violations = new ConcurrentHashMap<>();

    public static void addViolation(int invId) {
        int oriCount = 0;
        if (violations.containsKey(invId)) {
            oriCount = violations.get(invId);
        }
        violations.put(invId, ++oriCount);
    }

}
