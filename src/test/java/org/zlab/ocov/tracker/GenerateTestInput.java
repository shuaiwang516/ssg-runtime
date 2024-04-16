package org.zlab.ocov.tracker;

import org.junit.jupiter.api.Test;
import org.zlab.ocov.Utils;

import java.nio.file.Paths;
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

    // Do not run this test
    // @Test
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

    @Test
    public void createExampleInputForAccumulatedSize() {
        String baseFilePath = "input/baseClassInfoForAccumulatedSize.json";
        String topFilePath = "input/topObjectsForAccumulatedSize.json";
        String comparableFilePath = "input/comparableClassesForAccumulatedSize.json";

        Map<String, Map<String, String>> baseClassInfo = new HashMap<>();
        baseClassInfo.put("org.zlab.ocov.tracker.TestObjectGraph$TargetClassAccumulateSizeBase",
                new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TestObjectGraph$TargetClassAccumulateSizeBase")
                .put("a", "org.zlab.ocov.tracker.TestObjectGraph$TargetClassAccumulateSizeA");

        baseClassInfo.put("org.zlab.ocov.tracker.TestObjectGraph$TargetClassAccumulateSizeA",
                new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TestObjectGraph$TargetClassAccumulateSizeA")
                .put("ids", "java.util.List");

        baseClassInfo.put("org.zlab.ocov.tracker.TestObjectGraph$TargetClassAccumulateSizeB",
                new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TestObjectGraph$TargetClassAccumulateSizeB")
                .put("value", "java.lang.Integer");

        // baseClassInfo
        Utils.saveMapToFile(baseClassInfo, baseFilePath);
        // top objects
        Set<String> topObjects = new HashSet<>();
        topObjects.add("org.zlab.ocov.tracker.TestObjectGraph$TargetClassAccumulateSizeBase");
        Utils.saveSetToFile(topObjects, topFilePath);
        // comparable classes
        Set<String> comparableClasses = new HashSet<>();
        Utils.saveSetToFile(comparableClasses, comparableFilePath);
    }

    @Test
    public void createExampleInputForInvCombination() {
        String baseFilePath = "input/baseClassInfoForInvCombination.json";
        String topFilePath = "input/topObjectsForInvCombination.json";
        String comparableFilePath = "input/comparableClassesForInvCombination.json";

        Map<String, Map<String, String>> baseClassInfo = new HashMap<>();
        baseClassInfo.put("org.zlab.ocov.tracker.TestObjectGraph$TargetClassInvCombinationBase",
                new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TestObjectGraph$TargetClassInvCombinationBase")
                .put("a", "org.zlab.ocov.tracker.TestObjectGraph$TargetClassInvCombinationA");
        baseClassInfo.get("org.zlab.ocov.tracker.TestObjectGraph$TargetClassInvCombinationBase")
                .put("b", "org.zlab.ocov.tracker.TestObjectGraph$TargetClassInvCombinationB");

        baseClassInfo.put("org.zlab.ocov.tracker.TestObjectGraph$TargetClassInvCombinationA",
                new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TestObjectGraph$TargetClassInvCombinationA")
                .put("value", "java.lang.Integer");
        baseClassInfo.get("org.zlab.ocov.tracker.TestObjectGraph$TargetClassInvCombinationA")
                .put("compClass", "org.zlab.ocov.tracker.TestObjectGraph$CompClass");

        baseClassInfo.put("org.zlab.ocov.tracker.TestObjectGraph$TargetClassInvCombinationB",
                new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TestObjectGraph$TargetClassInvCombinationB")
                .put("value", "java.lang.Integer");
        baseClassInfo.get("org.zlab.ocov.tracker.TestObjectGraph$TargetClassInvCombinationB")
                .put("compClass", "org.zlab.ocov.tracker.TestObjectGraph$CompClass");

        // CompClass
        baseClassInfo.put("org.zlab.ocov.tracker.TestObjectGraph$CompClass", new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TestObjectGraph$CompClass").put("a", "int");

        // baseClassInfo
        Utils.saveMapToFile(baseClassInfo, baseFilePath);
        // top objects
        Set<String> topObjects = new HashSet<>();
        topObjects.add("org.zlab.ocov.tracker.TestObjectGraph$TargetClassInvCombinationBase");
        Utils.saveSetToFile(topObjects, topFilePath);
        // comparable classes
        Set<String> comparableClasses = new HashSet<>();
        comparableClasses.add("org.zlab.ocov.tracker.TestObjectGraph$CompClass");
        Utils.saveSetToFile(comparableClasses, comparableFilePath);
    }

    @Test
    public void createExampleInputForMultiEqual() {
        TestHelper helper = new TestHelper("MultiEqual");
        String classPrefix = "org.zlab.ocov.tracker.TestObjectGraph$";

        helper.addBaseClassInfo(classPrefix + "TargetClassMultiEqualBase", "a",
                classPrefix + "TargetClassMultiEqualA");
        helper.addBaseClassInfo(classPrefix + "TargetClassMultiEqualBase", "b",
                classPrefix + "TargetClassMultiEqualB");

        helper.addBaseClassInfo(classPrefix + "TargetClassMultiEqualBase1", "a",
                classPrefix + "TargetClassMultiEqualA");
        helper.addBaseClassInfo(classPrefix + "TargetClassMultiEqualBase1", "b",
                classPrefix + "TargetClassMultiEqualB");

        helper.addBaseClassInfo(classPrefix + "TargetClassMultiEqualA", "comp",
                classPrefix + "CompClass");
        helper.addBaseClassInfo(classPrefix + "TargetClassMultiEqualB", "comp1",
                classPrefix + "CompClass1");

        helper.addBaseClassInfo(classPrefix + "CompClass", "a", "int");
        helper.addBaseClassInfo(classPrefix + "CompClass1", "a", "int");

        helper.addTopObject(classPrefix + "TargetClassMultiEqualBase");
        helper.addTopObject(classPrefix + "TargetClassMultiEqualBase1");

        helper.addComparableClass(classPrefix + "CompClass");
        helper.addComparableClass(classPrefix + "CompClass1");

        helper.save();
    }

    @Test
    public void createInputForBoundary() {
        String suffix = "Boundary";
        TestHelper helper = new TestHelper(suffix);

        Map<Integer, Set<Integer>> branch2Collection = new HashMap<>();
        Set<Integer> set1 = branch2Collection.computeIfAbsent(1, k -> new HashSet<>());
        set1.add(1);
        set1.add(2);
        Set<Integer> set2 = branch2Collection.computeIfAbsent(2, k -> new HashSet<>());
        set2.add(9);
        Utils.saveBranch2Collection(branch2Collection,
                Paths.get("input/branch2CollectionForBoundary.json"));

        helper.save();
    }

    @Test
    public void createInputForCollectionFirstLast() {
        TestHelper helper = new TestHelper("CollectionFirstLast");
        String classPrefix = "org.zlab.ocov.tracker.TestObjectGraph$";

        helper.addBaseClassInfo(classPrefix + "TargetClassE1", "fList", "java.util.List");
        helper.addBaseClassInfo(classPrefix + "TargetClassE2", "compClass",
                classPrefix + "CompClass");

        helper.addTopObject(classPrefix + "TargetClassE1");
        helper.addTopObject(classPrefix + "TargetClassE2");

        helper.addComparableClass(classPrefix + "CompClass");
        helper.save();
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

    public static class TestHelper {
        public Map<String, Map<String, String>> baseClassInfo = new HashMap<>();
        public Set<String> topObjects = new HashSet<>();
        public Set<String> comparableClasses = new HashSet<>();

        public String bassClassPath;
        public String topObjectsPath;
        public String comparableClassesPath;

        public TestHelper(String suffix) {
            bassClassPath = String.format("input/baseClassInfoFor%s.json", suffix);
            topObjectsPath = String.format("input/topObjectsFor%s.json", suffix);
            comparableClassesPath = String.format("input/comparableClassesFor%s.json", suffix);
        }

        public void addBaseClassInfo(String className, String fieldName, String fieldType) {
            if (!baseClassInfo.containsKey(className)) {
                baseClassInfo.put(className, new HashMap<>());
            }
            baseClassInfo.get(className).put(fieldName, fieldType);
        }

        public void addTopObject(String className) {
            topObjects.add(className);
        }

        public void addComparableClass(String className) {
            comparableClasses.add(className);
        }

        public void save() {
            Utils.saveMapToFile(baseClassInfo, bassClassPath);
            Utils.saveSetToFile(topObjects, topObjectsPath);
            Utils.saveSetToFile(comparableClasses, comparableClassesPath);
        }
    }

}
