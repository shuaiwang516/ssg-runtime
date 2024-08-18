package org.zlab.ocov.tracker;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InvariantCombination implements Serializable {
    private static final long serialVersionUID = 20231215L;

    public Map<Integer, Set<Set<String>>> dumpId2BrokenInv = new HashMap<>();

    public InvariantCombination() {
        // for json
    }

    public void record(int dumpId, Set<String> brokenInvSet) {
        if (!dumpId2BrokenInv.containsKey(dumpId)) {
            dumpId2BrokenInv.put(dumpId, new HashSet<>());
        }
        dumpId2BrokenInv.get(dumpId).add(brokenInvSet);
    }

    public boolean merge(InvariantCombination other) {
        boolean changed = false;
        for (Map.Entry<Integer, Set<Set<String>>> entry : other.dumpId2BrokenInv.entrySet()) {
            int dumpId = entry.getKey();
            if (!dumpId2BrokenInv.containsKey(dumpId)) {
                dumpId2BrokenInv.put(dumpId, new HashSet<>());
            }
            if (dumpId2BrokenInv.get(dumpId).addAll(entry.getValue())) {
                Runtime.log("<Invariant Combination>: new broken invariants added");
                changed = true;
            }
        }
        return changed;
    }

    public void clear() {
        dumpId2BrokenInv.clear();
    }
}
