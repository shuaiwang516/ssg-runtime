package org.zlab.ocov.tracker.inv;

import org.zlab.ocov.tracker.InvariantCombination;

import java.io.Serializable;
import java.util.*;

public class InvariantBrokenFrequency implements Serializable {
    private static final long serialVersionUID = 20231215L;

    public transient int totalTestNum = 0;
    public transient Map<Integer, Map<String, Integer>> dumpId2InvBrokenCount = new HashMap<>();

    static class Entry {
        int dumpId;
        String inv;
        int brokenCount;

        Entry(int dumpId, String inv, int brokenCount) {
            this.dumpId = dumpId;
            this.inv = inv;
            this.brokenCount = brokenCount;
        }

        @Override
        public String toString() {
            return "Entry{" + "dumpId=" + dumpId + ", inv='" + inv + '\'' + ", brokenCount="
                    + brokenCount + '}';
        }
    }

    public void update(InvariantCombination invCombination) {
        if (invCombination == null) {
            return;
        }
        Map<Integer, Set<Set<String>>> dumpId2BrokenInv = invCombination.dumpId2BrokenInv;
        Map<Integer, Set<String>> brokenInvs = new HashMap<>();
        for (Map.Entry<Integer, Set<Set<String>>> entry : dumpId2BrokenInv.entrySet()) {
            int dumpId = entry.getKey();
            Set<String> brokenInvSet = new HashSet<>();
            for (Set<String> invSet : entry.getValue()) {
                brokenInvSet.addAll(invSet);
            }
            brokenInvs.put(dumpId, brokenInvSet);
        }
        update(brokenInvs);
    }

    public void update(Map<Integer, Set<String>> brokenInvs) {
        totalTestNum++;
        for (Map.Entry<Integer, Set<String>> entry : brokenInvs.entrySet()) {
            int dumpId = entry.getKey();
            Set<String> brokenInvSet = entry.getValue();
            dumpId2InvBrokenCount.computeIfAbsent(dumpId, k -> new HashMap<>());
            for (String inv : brokenInvSet) {
                dumpId2InvBrokenCount.get(dumpId).putIfAbsent(inv, 0);
                dumpId2InvBrokenCount.get(dumpId).put(inv,
                        dumpId2InvBrokenCount.get(dumpId).get(inv) + 1);
            }
        }
    }

    public Map<Integer, Set<String>> getMostInfrequentInvariants(int topK) {
        PriorityQueue<Entry> frequencyQueue = new PriorityQueue<>(
                (a, b) -> a.brokenCount - b.brokenCount);

        for (Map.Entry<Integer, Map<String, Integer>> entry : dumpId2InvBrokenCount.entrySet()) {
            int dumpId = entry.getKey();
            Map<String, Integer> invBrokenCount = entry.getValue();
            for (Map.Entry<String, Integer> invEntry : invBrokenCount.entrySet()) {
                frequencyQueue.add(new Entry(dumpId, invEntry.getKey(), invEntry.getValue()));
            }
        }

        Map<Integer, Set<String>> result = new HashMap<>();
        for (int i = 0; i < topK; i++) {
            Entry entry = frequencyQueue.poll();
            if (entry == null) {
                break;
            }
            result.computeIfAbsent(entry.dumpId, k -> new HashSet<>()).add(entry.inv);
        }
        return result;
    }
}
