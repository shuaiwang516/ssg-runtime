package org.zlab.ocov.tracker;

import java.io.Serializable;
import java.util.*;

public class EqualitySet implements Serializable {
    private static final long serialVersionUID = 20231215L;

    public Set<String> comparableClasses;

    public Map<String, Map<Integer, Set<String>>> equalSetDiffObj = new HashMap<>();
    public Map<String, Set<Set<String>>> equalSetDiffObjDedup = new HashMap<>();

    public Map<String, Map<Integer, Set<String>>> equalSetSameObjDedup = new HashMap<>();
    public Map<String, Set<SetMapping>> equalSetSameObj = new HashMap<>();

    static final String logPrefixAcrossObject = "Equality: across object graphs";
    static final String logPrefixSameObject = "Equality: same object graph";

    public EqualitySet() {
        // for json
    }

    public EqualitySet(Set<String> comparableClasses) {
        // For test purpose
        // comparableClasses.add("org.apache.cassandra.cql3.ColumnIdentifier");
        this.comparableClasses = comparableClasses;
    }

    public void update(Object obj, String className, String itinerary) {
        if (comparableClasses.contains(className)) {
            int hashCode = obj.hashCode();

            // Across different object graphs
            Map<Integer, Set<String>> hashCodeMap = equalSetDiffObj.computeIfAbsent(className,
                    k -> new HashMap<>());
            Set<String> itinerarySet = hashCodeMap.computeIfAbsent(hashCode, k -> new HashSet<>());
            itinerarySet.add(itinerary);

            // Within the same object graph
            Map<Integer, Set<String>> hashCodeMap1 = equalSetSameObjDedup.computeIfAbsent(className,
                    k -> new HashMap<>());
            Set<String> itinerarySet1 = hashCodeMap1.computeIfAbsent(hashCode,
                    k -> new HashSet<>());
            itinerarySet1.add(itinerary);
        }
    }

    public Map<String, Set<Set<String>>> dedupSameObjectGraph() {
        return dedup(equalSetSameObjDedup);
    }

    // Deduplication and containment filtering
    public Map<String, Set<Set<String>>> dedupAcrossObjectGraph() {
        return dedup(equalSetDiffObj);
    }

    public void cleanSameObjectGraph() {
        equalSetSameObjDedup.clear();
    }

    public void mergeSameObjectGraph(int dumpId) {
        // equalSetSameObjDedup =>
        // compClass2EqualitySetSameObjectGraphDedup

        Map<String, Set<Set<String>>> otherCompClass2EqualitySetSameObjectGraphDedup = dedupSameObjectGraph();
        // Merge into compClass2EqualitySetSameObjectGraphDedup
        mergeCompClass2EqualityDedup(equalSetSameObj,
                otherCompClass2EqualitySetSameObjectGraphDedup, false, logPrefixSameObject, dumpId);
    }

    public boolean merge(EqualitySet other) {
        if (other == null) {
            return false;
        }
        boolean changed = false;
        Map<String, Set<Set<String>>> otherEqualSetDiffObjDedup = other.dedupAcrossObjectGraph();
        if (mergeCompClass2EqualityDedup(equalSetDiffObjDedup, otherEqualSetDiffObjDedup, true,
                logPrefixAcrossObject)) {
            changed = true;
        }

        if (mergeCompClass2EqualityDedupWithDumpId(equalSetSameObj, other.equalSetSameObj, true,
                logPrefixSameObject)) {
            changed = true;
        }
        return changed;
    }

    public static boolean mergeCompClass2EqualityDedup(Map<String, Set<Set<String>>> equalSetDedup1,
            Map<String, Set<Set<String>>> equalSetDedup2, boolean useLog, String logPrefix) {
        boolean changed = false;
        for (String compClass : equalSetDedup2.keySet()) {
            if (equalSetDedup1.containsKey(compClass)) {
                Set<Set<String>> equalitySets = equalSetDedup1.get(compClass);
                Set<Set<String>> otherEqualitySets = equalSetDedup2.get(compClass);
                if (mergeSets(equalitySets, otherEqualitySets, compClass, useLog, logPrefix)) {
                    changed = true;
                }
            } else {
                equalSetDedup1.put(compClass, deepCopy(equalSetDedup2.get(compClass)));
                if (useLog) {
                    Runtime.log(String.format(
                            "<Equality: incorporate sets for a new class> class = %s, set = %s",
                            compClass, equalSetDedup2.get(compClass)));
                }
                changed = true;
            }
        }
        return changed;
    }

    public static boolean mergeCompClass2EqualityDedupWithDumpId(
            Map<String, Set<SetMapping>> equalSetDedup1,
            Map<String, Set<SetMapping>> equalSetDedup2, boolean useLog, String logPrefix) {
        boolean changed = false;
        for (String compClass : equalSetDedup2.keySet()) {
            if (equalSetDedup1.containsKey(compClass)) {
                Set<SetMapping> equalitySets = equalSetDedup1.get(compClass);
                Set<SetMapping> otherEqualitySets = equalSetDedup2.get(compClass);
                if (mergeSetsWithDumpId(equalitySets, otherEqualitySets, compClass, useLog,
                        logPrefix)) {
                    changed = true;
                }
            } else {
                // Deep copy
                Set<SetMapping> tmpMap = new HashSet<>();
                for (SetMapping set : equalSetDedup2.get(compClass)) {
                    tmpMap.add(set.clone());
                }
                equalSetDedup1.put(compClass, tmpMap);
                if (useLog) {
                    Runtime.log(String.format(
                            "<Equality: incorporate sets for a new class> class = %s, set = %s",
                            compClass, equalSetDedup2.get(compClass)));
                }
                changed = true;
            }
        }
        return changed;
    }

    public static boolean mergeCompClass2EqualityDedup(Map<String, Set<SetMapping>> equalSetDedup1,
            Map<String, Set<Set<String>>> equalSetDedup2, boolean useLog, String logPrefix,
            int dumpId) {
        Map<String, Set<SetMapping>> equalSetDedup2WithDumpId = new HashMap<>();
        for (String compClass : equalSetDedup2.keySet()) {
            Set<SetMapping> tmpMap = new HashSet<>();
            for (Set<String> set : equalSetDedup2.get(compClass)) {
                tmpMap.add(new SetMapping(set, dumpId));
            }
            equalSetDedup2WithDumpId.put(compClass, tmpMap);
        }
        return mergeCompClass2EqualityDedupWithDumpId(equalSetDedup1, equalSetDedup2WithDumpId,
                useLog, logPrefix);
    }

    public static boolean mergeSets(Set<Set<String>> s1, Set<Set<String>> s2, String className) {
        return mergeSets(s1, s2, className, true, "Equality:");
    }

    public static boolean mergeSets(Set<Set<String>> s1, Set<Set<String>> s2, String className,
            boolean useLog, String logPrefix) {
        boolean isChanged = false;

        // Seems no need to do this
        // s2.removeIf(Set::isEmpty);

        for (Set<String> setFromS2 : s2) {
            // skip empty set
            if (setFromS2.isEmpty()) {
                continue;
            }

            // Here we use >, not >= for strict super set
            boolean isStrictSupersetFound = false;
            boolean isSubsetFound = false;
            Set<Set<String>> setsToRemove = new HashSet<>();

            for (Set<String> setFromS1 : s1) {
                if (setFromS2.containsAll(setFromS1) && !setFromS2.equals(setFromS1)) {
                    setsToRemove.add(setFromS1);
                    // log: a larger equality set!
                    if (useLog) {
                        Runtime.log(String.format(
                                "<%s: a larger equality set> class = %s, oriset = %s, newset = %s",
                                logPrefix, className, setFromS1, setFromS2));
                    }
                    isStrictSupersetFound = true;
                    isChanged = true;
                }
                // See if it's a subset set
                if (setFromS1.containsAll(setFromS2)) {
                    // log: a smaller equality set!
                    isSubsetFound = true;
                }
            }

            // Remove all subsets from s1
            s1.removeAll(setsToRemove);

            if (isStrictSupersetFound) {
                s1.add(setFromS2);
            } else {
                if (!isSubsetFound) {
                    // A distinguished set
                    if (useLog) {
                        Runtime.log(String.format("<%s: a new equality set> class = %s, set = %s",
                                logPrefix, className, setFromS2));
                    }
                    isChanged = true;
                    s1.add(setFromS2);
                }
            }
        }
        return isChanged;
    }

    public static boolean mergeSetsWithDumpId(Set<SetMapping> s1, Set<SetMapping> s2,
            String className, boolean useLog, String logPrefix) {
        boolean isChanged = false;

        // Seems no need to do this
        // s2.removeIf(Set::isEmpty);

        for (SetMapping setFromS2 : s2) {
            // skip empty set
            if (setFromS2.keySet.isEmpty()) {
                continue;
            }

            // Here we use >, not >= for strict super set
            boolean isStrictSupersetFound = false;
            boolean isSubsetFound = false;
            Set<SetMapping> setsToRemove = new HashSet<>();
            int dumpId = setFromS2.value;

            for (SetMapping setFromS1 : s1) {
                if (setFromS2.keySet.containsAll(setFromS1.keySet)
                        && !setFromS2.equals(setFromS1)) {
                    setsToRemove.add(setFromS1);
                    // log: a larger equality set!
                    if (useLog) {
                        Runtime.log(String.format(
                                "<%s: a larger equality set> class = %s, oriset = %s, newset = %s, dumpId = %d",
                                logPrefix, className, setFromS1, setFromS2, dumpId));
                    }
                    isStrictSupersetFound = true;
                    isChanged = true;
                }
                // See if it's a subset set
                if (setFromS1.keySet.containsAll(setFromS2.keySet)) {
                    // log: a smaller equality set!
                    isSubsetFound = true;
                }
            }

            // Remove all subsets from s1
            s1.removeAll(setsToRemove);

            if (isStrictSupersetFound) {
                s1.add(setFromS2.clone());
            } else {
                if (!isSubsetFound) {
                    // A distinguished set
                    if (useLog) {
                        Runtime.log(String.format(
                                "<%s: a new equality set> class = %s, set = %s, dumpId = %d",
                                logPrefix, className, setFromS2, dumpId));
                    }
                    isChanged = true;
                    s1.add(setFromS2.clone());
                }
            }
        }
        return isChanged;
    }

    // Deduplication and containment filtering
    public static Map<String, Set<Set<String>>> dedup(
            Map<String, Map<Integer, Set<String>>> compClass2EqualitySet) {
        Map<String, Set<Set<String>>> equalSetDiffObjDedup = new HashMap<>();
        for (String compClass : compClass2EqualitySet.keySet()) {
            Map<Integer, Set<String>> equalitySet = compClass2EqualitySet.get(compClass);
            Set<Set<String>> sets = new HashSet<>(equalitySet.values());

            // Remove the smaller sets
            Set<Set<String>> setsToRemove = new HashSet<>();
            for (Set<String> set1 : sets) {
                for (Set<String> set2 : sets) {
                    if (set1.containsAll(set2) && !set1.equals(set2)) {
                        setsToRemove.add(set2);
                    }
                }
            }
            sets.removeAll(setsToRemove);
            equalSetDiffObjDedup.put(compClass, sets);
        }
        return equalSetDiffObjDedup;
    }

    public static Set<Set<String>> deepCopy(Set<Set<String>> originalSet) {
        Set<Set<String>> deepCopiedSet = new HashSet<>();
        for (Set<String> innerSet : originalSet) {
            Set<String> copiedInnerSet = new HashSet<>(innerSet);
            deepCopiedSet.add(copiedInnerSet);
        }
        return deepCopiedSet;
    }

    public static class SetMapping implements Serializable {
        private static final long serialVersionUID = 20231215L;

        public Set<String> keySet;
        public Integer value;

        public SetMapping(Set<String> keySet, Integer value) {
            this.keySet = keySet;
            this.value = value;
        }

        // override equals and hashCode: as long as keySet is equal, then the two are
        // equal
        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof SetMapping)) {
                return false;
            }
            SetMapping other = (SetMapping) obj;
            return keySet.equals(other.keySet);
        }

        @Override
        public int hashCode() {
            return keySet.hashCode();
        }

        // clone
        @Override
        public SetMapping clone() {
            return new SetMapping(new HashSet<>(keySet), value);
        }

        // tostring
        @Override
        public String toString() {
            return String.format("SetMapping: keySet = %s, value = %d", keySet, value);
        }
    }

}
