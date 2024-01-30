package org.zlab.ocov.tracker;

import org.junit.jupiter.api.Test;
import org.zlab.ocov.Utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GenerateTestInput {
    @Test
    public void createExampleInput() {
        Map<String, Map<String, String>> baseClassInfo = new HashMap<>();
        // org.zlab.ocov.tracker.TestObjectGraph$TargetClassA
        baseClassInfo.put("org.zlab.ocov.tracker.TestObjectGraph$TargetClassA", new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TestObjectGraph$TargetClassA").put("a", "int");
        baseClassInfo.get("org.zlab.ocov.tracker.TestObjectGraph$TargetClassA").put("b", "int");
        baseClassInfo.get("org.zlab.ocov.tracker.TestObjectGraph$TargetClassA").put("bObj",
                "org.zlab.ocov.tracker.TestObjectGraph$TargetClassB");

        // org.zlab.ocov.tracker.TestObjectGraph$TargetClassB
        baseClassInfo.put("org.zlab.ocov.tracker.TestObjectGraph$TargetClassB", new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TestObjectGraph$TargetClassB").put("i", "int");
        baseClassInfo.get("org.zlab.ocov.tracker.TestObjectGraph$TargetClassB").put("ids",
                "java.util.List");

        // org.zlab.ocov.tracker.TestObjectGraph$TargetClassC
        baseClassInfo.put("org.zlab.ocov.tracker.TestObjectGraph$TargetClassC", new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TestObjectGraph$TargetClassC").put("c", "int");

        // org.zlab.ocov.tracker.TestObjectGraph$TargetClassD
        baseClassInfo.put("org.zlab.ocov.tracker.TestObjectGraph$TargetClassD", new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TestObjectGraph$TargetClassD").put("a", "int");
        baseClassInfo.get("org.zlab.ocov.tracker.TestObjectGraph$TargetClassD").put("bObj",
                "org.zlab.ocov.tracker.TestObjectGraph$TargetClassB");
        baseClassInfo.get("org.zlab.ocov.tracker.TestObjectGraph$TargetClassD").put("dObj",
                "org.zlab.ocov.tracker.TestObjectGraph$TargetClassD");
        Utils.saveMapToFile(baseClassInfo, "input/baseClassInfo.json");

        Set<String> topObjects = new HashSet<>();
        topObjects.add("org.zlab.ocov.tracker.TestObjectGraph$TargetClassA");
        topObjects.add("org.zlab.ocov.tracker.TestObjectGraph$TargetClassD");
        Utils.saveSetToFile(topObjects, "input/topObjects.json");
    }

    @Test
    public void createExampleInput1() {
        Map<String, Map<String, String>> baseClassInfo = new HashMap<>();
        // org.zlab.ocov.tracker.TestObjectGraph$TargetClassE
        baseClassInfo.put("org.zlab.ocov.tracker.TestObjectGraph$TargetClassE", new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TestObjectGraph$TargetClassE").put("fList",
                "java.util.List");

        // org.zlab.ocov.tracker.TestObjectGraph$TargetClassF1
        baseClassInfo.put("org.zlab.ocov.tracker.TestObjectGraph$TargetClassF1", new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TestObjectGraph$TargetClassF1").put("f1", "int");

        // org.zlab.ocov.tracker.TestObjectGraph$TargetClassF2
        baseClassInfo.put("org.zlab.ocov.tracker.TestObjectGraph$TargetClassF2", new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TestObjectGraph$TargetClassF2").put("f2", "int");
        Utils.saveMapToFile(baseClassInfo, "input/baseClassInfo1.json");

        Set<String> topObjects = new HashSet<>();
        topObjects.add("org.zlab.ocov.tracker.TestObjectGraph$TargetClassE");
        Utils.saveSetToFile(topObjects, "input/topObjects1.json");
    }

    @Test
    public void createExampleInputForMap() {
        Map<String, Map<String, String>> baseClassInfo = new HashMap<>();
        // org.zlab.ocov.tracker.TestObjectGraph$TargetClassE
        baseClassInfo.put("org.zlab.ocov.tracker.TestObjectGraph$TargetClassWithMap",
                new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TestObjectGraph$TargetClassWithMap").put("map",
                "java.util.Map");

        // org.zlab.ocov.tracker.TestObjectGraph$TargetClassF1
        baseClassInfo.put("org.zlab.ocov.tracker.TestObjectGraph$TargetClassF1", new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TestObjectGraph$TargetClassF1").put("f1", "int");

        // org.zlab.ocov.tracker.TestObjectGraph$TargetClassF2
        baseClassInfo.put("org.zlab.ocov.tracker.TestObjectGraph$TargetClassF2", new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TestObjectGraph$TargetClassF2").put("f2", "int");
        Utils.saveMapToFile(baseClassInfo, "input/baseClassInfo2.json");

        Set<String> topObjects = new HashSet<>();
        topObjects.add("org.zlab.ocov.tracker.TestObjectGraph$TargetClassWithMap");
        Utils.saveSetToFile(topObjects, "input/topObjects2.json");
    }

    @Test
    public void createExampleTopObjectsCassandra() {
        Set<String> topObjects = new HashSet<>();
        topObjects.add("org.apache.cassandra.config.CFMetaData");
        topObjects.add("org.apache.cassandra.db.Mutation");
        topObjects.add("org.apache.cassandra.db.AtomicBTreeColumns");

        Utils.saveSetToFile(topObjects, "/tmp/topObjects.json");
    }

    @Test
    public void createExampleInputForEnum() {
        Map<String, Map<String, String>> baseClassInfo = new HashMap<>();
        // org.zlab.ocov.tracker.TestObjectGraph$TargetClassE
        baseClassInfo.put("org.zlab.ocov.tracker.TestObjectGraph$TargetClassForEnum",
                new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TestObjectGraph$TargetClassForEnum").put("e",
                "org.zlab.ocov.tracker.TestObjectGraph$TargetEnum");
        baseClassInfo.get("org.zlab.ocov.tracker.TestObjectGraph$TargetClassForEnum").put("f1",
                "org.zlab.ocov.tracker.TestObjectGraph$TargetClassF1");
        // org.zlab.ocov.tracker.TestObjectGraph$TargetClassF1
        baseClassInfo.put("org.zlab.ocov.tracker.TestObjectGraph$TargetClassF1", new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TestObjectGraph$TargetClassF1").put("f1", "int");
        Utils.saveMapToFile(baseClassInfo, "input/baseClassInfoForEnum.json");

        Set<String> topObjects = new HashSet<>();
        topObjects.add("org.zlab.ocov.tracker.TestObjectGraph$TargetClassForEnum");
        Utils.saveSetToFile(topObjects, "input/topObjectsForEnum.json");
    }

    @Test
    public void createExampleInputForEquality() {
        Map<String, Map<String, String>> baseClassInfo = new HashMap<>();
        // org.zlab.ocov.tracker.TestObjectGraph$TargetClassEquality
        baseClassInfo.put("org.zlab.ocov.tracker.TestObjectGraph$TargetClassEquality",
                new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TestObjectGraph$TargetClassEquality").put(
                "targetClassEqualityA",
                "org.zlab.ocov.tracker.TestObjectGraph$TargetClassEqualityA");
        baseClassInfo.get("org.zlab.ocov.tracker.TestObjectGraph$TargetClassEquality").put(
                "targetClassEqualityC",
                "org.zlab.ocov.tracker.TestObjectGraph$TargetClassEqualityC");

        // org.zlab.ocov.tracker.TestObjectGraph$TargetClassEqualityA
        baseClassInfo.put("org.zlab.ocov.tracker.TestObjectGraph$TargetClassEqualityA",
                new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TestObjectGraph$TargetClassEqualityA").put(
                "targetClassEqualityAA",
                "org.zlab.ocov.tracker.TestObjectGraph$TargetClassEqualityAA");

        // org.zlab.ocov.tracker.TestObjectGraph$TargetClassEqualityAA
        baseClassInfo.put("org.zlab.ocov.tracker.TestObjectGraph$TargetClassEqualityAA",
                new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TestObjectGraph$TargetClassEqualityAA")
                .put("compClass", "org.zlab.ocov.tracker.TestObjectGraph$CompClass");

        // org.zlab.ocov.tracker.TestObjectGraph$TargetClassEqualityC
        baseClassInfo.put("org.zlab.ocov.tracker.TestObjectGraph$TargetClassEqualityC",
                new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TestObjectGraph$TargetClassEqualityC")
                .put("compClass", "org.zlab.ocov.tracker.TestObjectGraph$CompClass");

        // CompClass
        baseClassInfo.put("org.zlab.ocov.tracker.TestObjectGraph$CompClass", new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TestObjectGraph$CompClass").put("a", "int");

        Utils.saveMapToFile(baseClassInfo, "input/baseClassInfoForEquality.json");

        Set<String> topObjects = new HashSet<>();
        topObjects.add("org.zlab.ocov.tracker.TestObjectGraph$TargetClassEquality");
        Utils.saveSetToFile(topObjects, "input/topObjectsForEquality.json");

        Set<String> comparableClasses = new HashSet<>();
        comparableClasses.add("org.zlab.ocov.tracker.TestObjectGraph$CompClass");
        Utils.saveSetToFile(comparableClasses, "input/comparableClassesForEquality.json");
    }

    @Test
    public void createExampleInputForIsSerialized() {
        Map<String, Map<String, String>> baseClassInfo = new HashMap<>();
        // org.zlab.ocov.tracker.TestObjectGraph$TargetClassE
        baseClassInfo.put("org.zlab.ocov.tracker.TestObjectGraph$TargetClassForEnum",
                new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TestObjectGraph$TargetClassForEnum").put("e",
                "org.zlab.ocov.tracker.TestObjectGraph$TargetEnum");
        baseClassInfo.get("org.zlab.ocov.tracker.TestObjectGraph$TargetClassForEnum").put("f1",
                "org.zlab.ocov.tracker.TestObjectGraph$TargetClassF1");
        // org.zlab.ocov.tracker.TestObjectGraph$TargetClassF1
        baseClassInfo.put("org.zlab.ocov.tracker.TestObjectGraph$TargetClassF1", new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TestObjectGraph$TargetClassF1").put("f1", "int");

        // baseClassInfo
        Utils.saveMapToFile(baseClassInfo, "input/baseClassInfoForIsSerialized.json");

        // top objects
        Set<String> topObjects = new HashSet<>();
        topObjects.add("org.zlab.ocov.tracker.TestObjectGraph$TargetClassForEnum");
        Utils.saveSetToFile(topObjects, "input/topObjectsForIsSerialized.json");

        // comparable classes
        Set<String> comparableClasses = new HashSet<>();
        Utils.saveSetToFile(comparableClasses, "input/comparableClassesForIsSerialized.json");

        // modified fields
        Map<String, Set<String>> modifiedFields = new HashMap<>();
        modifiedFields.put("org.zlab.ocov.tracker.TestObjectGraph$TargetClassForEnum",
                new HashSet<>());
        modifiedFields.get("org.zlab.ocov.tracker.TestObjectGraph$TargetClassForEnum").add("f1");
        Utils.saveModifiedFields(modifiedFields, "input/modifiedFieldsForIsSerialized.json");

        // enum
        Set<String> modifiedEnums = new HashSet<>();
        modifiedEnums.add("org.zlab.ocov.tracker.TestObjectGraph$TargetEnum");
        Utils.saveSetToFile(modifiedEnums, "input/modifiedEnumsForIsSerialized.json");
    }

    @Test
    public void createExampleInputForSizeCompute() {
        String baseFilePath = "input/baseClassInfoForSizeCompute.json";
        String topFilePath = "input/topObjectsForSizeCompute.json";
        String comparableFilePath = "input/comparableClassesForSizeCompute.json";

        Map<String, Map<String, String>> baseClassInfo = new HashMap<>();
        // org.zlab.ocov.tracker.TestObjectGraph$TargetClassE
        baseClassInfo.put("org.zlab.ocov.tracker.TestObjectGraph$TargetClassWithSizeBase",
                new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TestObjectGraph$TargetClassWithSizeBase").put("a",
                "org.zlab.ocov.tracker.TestObjectGraph$TargetClassWithSizeA");

        baseClassInfo.put("org.zlab.ocov.tracker.TestObjectGraph$TargetClassWithSizeA",
                new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TestObjectGraph$TargetClassWithSizeA").put("size",
                "int");

        // baseClassInfo
        Utils.saveMapToFile(baseClassInfo, baseFilePath);
        // top objects
        Set<String> topObjects = new HashSet<>();
        topObjects.add("org.zlab.ocov.tracker.TestObjectGraph$TargetClassWithSizeBase");
        Utils.saveSetToFile(topObjects, topFilePath);
        // comparable classes
        Set<String> comparableClasses = new HashSet<>();
        Utils.saveSetToFile(comparableClasses, comparableFilePath);
    }

    // @Test
    public void test() {
        // Example input for baseClassInfo
        ObjectCoverage coverage = new ObjectCoverage(Runtime.baseClassPath, Runtime.topObjectsPath);

        System.out.println("ret1 = "
                + coverage.baseClassInfo.containsKey("org.apache.cassandra.db.AtomicBTreeColumns"));
        System.out.println("ret2 = "
                + coverage.baseClassInfo.get("org.apache.cassandra.db.AtomicBTreeColumns"));
    }

}
