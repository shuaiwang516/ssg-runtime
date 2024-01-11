package org.zlab.ocov.tracker;

import java.io.Serializable;
import java.util.*;

public class EqualitySet implements Serializable {
    private static final long serialVersionUID = 20231215L;

    public Set<String> comparableClasses;
    public Map<String, Map<Integer, Set<String>>> compClass2EqualitySet = new HashMap<>();

    public Map<String, Set<Set<String>>> compClass2EqualitySetDedup = new HashMap<>();

    public EqualitySet() {
        // for json
    }

    public EqualitySet(Set<String> comparableClasses) {
        // For test purpose
        // comparableClasses.add("org.apache.cassandra.cql3.ColumnIdentifier");
        this.comparableClasses = comparableClasses;
    }

    // Deduplication and containment filtering
    public Map<String, Set<Set<String>>> deduplicate() {
        Map<String, Set<Set<String>>> compClass2EqualitySetDedup = new HashMap<>();
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
            compClass2EqualitySetDedup.put(compClass, sets);
        }
        return compClass2EqualitySetDedup;
    }

    public boolean merge(EqualitySet other) {
        if (other == null) {
            return false;
        }
        boolean changed = false;
        Map<String, Set<Set<String>>> otherCompClass2EqualitySetDedup = other.deduplicate();
        for (String compClass : otherCompClass2EqualitySetDedup.keySet()) {
            if (compClass2EqualitySetDedup.containsKey(compClass)) {
                Set<Set<String>> equalitySets = compClass2EqualitySetDedup.get(compClass);
                Set<Set<String>> otherEqualitySets = otherCompClass2EqualitySetDedup.get(compClass);
                if (mergeSets(equalitySets, otherEqualitySets, compClass)) {
                    changed = true;
                }
            } else {
                compClass2EqualitySetDedup.put(compClass,
                        otherCompClass2EqualitySetDedup.get(compClass));
                changed = true;
            }
        }
        return changed;
    }

    public static boolean mergeSets(Set<Set<String>> s1, Set<Set<String>> s2, String className) {
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
                    Runtime.log(String.format(
                            "<Equality: a larger equality set> class = %s, oriset = %s, newset = %s",
                            className, setFromS1, setFromS2));
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
                    Runtime.log(String.format("<Equality: a new equality set> class = %s, set = %s",
                            className, setFromS2));
                    isChanged = true;
                    s1.add(setFromS2);
                }
            }
        }
        return isChanged;
    }

}
