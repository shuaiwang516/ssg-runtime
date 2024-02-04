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

        for (Map.Entry<String, Map<Integer, Map<String, Integer>>> entry : equalitySet.equalSetAcrossObj
                .entrySet()) {
            for (Map.Entry<Integer, Map<String, Integer>> objEntry : entry.getValue().entrySet()) {
                Set<Integer> relatedObjects = new HashSet<>(objEntry.getValue().values());
                if (relatedObjects.size() > 1) {
                    Set<String> brokenInvSet = new HashSet<>();
                    for (Integer objId : relatedObjects) {
                        if (obj2BrokenInvSet.containsKey(objId))
                            brokenInvSet.addAll(obj2BrokenInvSet.get(objId));
                    }
                    MultiObjectBrokenInvSet.add(brokenInvSet);
                }
            }
        }

        for (Map.Entry<String, Map<Integer, Map<String, Set<Integer>>>> entry : equalitySet.equalSetSameItineraryAcrossObj
                .entrySet()) {
            for (Map.Entry<Integer, Map<String, Set<Integer>>> objEntry : entry.getValue()
                    .entrySet()) {
                for (Map.Entry<String, Set<Integer>> invEntry : objEntry.getValue().entrySet()) {
                    // No need for deep copy
                    Set<Integer> relatedObjects = new HashSet<>(invEntry.getValue());
                    if (relatedObjects.size() > 1) {
                        Set<String> brokenInvSet = new HashSet<>();
                        for (Integer objId : relatedObjects) {
                            if (obj2BrokenInvSet.containsKey(objId))
                                brokenInvSet.addAll(obj2BrokenInvSet.get(objId));
                        }
                        MultiObjectBrokenInvSet.add(brokenInvSet);
                    }
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
