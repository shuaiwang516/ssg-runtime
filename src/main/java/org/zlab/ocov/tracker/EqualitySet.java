package org.zlab.ocov.tracker;

import org.zlab.ocov.tracker.inv.unary.LogInfo;

import java.io.Serializable;
import java.util.*;

/**
 * There's certain overlapping between the following three conditions, one
 * example is test: testEqualityAcrossObjectGraph. Set enableAcrossEquality to
 * true can fix it, but it runs into performance problem.
 */
public class EqualitySet implements Serializable {
    private static final long serialVersionUID = 20231215L;
    private static final boolean finegrainedEqualityCheck = false;

    public Set<String> comparableClasses;
    public List<ItinerarySingleTopObject> itinerarySingleTopObjects = new LinkedList<>();

    /**
     * Equality within one object graph - Itinerary is different
     */
    public static final boolean enableSameObjEquality = true;
    public Map<String, Map<Integer, Set<String>>> equalSetSameObj = new HashMap<>();
    public Map<String, Set<SetMapping>> equalSetSameObjDedup = new HashMap<>();

    /**
     * Equality across object graphs - Itinerary is different TODO: switch to
     * setMapping?
     */
    public static final boolean enableAcrossEquality = true;
    public Map<String, Map<Integer, Map<String, Integer>>> equalSetAcrossObj = new HashMap<>();
    public Map<String, Set<Set<String>>> equalSetAcrossObjDedup = new HashMap<>();

    /**
     * Equality across object graphs - Itinerary is the same Integer means its
     * occurrence
     */
    public static final boolean enableSameItineraryAcrossObj = false;
    public Map<String, Map<Integer, Map<String, Set<Integer>>>> equalSetSameItineraryAcrossObj = new HashMap<>();
    public Map<String, Set<String>> equalSetSameItineraryAcrossObjDedup = new HashMap<>();

    static final String logPrefixAcrossObject = "Equality: across object graphs";
    static final String logPrefixSameObject = "Equality: same object graph";
    static final String logPrefixAcrossObjectSameItinerary = "Equality: across object graphs with same itinerary";

    public EqualitySet() {
        // for json
    }

    public EqualitySet(Set<String> comparableClasses) {
        // For test purpose
        // comparableClasses.add("org.apache.cassandra.cql3.ColumnIdentifier");
        this.comparableClasses = comparableClasses;
    }

    public void clear() {
        if (finegrainedEqualityCheck)
            itinerarySingleTopObjects.clear();
        else
            equalSetAcrossObj.clear();
        equalSetSameObj.clear();
        equalSetSameObjDedup.clear();
        equalSetAcrossObjDedup.clear();

        equalSetSameItineraryAcrossObj.clear();
        equalSetSameItineraryAcrossObjDedup.clear();
    }

    private static final Set<String> debugStringList = new HashSet<>();
    static {
        debugStringList.add("v1");
        debugStringList.add("v2");
        debugStringList.add("k");
    }

    /**
     * Equality format: [contextHashcode, dumpId, itinerary]
     */
    public void update(Object obj, String className, String itinerary, int objId, LogInfo logInfo) {
        if (!comparableClasses.contains(className))
            return;
        int hashCode = obj.hashCode();
        // String debugString = obj.toString();

        // if (!debugStringList.contains(obj.toString()))
        // return;

        // DEBUG
        Runtime.log("EqualitySet: update value = " + obj.toString() + ", iti = " + itinerary
                + ", dumpId = " + logInfo.dumpId);

        if (enableAcrossEquality) {
            if (!finegrainedEqualityCheck) {
                Map<Integer, Map<String, Integer>> hashCodeMap0 = equalSetAcrossObj
                        .computeIfAbsent(className, k -> new HashMap<>());
                Map<String, Integer> itinerarySet0 = hashCodeMap0.computeIfAbsent(hashCode,
                        k -> new HashMap<>());
                itinerarySet0.put(logInfo.contextHashCode + ":" + itinerary, objId);
                // itinerarySet0.put(itinerary, objId);
            }
        }

        if (enableSameObjEquality) {
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

            if (finegrainedEqualityCheck)
                itinerarySingleTopObjects.add(new ItinerarySingleTopObject(equalSetSameObj));

            // update equalSetSameItineraryAcrossObj
            if (enableSameItineraryAcrossObj)
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
     * In the end, we maintain Map<String, Set<String>> dedup
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

        if (finegrainedEqualityCheck) {
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
                other.equalSetSameItineraryAcrossObjDedup, logPrefixAcrossObjectSameItinerary)) {
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
                    Runtime.log(
                            String.format("<%s: first occur for compClass> class = %s, set = %s",
                                    logPrefix, compClass, equalSetDedup2.get(compClass)));
                }
                changed = true;
            }
        }
        return changed;
    }

    public static boolean merge1(Map<String, Map<String, Map<String, Integer>>> equalSetAcrossObj1,
            Map<String, Map<String, Map<String, Integer>>> equalSetAcrossObj2) {
        boolean changed = false;
        for (String compClass : equalSetAcrossObj2.keySet()) {
            if (equalSetAcrossObj1.containsKey(compClass)) {
                Map<String, Map<String, Integer>> hashCodeMap1 = equalSetAcrossObj1.get(compClass);
                Map<String, Map<String, Integer>> hashCodeMap2 = equalSetAcrossObj2.get(compClass);
                for (String hashCode : hashCodeMap2.keySet()) {
                    if (hashCodeMap1.containsKey(hashCode)) {
                        Map<String, Integer> itinerarySet1 = hashCodeMap1.get(hashCode);
                        Map<String, Integer> itinerarySet2 = hashCodeMap2.get(hashCode);
                        for (String itinerary : itinerarySet2.keySet()) {
                            if (!itinerarySet1.containsKey(itinerary)) {
                                itinerarySet1.put(itinerary, itinerarySet2.get(itinerary));
                                Runtime.log(String.format(
                                        "<debug %s: new itinerary> class = %s, hashCode = %s, itinerary = %s",
                                        logPrefixAcrossObject, compClass, hashCode, itinerary));
                                changed = true;
                            }
                        }
                    } else {
                        Runtime.log(String.format(
                                "<debug %s: new hashCode> class = %s, hashCode = %s, itinerary = %s",
                                logPrefixAcrossObject, compClass, hashCode,
                                hashCodeMap2.get(hashCode)));
                        hashCodeMap1.put(hashCode, new HashMap<>(hashCodeMap2.get(hashCode)));
                        changed = true;
                    }
                }
            } else {
                Runtime.log(String.format("<debug %s: new compClass> class = %s, hashCodeMap = %s",
                        logPrefixAcrossObject, compClass, equalSetAcrossObj2.get(compClass)));
                equalSetAcrossObj1.put(compClass, new HashMap<>(equalSetAcrossObj2.get(compClass)));
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
                    Runtime.log(
                            String.format("<%s: first occur for compClass> class = %s, set = %s",
                                    logPrefix, compClass, equalSetDedup2.get(compClass)));
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
            Map<String, Set<String>> equalSetDedup2, String logPrefix) {
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
                Runtime.log(String.format(
                        "<%s: larger set, same itinerary> class = %s, newset = %s, oldset = %s",
                        logPrefix, compClass, itinerarySet1, itinerarySet2));
                changed = true;
            } else {
                if (!equalSetDedup2.get(compClass).isEmpty()) {
                    equalSetDedup1.put(compClass, new HashSet<>(equalSetDedup2.get(compClass)));
                    Runtime.log(String.format("<%s: new set, same itinerary> class = %s, set = %s",
                            logPrefix, compClass, equalSetDedup2.get(compClass)));
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
                                "<%s: larger set, diff itinerary> class = %s, newset = %s, oldset = %s",
                                logPrefix, className, setFromS2, setFromS1));
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
                        Runtime.log(String.format(
                                "<%s: new set> class = %s, cur set size = %d, set = %s", logPrefix,
                                className, s1.size(), setFromS2));
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
            for (SetMapping setFromS1 : s1) {
                if (setFromS2.keySet.containsAll(setFromS1.keySet)
                        && !setFromS2.equals(setFromS1)) {
                    setsToRemove.add(setFromS1);
                    if (useLog) {
                        Runtime.log(String.format(
                                "<%s: larger set, diff itinerary> class = %s, newset = %s, oldset = %s",
                                logPrefix, className, setFromS2, setFromS1));
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
                                "<%s: new set> class = %s, cur set size = %d, set = %s", logPrefix,
                                className, s1.size(), setFromS2));
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

        private Set<String> keySet;
        private Integer dumpId; // For equality, this stores the dumpId

        public SetMapping(Set<String> keySet, Integer dumpId) {
            this.keySet = keySet;
            this.dumpId = dumpId;
        }

        public int getDumpId() {
            return dumpId;
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
            return new SetMapping(new HashSet<>(keySet), dumpId);
        }

        // tostring
        @Override
        public String toString() {
            return String.format("SetMapping: keySet = %s, dumpId = %d", keySet, dumpId);
        }
    }

    public static class ItinerarySingleTopObject implements Serializable {
        private static final long serialVersionUID = 20231215L;
        public Map<String, Map<Integer, Set<String>>> itineraries;

        public ItinerarySingleTopObject(Map<String, Map<Integer, Set<String>>> itineraries) {
            this.itineraries = itineraries;
        }
    }

    public static void extractEqualityEdgesAcross(
            Map<Integer, Map<Integer, Set<String>>> equalityEdges,
            Map<String, Map<Integer, Map<String, Integer>>> equalSetAcrossObj) {
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
    }

    public static void extractEqualityEdgesSameIti(
            Map<Integer, Map<Integer, Set<String>>> equalityEdges,
            Map<String, Map<Integer, Map<String, Set<Integer>>>> equalSetSameItineraryAcrossObj) {
        for (Map.Entry<String, Map<Integer, Map<String, Set<Integer>>>> entry : equalSetSameItineraryAcrossObj
                .entrySet()) {
            for (Map.Entry<Integer, Map<String, Set<Integer>>> objEntry : entry.getValue()
                    .entrySet()) {
                Map<String, Set<Integer>> itinerarySet = objEntry.getValue();
                for (Map.Entry<String, Set<Integer>> itineraryEntry1 : itinerarySet.entrySet()) {
                    String itinerary = itineraryEntry1.getKey();
                    Set<Integer> objIds = itineraryEntry1.getValue();
                    for (int objId1 : objIds) {
                        for (int objId2 : objIds) {
                            if (objId1 == objId2)
                                continue;
                            String edge = String.format("<Equality> %s == %s", itinerary,
                                    itinerary);
                            equalityEdges.computeIfAbsent(objId1, k -> new HashMap<>())
                                    .computeIfAbsent(objId2, k -> new HashSet<>()).add(edge);
                        }
                    }
                }
            }
        }
    }
}
