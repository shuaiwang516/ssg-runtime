package org.zlab.ocov.tracker;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BoundaryWithCollection implements Serializable {
    private static final long serialVersionUID = 20231215L;

    Map<Integer, Set<Integer>> branch2Collection;

    int seqId = 0;
    Map<Integer, RecordCollection> id2Collection = new HashMap<>();
    // Boolean means the branch status
    Map<Integer, Map<Boolean, Set<Integer>>> boundaryInvariant = new HashMap<>();

    public BoundaryWithCollection(Map<Integer, Set<Integer>> branch2Collection) {
        this.branch2Collection = branch2Collection;
    }

    // ----Boundary Related----
    public boolean updateBranchWithCollection(Object obj, int id) {
        if (obj == null)
            return false;
        if (obj instanceof Boolean) {
            boolean branchStatus = (Boolean) obj;
            // use computeIfAbsent
            Map<Boolean, Set<Integer>> branch2CollectionMap = boundaryInvariant.computeIfAbsent(id,
                    k -> new HashMap<>());
            Set<Integer> collectionSet = branch2CollectionMap.computeIfAbsent(branchStatus,
                    k -> new HashSet<>());
            return collectionSet.addAll(branch2Collection.get(id));
        }
        return false;
    }

    public boolean updateCollection(Object obj, int id) {
        if (obj == null)
            return false;
        if (!id2Collection.containsKey(id)) {
            id2Collection.put(id, new RecordCollection(System.identityHashCode(obj), seqId++));
            return true;
        } else {
            RecordCollection recordCollection = id2Collection.get(id);
            recordCollection.identityHashCode = System.identityHashCode(obj);
            recordCollection.seqId = seqId++;
            return true;
        }
    }

    public void clear() {
        boundaryInvariant.clear();
        id2Collection.clear();
    }

    public boolean merge(BoundaryWithCollection other) {
        boolean changed = false;
        for (Map.Entry<Integer, Map<Boolean, Set<Integer>>> entry : other.boundaryInvariant
                .entrySet()) {
            int id = entry.getKey();
            Map<Boolean, Set<Integer>> branch2CollectionMap = boundaryInvariant.computeIfAbsent(id,
                    k -> new HashMap<>());
            for (Map.Entry<Boolean, Set<Integer>> entry2 : entry.getValue().entrySet()) {
                boolean branchStatus = entry2.getKey();
                Set<Integer> collectionSet = branch2CollectionMap.computeIfAbsent(branchStatus,
                        k -> new HashSet<>());
                changed |= collectionSet.addAll(entry2.getValue());
            }
        }
        if (changed) {
            Runtime.log("<BoundaryWithCollection Status Change>");
        } else {
            Runtime.log("<BoundaryWithCollection Status Unchanged>");
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
