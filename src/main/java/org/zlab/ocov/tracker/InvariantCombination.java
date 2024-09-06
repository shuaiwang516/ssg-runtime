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
                Runtime.log("<Invariant Combination>: new combinations, dumpId=" + dumpId
                        + ", new combination = " + entry.getValue());
                changed = true;
            }
        }
        return changed;
    }

    // With frequency filtering
    public void merge(InvariantCombination other,
            Map<Integer, Set<String>> invariantBrokenLessFrequently,
            FormatCoverageStatus formatCoverageStatus, boolean checkSpecialDumpIds,
            Set<Integer> specialDumpIds) {
        boolean changed = false;
        for (Map.Entry<Integer, Set<Set<String>>> entry : other.dumpId2BrokenInv.entrySet()) {
            int dumpId = entry.getKey();

            boolean curChanged = false;
            if (!dumpId2BrokenInv.containsKey(dumpId)) {
                dumpId2BrokenInv.put(dumpId, new HashSet<>());
            }

            for (Set<String> brokenInvSet : entry.getValue()) {
                if (dumpId2BrokenInv.get(dumpId).contains(brokenInvSet))
                    continue;
                if (invariantBrokenLessFrequently.containsKey(dumpId)) {
                    Set<String> intersection = new HashSet<>(brokenInvSet);
                    intersection.retainAll(invariantBrokenLessFrequently.get(dumpId));
                    if (!intersection.isEmpty()) {
                        Runtime.log(
                                "<Invariant Combination with Frequency>: new combinations, dumpId="
                                        + dumpId + ", new combination = " + entry.getValue());
                        curChanged = true;
                    }
                }

                // add it anyway
                dumpId2BrokenInv.get(dumpId).add(brokenInvSet);
            }

            if (curChanged) {
                changed = true;
                if (checkSpecialDumpIds && specialDumpIds != null
                        && specialDumpIds.contains(dumpId)) {
                    formatCoverageStatus.setNewFormatAtModifiedMergePoint("dumpId=" + dumpId);
                }
            }
        }
        if (changed)
            formatCoverageStatus.setNewFormat("invariantCombination");
    }

    public void clear() {
        dumpId2BrokenInv.clear();
    }
}
