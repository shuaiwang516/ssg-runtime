package org.zlab.ocov.tracker;

import org.zlab.ocov.Utils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InvariantCombination implements Serializable {
    private static final long serialVersionUID = 20231215L;

    // FIXME: inv-combine also contains <Equality>, this affects the computation of
    // non-matchable format
    private static final boolean enableVDCheck = false;
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
            Set<Integer> specialDumpIds, Utils.DeltaInfo deltaInfo) {
        boolean changed = false;
        boolean isNonMatchable = false;
        for (Map.Entry<Integer, Set<Set<String>>> entry : other.dumpId2BrokenInv.entrySet()) {
            int dumpId = entry.getKey();

            if (!dumpId2BrokenInv.containsKey(dumpId))
                dumpId2BrokenInv.put(dumpId, new HashSet<>());

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
                        changed = true;
                        // Check NonMatchable
                        if (enableVDCheck && !isNonMatchable
                                && checkNonMatchable(brokenInvSet, deltaInfo))
                            isNonMatchable = true;
                    }
                }
                // Add it anyway
                dumpId2BrokenInv.get(dumpId).add(brokenInvSet);
            }
        }
        if (changed) {
            formatCoverageStatus.setNewFormat("InvariantCombination");
            formatCoverageStatus.setMultiInvBroken("Multi-Inv Broken");
        }
        if (isNonMatchable)
            formatCoverageStatus.setNonMatchableNewFormat("");
    }

    public void clear() {
        dumpId2BrokenInv.clear();
    }

    public static boolean checkNonMatchable(Set<String> brokenInvs, Utils.DeltaInfo deltaInfo) {
        if (deltaInfo == null)
            return false;
        for (String inv : brokenInvs) {
            String iti = extractRefPath(inv);
            if (iti == null)
                continue;
            if (Utils.isNonMatchableFormat(deltaInfo.matchableClassInfo, deltaInfo.changedClasses,
                    iti))
                return true;
        }
        return false;
    }

    public static String extractRefPath(String inv) {
        // brokenInvs.add("<" + invariant.typeName + ">, iti = " + itinerary);
        // get everything after iti = ...
        int idx = inv.indexOf("iti = ");
        if (idx == -1)
            return null;
        return inv.substring(idx + 6);
    }
}
