package org.zlab.ocov.tracker;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.zlab.ocov.Utils.computeBinaryComparison;
import static org.zlab.ocov.Utils.toLong;

public class Boundary implements Serializable {
    private static final long serialVersionUID = 20231215L;

    // Boolean means the branch status
    Map<Integer, Set<Boolean>> boundaryInvariant = new HashMap<>();
    Map<Integer, Long> boundaryDistance = new HashMap<>();

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

    public boolean updateBranch(Object lhsOp, Object rhsOp, String operator, int dumpId) {
        long lhs = toLong(lhsOp);
        long rhs = toLong(rhsOp);

        // compute |lhs - rhs| and add it to boundaryDistance if it's smaller
        long distance = Math.abs(lhs - rhs);
        if (boundaryDistance.containsKey(dumpId)) {
            if (distance < boundaryDistance.get(dumpId)) {
                if (Runtime.debug)
                    Runtime.log(
                            "[boundary] update boundary distance: " + distance + " id: " + dumpId);
                boundaryDistance.put(dumpId, distance);
            }
        } else {
            if (Runtime.debug)
                Runtime.log("[boundary] new boundary distance: " + distance + " id: " + dumpId);
            boundaryDistance.put(dumpId, distance);
        }

        boolean status = computeBinaryComparison(lhs, rhs, operator);
        Set<Boolean> values = boundaryInvariant.computeIfAbsent(dumpId, k -> new HashSet<>());
        values.add(status);
        return status;
    }

    public void clear() {
        boundaryInvariant.clear();
    }

    public boolean merge(Boundary other) {
        boolean changed = false;
        // merge boundaryDistance
        for (Map.Entry<Integer, Long> entry : other.boundaryDistance.entrySet()) {
            int id = entry.getKey();
            long distance = entry.getValue();
            if (boundaryDistance.containsKey(id)) {
                if (distance < boundaryDistance.get(id)) {
                    Runtime.log("<Boundary Distance Change> before: " + boundaryDistance.get(id)
                            + " after: " + distance + " id: " + id);
                    boundaryDistance.put(id, distance);
                    changed = true;
                }
            } else {
                Runtime.log("<Boundary Distance Change> before: null after: " + distance + " id: "
                        + id);
                boundaryDistance.put(id, distance);
                changed = true;
            }
        }
        for (Map.Entry<Integer, Set<Boolean>> entry : other.boundaryInvariant.entrySet()) {
            int id = entry.getKey();
            Set<Boolean> values = boundaryInvariant.computeIfAbsent(id, k -> new HashSet<>());
            if (values.addAll(entry.getValue())) {
                Runtime.log("<Boundary Status Change> status = " + values + " id: " + id);
                changed = true;
            }
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
