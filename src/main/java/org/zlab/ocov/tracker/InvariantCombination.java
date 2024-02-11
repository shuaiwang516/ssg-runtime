package org.zlab.ocov.tracker;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InvariantCombination implements Serializable {
    private static final long serialVersionUID = 20231215L;

    public Map<Integer, Set<String>> obj2BrokenInvSet = new HashMap<>();

    public Set<Set<String>> singleObjectBrokenInvSet = new HashSet<>();
    public Set<Set<String>> MultiObjectBrokenInvSet = new HashSet<>();

    public InvariantCombination() {
        // for json
    }

    public void record(int objId, Set<String> brokenInvSet) {
        obj2BrokenInvSet.put(objId, brokenInvSet);
    }

    public void infer(EqualitySet eqSet) {
        inferSingle();
        inferMulti(eqSet);
    }

    public void inferSingle() {
        for (Set<String> brokenInvSet : obj2BrokenInvSet.values()) {
            // deep copy add
            singleObjectBrokenInvSet.add(new HashSet<>(brokenInvSet));
        }
    }

    public void inferMulti(EqualitySet equalitySet) {
        Map<Integer, Map<Integer, Set<String>>> equalAcrossObj = EqualitySet
                .extractEqualityEdges(equalitySet.equalSetAcrossObj);

        for (Map.Entry<Integer, Map<Integer, Set<String>>> entry : equalAcrossObj.entrySet()) {
            Integer objId1 = entry.getKey();
            for (Map.Entry<Integer, Set<String>> objEntry : entry.getValue().entrySet()) {
                Integer objId2 = objEntry.getKey();
                Set<String> equalInvs = objEntry.getValue();
                Set<String> brokenInvSet1 = obj2BrokenInvSet.get(objId1);
                Set<String> brokenInvSet2 = obj2BrokenInvSet.get(objId2);
                Set<String> brokenInvSet = new HashSet<>();
                if (brokenInvSet1 != null) {
                    brokenInvSet.addAll(brokenInvSet1);
                }
                if (brokenInvSet2 != null) {
                    brokenInvSet.addAll(brokenInvSet2);
                }
                brokenInvSet.addAll(equalInvs);
                if (!brokenInvSet.isEmpty()) {
                    MultiObjectBrokenInvSet.add(brokenInvSet);
                }
            }
        }
    }

    public boolean merge(InvariantCombination other) {
        boolean changed = false;
        // TODO: more detailed runtime log
        if (singleObjectBrokenInvSet.addAll(other.singleObjectBrokenInvSet)) {
            Runtime.log("<Invariant Combination>: new single object broken invariants added");
            changed = true;
        }
        if (MultiObjectBrokenInvSet.addAll(other.MultiObjectBrokenInvSet)) {
            Runtime.log("<Invariant Combination>: new multi object broken invariants added");
            changed = true;
        }
        return changed;
    }

    public void clear() {
        obj2BrokenInvSet.clear();
        singleObjectBrokenInvSet.clear();
        MultiObjectBrokenInvSet.clear();
    }

}
