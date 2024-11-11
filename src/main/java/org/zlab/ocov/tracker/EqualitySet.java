package org.zlab.ocov.tracker;

import org.zlab.ocov.Utils;
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

    public transient Set<String> comparableClasses;
    /**
     * Equality within one object graph - Itinerary is different Equality ClassName
     * -> DumpId -> Equalities
     */
    public static final boolean enableSameObjEquality = true;
    public transient Map<String, Map<Integer, Map<Integer, Set<String>>>> equalSetSameObj = new HashMap<>();
    public Map<String, Map<Integer, Set<Set<String>>>> equalSetSameObjDedup = new HashMap<>();

    static final String logPrefixAcrossObject = "Equality: across object graphs";
    static final String logPrefixSameObject = "Equality: same object graph";
    static final String logPrefixAcrossObjectSameItinerary = "Equality: across object graphs with same itinerary";

    static final FormatCoverageStatus dummyFormatCoverageStatus = new FormatCoverageStatus();

    public EqualitySet() {
        // for json
    }

    public EqualitySet(Set<String> comparableClasses) {
        // For test purpose
        // comparableClasses.add("org.apache.cassandra.cql3.ColumnIdentifier");
        this.comparableClasses = comparableClasses;
    }

    public void clear() {
        equalSetSameObj.clear();
        equalSetSameObjDedup.clear();
    }

    private static final Set<String> debugStringList = new HashSet<>();
    static {
        debugStringList.add("v1");
        debugStringList.add("v2");
        debugStringList.add("k");
    }

    /**
     * Equality format: [contextHashcode, dumpId, itinerary] contextHashcode:
     * hashcode of a creation stacktrace (in string format)
     */
    public void update(Object obj, String className, String itinerary, int objId, LogInfo logInfo) {
        if (!Runtime.enableEqualityLikelyInvariant)
            return;
        if (!comparableClasses.contains(className))
            return;
        int hashCode = obj.hashCode();

        // String debugString = obj.toString();
        // if (!debugStringList.contains(obj.toString()))
        // return;

        if (Runtime.debug)
            Runtime.log("EqualitySet: context hashcode = " + logInfo.contextHashCode
                    + ", update value = " + obj.toString() + ", iti = " + itinerary + ", dumpId = "
                    + logInfo.dumpId);

        if (enableSameObjEquality) {
            Map<Integer, Map<Integer, Set<String>>> dumpId2hashCodeMap1 = equalSetSameObj
                    .computeIfAbsent(className, k -> new HashMap<>());
            Map<Integer, Set<String>> hashCodeMap1 = dumpId2hashCodeMap1
                    .computeIfAbsent(logInfo.dumpId, k -> new HashMap<>());
            Set<String> itinerarySet1 = hashCodeMap1.computeIfAbsent(hashCode,
                    k -> new HashSet<>());
            itinerarySet1.add(logInfo.contextHashCode + ":" + itinerary);
        }
    }

    public void dumpSameObjectGraph(int dumpId, Set<String> brokenInvs) {
        if (equalSetSameObj.isEmpty())
            return;

        if (Runtime.debug) {
            Runtime.log("[dumpSameObjectGraph] before merge: dumpId = " + dumpId
                    + ", equalSetSameObjDedup = " + equalSetSameObjDedup + ", equalSetSameObj = "
                    + equalSetSameObj);
        }

        Map<String, Map<Integer, Set<Set<String>>>> dedupEqualSetSameObj = dedup(equalSetSameObj);

        if (ObjectGraphCoverage.enableInvariantCombination) {
            brokenInvs.addAll(collapse(dedupEqualSetSameObj));
        }

        mergeCompClass2EqualityDedupWithDumpId(equalSetSameObjDedup, dedupEqualSetSameObj, false,
                logPrefixSameObject, dummyFormatCoverageStatus, null);
        if (Runtime.debug) {
            Runtime.log("[dumpSameObjectGraph] after merge: dumpId = " + dumpId
                    + ", equalSetSameObjDedup = " + equalSetSameObjDedup + ", equalSetSameObj = "
                    + equalSetSameObj);
        }

        equalSetSameObj = new HashMap<>();
    }

    public void merge(EqualitySet other, FormatCoverageStatus formatCoverageStatus,
            Map<String, Map<String, String>> matchableClassInfo) {
        if (other == null) {
            return;
        }
        mergeCompClass2EqualityDedupWithDumpId(equalSetSameObjDedup, other.equalSetSameObjDedup,
                true, logPrefixSameObject, formatCoverageStatus, matchableClassInfo);
    }

    public static void mergeCompClass2EqualityDedupWithDumpId(
            Map<String, Map<Integer, Set<Set<String>>>> equalSetDedup1,
            Map<String, Map<Integer, Set<Set<String>>>> equalSetDedup2, boolean useLog,
            String logPrefix, FormatCoverageStatus formatCoverageStatus,
            Map<String, Map<String, String>> matchableClassInfo) {
        boolean changed = false;
        boolean nonMatchable = false;
        for (String compClass : equalSetDedup2.keySet()) {
            if (!equalSetDedup1.containsKey(compClass)) {
                Map<Integer, Set<Set<String>>> tmpMap = new HashMap<>();
                // deep copy, include the set
                for (Map.Entry<Integer, Set<Set<String>>> entry : equalSetDedup2.get(compClass)
                        .entrySet()) {
                    int dumpId = entry.getKey();
                    Set<Set<String>> tmpSet = new HashSet<>();
                    for (Set<String> set : entry.getValue()) {
                        tmpSet.add(new HashSet<>(set));
                    }
                    tmpMap.put(dumpId, tmpSet);
                    // No need to check modification here.
                    // If a comp class occur firstly, it must have led to new likely invariants
                    // The mod check is already performed previously
                }
                equalSetDedup1.put(compClass, tmpMap);
                if (useLog) {
                    Runtime.log(
                            String.format("<%s: first occur for compClass> class = %s, set = %s",
                                    logPrefix, compClass, equalSetDedup2.get(compClass)));
                }
                changed = true;
                continue;
            }
            Map<Integer, Set<Set<String>>> equalitySets = equalSetDedup1.get(compClass);
            Map<Integer, Set<Set<String>>> otherEqualitySets = equalSetDedup2.get(compClass);
            for (Integer dumpId : otherEqualitySets.keySet()) {
                if (!equalitySets.containsKey(dumpId)) {
                    Set<Set<String>> tmpSet = new HashSet<>();
                    for (Set<String> set : otherEqualitySets.get(dumpId)) {
                        tmpSet.add(new HashSet<>(set));
                    }
                    equalitySets.put(dumpId, tmpSet);
                    if (useLog) {
                        Runtime.log(String.format(
                                "<%s: first occur for dumpId> class = %s, dumpId = %d, set = %s",
                                logPrefix, compClass, dumpId, otherEqualitySets.get(dumpId)));
                    }
                    // No modification check: similar reason as above
                    changed = true;
                    continue;
                }
                Set<Set<String>> equalitySet = equalitySets.get(dumpId);
                Set<Set<String>> otherEqualitySet = otherEqualitySets.get(dumpId);
                MergeStatus mergeStatus = mergeSets(equalitySet, otherEqualitySet, compClass,
                        useLog, logPrefix, matchableClassInfo);
                if (mergeStatus.changed) {
                    changed = true;
                }
                if (mergeStatus.nonMatchable)
                    nonMatchable = true;
            }
        }
        if (changed)
            formatCoverageStatus.setNewFormat("New equalitySet");
        if (nonMatchable)
            formatCoverageStatus.setNonMatchableNewFormat("Non-matchable equalitySet");
    }

    public static boolean mergeSets(Set<Set<String>> s1, Set<Set<String>> s2, String className) {
        return mergeSets(s1, s2, className, true, "Equality:", new HashMap<>()).changed;
    }

    // Merge s2 into s1
    public static MergeStatus mergeSets(Set<Set<String>> s1, Set<Set<String>> s2, String className,
            boolean useLog, String logPrefix, Map<String, Map<String, String>> matchableClassInfo) {
        boolean isChanged = false;
        boolean nonMatchable = false;

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
                // Check larger set: setFromS2 > setFromS1
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
                // Check smaller set: setFromS1 >= setFromS2
                if (setFromS1.containsAll(setFromS2)) {
                    // log: a smaller equality set!
                    isSubsetFound = true;
                }
            }

            // Remove all subsets from s1
            s1.removeAll(setsToRemove);

            if (isStrictSupersetFound) {
                s1.add(setFromS2);
                if (!nonMatchable && matchableClassInfo != null
                        && checkNonMatchable(setFromS2, matchableClassInfo)) {
                    nonMatchable = true;
                }
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
                    if (!nonMatchable && matchableClassInfo != null
                            && checkNonMatchable(setFromS2, matchableClassInfo)) {
                        nonMatchable = true;
                    }
                }
            }
        }
        return new MergeStatus(isChanged, nonMatchable);
    }

    public static boolean checkNonMatchable(Set<String> itis,
            Map<String, Map<String, String>> matchableClassInfo) {
        for (String iti : itis) {
            if (!Utils.isMatchableFormat(matchableClassInfo, iti)) {
                return true;
            }
        }
        return false;
    }

    // Deduplication and containment filtering
    public static Map<String, Map<Integer, Set<Set<String>>>> dedup(
            Map<String, Map<Integer, Map<Integer, Set<String>>>> compClass2EqualitySet) {
        Map<String, Map<Integer, Set<Set<String>>>> equalSetAcrossObjDedup = new HashMap<>();
        for (String compClass : compClass2EqualitySet.keySet()) {
            Map<Integer, Map<Integer, Set<String>>> equalitySet = compClass2EqualitySet
                    .get(compClass);
            for (Integer dumpId : equalitySet.keySet()) {
                Map<Integer, Set<String>> equalitySetDumpId = equalitySet.get(dumpId);
                Set<Set<String>> sets = dedupEqualitySet(equalitySetDumpId);
                equalSetAcrossObjDedup.computeIfAbsent(compClass, k -> new HashMap<>()).put(dumpId,
                        sets);
            }
        }
        return equalSetAcrossObjDedup;
    }

    public static Set<Set<String>> dedupEqualitySet(Map<Integer, Set<String>> equalitySet) {
        Set<Set<String>> sets = new HashSet<>(equalitySet.values());
        dedupSet(sets);
        return sets;
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

    /**
     * Used for invariant combination Format: [compClass: iti_1, iti_2, iti_n]
     */
    public static Set<String> collapse(
            Map<String, Map<Integer, Set<Set<String>>>> dedupEqualSetSameObj) {
        Set<String> collapsedSet = new HashSet<>();
        for (String compClass : dedupEqualSetSameObj.keySet()) {
            Map<Integer, Set<Set<String>>> dumpId2EqualitySet = dedupEqualSetSameObj.get(compClass);
            for (Integer dumpId : dumpId2EqualitySet.keySet()) {
                Set<Set<String>> equalitySet = dumpId2EqualitySet.get(dumpId);
                for (Set<String> equality : equalitySet) {
                    collapsedSet.add("<Equality> " + compClass + ": " + new TreeSet<>(equality));
                }
            }
        }
        return collapsedSet;
    }

    public static class MergeStatus {
        public boolean changed;
        public boolean nonMatchable;

        public MergeStatus(boolean changed, boolean nonMatchable) {
            this.changed = changed;
            this.nonMatchable = nonMatchable;
        }
    }
}
