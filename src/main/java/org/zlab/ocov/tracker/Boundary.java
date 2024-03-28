package org.zlab.ocov.tracker;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Boundary implements Serializable {
    private static final long serialVersionUID = 20231215L;

    Map<Integer, Set<Integer>> branch2Collection;

    int seqId = 0;
    // Boolean means the branch status
    Map<Integer, Set<Boolean>> boundaryInvariant = new HashMap<>();

    public Boundary() {
    }

    // ----Boundary Related----
    public boolean updateBranch(Object obj, int id) {
        if (obj == null)
            return false;
        if (obj instanceof Boolean) {
            boolean branchStatus = (Boolean) obj;
            // use computeIfAbsent
            Set<Boolean> values = boundaryInvariant.computeIfAbsent(id, k -> new HashSet<>());
            return values.add(branchStatus);
        }
        return false;
    }

    public void clear() {
        boundaryInvariant.clear();
    }

    public boolean merge(Boundary other) {
        boolean changed = false;
        for (Map.Entry<Integer, Set<Boolean>> entry : other.boundaryInvariant.entrySet()) {
            int id = entry.getKey();
            Set<Boolean> values = boundaryInvariant.computeIfAbsent(id, k -> new HashSet<>());
            if (values.addAll(entry.getValue())) {
                changed = true;
            }
        }
        if (changed) {
            Runtime.log("<Boundary Status Change>");
        } else {
            Runtime.log("<Boundary Status Unchanged>");
        }
        return changed;
    }

    public static class RecordCollection implements Serializable {
        public int identityHashCode;
        public int seqId;

        public RecordCollection(int identityHashCode, int seqId) {
            this.identityHashCode = identityHashCode;
            this.seqId = seqId;
        }
    }

}
