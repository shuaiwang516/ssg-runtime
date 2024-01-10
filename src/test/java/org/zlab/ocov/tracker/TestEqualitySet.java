package org.zlab.ocov.tracker;

import org.junit.jupiter.api.Test;
import org.zlab.ocov.Utils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class TestEqualitySet {

    @Test
    public void testMergeSets1() {

        Set<Set<String>> s1 = new HashSet<>();
        Set<Set<String>> s2 = new HashSet<>();

        Set<String> s11 = new HashSet<>();
        s11.add("a");
        s11.add("b");

        Set<String> s12 = new HashSet<>();
        s12.add("a");
        s12.add("c");

        s1.add(s11);
        s1.add(s12);

        Set<String> s21 = new HashSet<>();
        s21.add("a");
        s21.add("b");
        s21.add("c");
        s2.add(s21);

        assert EqualitySet.mergeSets(s1, s2, null);
        assert s1.size() == 1;
    }

    @Test
    public void testMergeSets2() {

        Set<Set<String>> s1 = new HashSet<>();
        Set<Set<String>> s2 = new HashSet<>();

        Set<String> s11 = new HashSet<>();
        s11.add("a");
        s11.add("b");

        Set<String> s12 = new HashSet<>();
        s12.add("a");
        s12.add("c");

        s1.add(s11);
        s1.add(s12);

        Set<String> s21 = new HashSet<>();
        s21.add("b");
        s21.add("c");
        s2.add(s21);

        assert EqualitySet.mergeSets(s1, s2, null);
        assert s1.size() == 3;
    }

    @Test
    public void testMergeSets3() {

        Set<Set<String>> s1 = new HashSet<>();
        Set<Set<String>> s2 = new HashSet<>();

        Set<String> s21 = new HashSet<>();
        s21.add("b");
        s21.add("c");
        s2.add(s21);

        assert EqualitySet.mergeSets(s1, s2, null);
        assert s1.size() == 1;
    }

    // @Test
    public void testLoadComparableClasses() {
        Path p = Paths.get(
                "/Users/hanke/Desktop/Project/vasco/system/cassandra/apache-cassandra-2.2.8/comparableClasses.json");
        Set<String> comparableClasses = Utils.loadSetFromFile(p.toString());
        // print size
        System.out.println(comparableClasses.size());
    }

}
