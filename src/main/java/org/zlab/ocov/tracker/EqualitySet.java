package org.zlab.ocov.tracker;

import org.zlab.ocov.tracker.graph.ObjectGraph;

import java.io.Serializable;
import java.util.*;

public class EqualitySet implements Serializable {
    private static final long serialVersionUID = 20231215L;
    public static boolean enableAcrossEquality = false;

    public Set<String> comparableClasses;

    public List<ItinerarySingleTopObject> itinerarySingleTopObjects = new LinkedList<>();

    /**
     * Currently there's certain overlapping between the following three conditions,
     * one example is test: testEqualityAcrossObjectGraph. Set enableAcrossEquality
     * to true can fix it, but it runs into performance problem.
     */

    /**
     * Equality within one object graph - Itinerary is different
     */
    public Map<String, Map<Integer, Set<String>>> equalSetSameObj = new HashMap<>();
    public Map<String, Set<SetMapping>> equalSetSameObjDedup = new HashMap<>();

    /**
     * Equality across object graphs - Itinerary is different
     */
    public Map<String, Map<Integer, Map<String, Integer>>> equalSetAcrossObj = new HashMap<>();
    public Map<String, Set<Set<String>>> equalSetAcrossObjDedup = new HashMap<>();

    /**
     * Equality across object graphs - Itinerary is the same
     */
    public Map<String, Map<Integer, Map<String, Set<Integer>>>> equalSetSameItineraryAcrossObj = new HashMap<>();
    public Map<String, Set<String>> equalSetSameItineraryAcrossObjDedup = new HashMap<>();

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

    public void clear() {
        if (enableAcrossEquality)
            itinerarySingleTopObjects.clear();
        else
            equalSetAcrossObj.clear();
        equalSetSameObj.clear();
        equalSetSameObjDedup.clear();
        equalSetAcrossObjDedup.clear();

        equalSetSameItineraryAcrossObj.clear();
        equalSetSameItineraryAcrossObjDedup.clear();
    }

    public void update(ObjectGraph.Vertex vertex, String className, String itinerary, int objId) {
        if (comparableClasses.contains(className)) {
            assert vertex.value instanceof Integer;
            int hashCode = (int) vertex.value;
            if (!enableAcrossEquality) {
                Map<Integer, Map<String, Integer>> hashCodeMap0 = equalSetAcrossObj
                        .computeIfAbsent(className, k -> new HashMap<>());
                Map<String, Integer> itinerarySet0 = hashCodeMap0.computeIfAbsent(hashCode,
                        k -> new HashMap<>());
                itinerarySet0.put(itinerary, objId);
            }

            Map<Integer, Set<String>> hashCodeMap1 = equalSetSameObj.computeIfAbsent(className,
                    k -> new HashMap<>());
            Set<String> itinerarySet1 = hashCodeMap1.computeIfAbsent(hashCode,
                    k -> new HashSet<>());
            itinerarySet1.add(itinerary);
        }
    }

    public void update(Object obj, String className, String itinerary, int objId) {
        if (comparableClasses.contains(className)) {
            int hashCode = obj.hashCode();
            if (!enableAcrossEquality) {
                Map<Integer, Map<String, Integer>> hashCodeMap0 = equalSetAcrossObj
                        .computeIfAbsent(className, k -> new HashMap<>());
                Map<String, Integer> itinerarySet0 = hashCodeMap0.computeIfAbsent(hashCode,
                        k -> new HashMap<>());
                itinerarySet0.put(itinerary, objId);
            }

            Map<Integer, Set<String>> hashCodeMap1 = equalSetSameObj.computeIfAbsent(className,
                    k -> new HashMap<>());
            Set<String> itinerarySet1 = hashCodeMap1.computeIfAbsent(hashCode,
                    k -> new HashSet<>());
            itinerarySet1.add(itinerary);
        }
    }

    public void dumpSameObjectGraph(int dumpId, int objId) {
        if (!equalSetSameObj.isEmpty()) {
            mergeCompClass2EqualityDedup(equalSetSameObjDedup, dedup(equalSetSameObj), false,
                    logPrefixSameObject, dumpId);

            if (enableAcrossEquality)
                itinerarySingleTopObjects.add(new ItinerarySingleTopObject(equalSetSameObj));

            // update equalSetSameItineraryAcrossObj
            updateEqualSetSameItineraryAcrossObjDedup(equalSetSameObj, objId);

            equalSetSameObj = new HashMap<>();
        }
    }

    // Multi occurrence with the same itinerary
    /**
     * equalSetSameObj for each object, merge them into
     * equalSetSameItineraryAcrossObj - Check whether the iti is already included in
     * equalSetSameItineraryAcrossObjDedup - If not, check whether it's already
     * included in equalSetSameItineraryAcrossObj - If yes, add the iti to the set
     * In the end, we maintain Map<String, Set<String> dedup
     */
    public void updateEqualSetSameItineraryAcrossObjDedup(
            Map<String, Map<Integer, Set<String>>> equalSetSameObj, int objId) {
        for (String compClass : equalSetSameObj.keySet()) {
            Map<Integer, Set<String>> hashCodeMap1 = equalSetSameObj.get(compClass);
            Map<Integer, Map<String, Set<Integer>>> hashCodeMap2 = equalSetSameItineraryAcrossObj
                    .computeIfAbsent(compClass, k -> new HashMap<>());

            Set<String> dupSet = equalSetSameItineraryAcrossObjDedup.computeIfAbsent(compClass,
                    k -> new HashSet<>());

            for (Integer hashCode : hashCodeMap1.keySet()) {
                Set<String> itinerarySet1 = hashCodeMap1.get(hashCode);
                for (String itinerary : itinerarySet1) {
                    if (dupSet.contains(itinerary)) {
                        continue;
                    }
                    if (hashCodeMap2.containsKey(hashCode)
                            && hashCodeMap2.get(hashCode).containsKey(itinerary)) {
                        equalSetSameItineraryAcrossObjDedup
                                .computeIfAbsent(compClass, k -> new HashSet<>()).add(itinerary);
                    }
                    hashCodeMap2.computeIfAbsent(hashCode, k -> new HashMap<>())
                            .computeIfAbsent(itinerary, k -> new HashSet<>()).add(objId);

                }
            }
        }
    }

    public Map<String, Set<Set<String>>> dedupAcrossObjectGraph() {
        Map<String, Set<Set<String>>> equalSetAcrossObjDedup = new HashMap<>();

        if (enableAcrossEquality) {
            // Compute equality only once when merging
            Map<String, Map<Integer, Set<Set<String>>>> equalSetAcrossObjTmp = new HashMap<>();

            for (ItinerarySingleTopObject itinerarySingleTopObject : itinerarySingleTopObjects) {
                Map<String, Map<Integer, Set<String>>> itineraries = itinerarySingleTopObject.itineraries;

                for (String compClass : itineraries.keySet()) {
                    Map<Integer, Set<String>> hashCodeMap1 = itineraries.get(compClass);
                    Map<Integer, Set<Set<String>>> hashCodeMap2 = equalSetAcrossObjTmp
                            .computeIfAbsent(compClass, k -> new HashMap<>());
                    for (Integer hashCode : hashCodeMap1.keySet()) {
                        Set<String> itinerarySet1 = hashCodeMap1.get(hashCode);
                        Set<Set<String>> itinerarySetOri = hashCodeMap2.computeIfAbsent(hashCode,
                                k -> new HashSet<>());
                        Set<Set<String>> itinerarySetNew = new HashSet<>();
                        for (String itinerary : itinerarySet1) {
                            for (Set<String> itinerarySet : itinerarySetOri) {
                                Set<String> itinerarySetClone = new HashSet<>(itinerarySet);
                                itinerarySetClone.add(itinerary);
                                itinerarySetNew.add(itinerarySetClone);
                            }
                            Set<String> selfSet = new HashSet<>();
                            selfSet.add(itinerary);
                            itinerarySetNew.add(selfSet);
                        }
                        itinerarySetOri.clear();
                        itinerarySetOri.addAll(itinerarySetNew);
                    }
                }
            }
            for (String compClass : equalSetAcrossObjTmp.keySet()) {
                Map<Integer, Set<Set<String>>> hashCodeMap = equalSetAcrossObjTmp.get(compClass);
                Set<Set<String>> sets = new HashSet<>();
                for (Integer hashCode : hashCodeMap.keySet()) {
                    for (Set<String> set : hashCodeMap.get(hashCode)) {
                        sets.add(new HashSet<>(set));
                    }
                }
                dedupSet(sets);
                equalSetAcrossObjDedup.put(compClass, sets);
            }
            return equalSetAcrossObjDedup;
        } else {
            for (String compClass : equalSetAcrossObj.keySet()) {
                Map<Integer, Map<String, Integer>> hashCodeMap = equalSetAcrossObj.get(compClass);
                Set<Set<String>> sets = new HashSet<>();
                for (Integer hashCode : hashCodeMap.keySet()) {
                    Set<String> set = new HashSet<>(hashCodeMap.get(hashCode).keySet());
                    sets.add(set);
                }
                dedupSet(sets);
                equalSetAcrossObjDedup.put(compClass, deepCopy(sets));
            }
            return equalSetAcrossObjDedup;
        }
    }

    public void infer() {
        equalSetAcrossObjDedup = dedupAcrossObjectGraph();
    }

    public boolean merge(EqualitySet other) {
        if (other == null) {
            return false;
        }
        boolean changed = false;
        if (mergeCompClass2EqualityDedup(equalSetAcrossObjDedup, other.equalSetAcrossObjDedup, true,
                logPrefixAcrossObject)) {
            changed = true;
        }
        if (mergeCompClass2EqualityDedupWithDumpId(equalSetSameObjDedup, other.equalSetSameObjDedup,
                true, logPrefixSameObject)) {
            changed = true;
        }
        if (mergeEqualityWithSameItinerary(equalSetSameItineraryAcrossObjDedup,
                other.equalSetSameItineraryAcrossObjDedup)) {
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
                            "<Equality: new set, diff itinerary> class = %s, set = %s", compClass,
                            equalSetDedup2.get(compClass)));
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
                            "<Equality: new set, diff itinerary> class = %s, set = %s", compClass,
                            equalSetDedup2.get(compClass)));
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

    public static boolean mergeEqualityWithSameItinerary(Map<String, Set<String>> equalSetDedup1,
            Map<String, Set<String>> equalSetDedup2) {
        // merge 2 into 1
        boolean changed = false;

        for (String compClass : equalSetDedup2.keySet()) {
            if (equalSetDedup1.containsKey(compClass)) {
                Set<String> itinerarySet1 = equalSetDedup1.get(compClass);
                Set<String> itinerarySet2 = equalSetDedup2.get(compClass);
                if (itinerarySet1.containsAll(itinerarySet2)) {
                    // 2 is a subset of 1
                    continue;
                }
                if (itinerarySet2.containsAll(itinerarySet1)) {
                    // 1 is a subset of 2
                    equalSetDedup1.put(compClass, itinerarySet2);
                    changed = true;
                    continue;
                }
                // 1 and 2 are not subsets of each other
                // merge 2 into 1
                itinerarySet1.addAll(itinerarySet2);
                Runtime.log(
                        String.format("<Equality: larger set, same itinerary> class = %s, set = %s",
                                compClass, itinerarySet1));
                changed = true;
            } else {
                if (!equalSetDedup2.get(compClass).isEmpty()) {
                    equalSetDedup1.put(compClass, new HashSet<>(equalSetDedup2.get(compClass)));
                    Runtime.log(String.format(
                            "<Equality: new set, same itinerary> class = %s, set = %s", compClass,
                            equalSetDedup2.get(compClass)));
                    changed = true;
                }
            }
        }
        return changed;
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
                    if (useLog) {
                        Runtime.log(String.format(
                                "<%s: larger set, diff itinerary> class = %s, oriset = %s, newset = %s",
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
                        Runtime.log(
                                String.format("<%s: new set, diff itinerary> class = %s, set = %s",
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
                    if (useLog) {
                        Runtime.log(String.format(
                                "<%s: larger set, diff itinerary> class = %s, oriset = %s, newset = %s, dumpId = %d",
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
                                "<%s: new set, diff itinerary> class = %s, set = %s, dumpId = %d",
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
        Map<String, Set<Set<String>>> equalSetAcrossObjDedup = new HashMap<>();
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
            equalSetAcrossObjDedup.put(compClass, sets);
        }
        return equalSetAcrossObjDedup;
    }

    // Deduplication and containment filtering
    public static void dedupSet(Set<Set<String>> sets) {
        Set<Set<String>> setsToRemove = new HashSet<>();
        for (Set<String> set1 : sets) {
            for (Set<String> set2 : sets) {
                if (set1.containsAll(set2) && !set1.equals(set2)) {
                    setsToRemove.add(set2);
                }
            }
        }
        sets.removeAll(setsToRemove);
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

    public static class ItinerarySingleTopObject implements Serializable {
        private static final long serialVersionUID = 20231215L;
        public Map<String, Map<Integer, Set<String>>> itineraries;

        public ItinerarySingleTopObject(Map<String, Map<Integer, Set<String>>> itineraries) {
            this.itineraries = itineraries;
        }
    }

    public static Map<Integer, Map<Integer, Set<String>>> extractEqualityEdges(
            Map<String, Map<Integer, Map<String, Integer>>> equalSetAcrossObj) {
        Map<Integer, Map<Integer, Set<String>>> equalityEdges = new HashMap<>();
        for (Map.Entry<String, Map<Integer, Map<String, Integer>>> entry : equalSetAcrossObj
                .entrySet()) {
            for (Map.Entry<Integer, Map<String, Integer>> objEntry : entry.getValue().entrySet()) {
                Map<String, Integer> itinerarySet = objEntry.getValue();
                for (Map.Entry<String, Integer> itineraryEntry1 : itinerarySet.entrySet()) {
                    String itinerary1 = itineraryEntry1.getKey();
                    int objId1 = itineraryEntry1.getValue();
                    for (Map.Entry<String, Integer> itineraryEntry2 : itinerarySet.entrySet()) {
                        String itinerary2 = itineraryEntry2.getKey();
                        int objId2 = itineraryEntry2.getValue();
                        if (objId1 != objId2) {
                            String edge = String.format("<Equality> %s == %s", itinerary1,
                                    itinerary2);
                            equalityEdges.computeIfAbsent(objId1, k -> new HashMap<>())
                                    .computeIfAbsent(objId2, k -> new HashSet<>()).add(edge);
                        }
                    }
                }
            }
        }
        return equalityEdges;
    }

}
