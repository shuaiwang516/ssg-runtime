package org.zlab.ocov.tracker;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Boundary implements Serializable {
    private static final long serialVersionUID = 20231215L;

    int seqId = 0;
    // Boolean means the branch status
    Map<Integer, Set<Boolean>> boundaryInvariant = new HashMap<>();
    Map<Integer, Integer> boundaryDistance = new HashMap<>();

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
        boolean status;
        // update boundary distance
        if (lhsOp instanceof Integer && rhsOp instanceof Integer) {
            int lhs = (Integer) lhsOp;
            int rhs = (Integer) rhsOp;
            switch (operator) {
                case "<" :
                    status = lhs < rhs;
                    break;
                case "<=" :
                    status = lhs <= rhs;
                    break;
                case ">" :
                    status = lhs > rhs;
                    break;
                case ">=" :
                    status = lhs >= rhs;
                    break;
                default :
                    throw new RuntimeException("Unsupported operator: " + operator);
            }
            // compute |lhs - rhs| and add it to boundaryDistance if it's smaller
            int distance = Math.abs(lhs - rhs);
            if (boundaryDistance.containsKey(dumpId)) {
                if (distance < boundaryDistance.get(dumpId)) {
                    boundaryDistance.put(dumpId, distance);
                }
            } else {
                boundaryDistance.put(dumpId, distance);
            }
            Set<Boolean> values = boundaryInvariant.computeIfAbsent(dumpId, k -> new HashSet<>());
            values.add(status);
            return status;
        } else {
            throw new RuntimeException("Unsupported type for boundary: "
                    + lhsOp.getClass().getName() + " and " + rhsOp.getClass().getName());
        }
    }

    public void clear() {
        boundaryInvariant.clear();
    }

    public boolean merge(Boundary other) {
        boolean changed = false;
        // merge boundaryDistance
        for (Map.Entry<Integer, Integer> entry : other.boundaryDistance.entrySet()) {
            int id = entry.getKey();
            int distance = entry.getValue();
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
