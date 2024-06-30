package org.zlab.ocov.tracker;

import org.junit.jupiter.api.Test;
import org.zlab.ocov.Utils;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TestInputGeneration {
    @Test
    public void createExampleInput() {
        Map<String, Map<String, String>> baseClassInfo = new HashMap<>();
        // org.zlab.ocov.tracker.TargetClass$TargetClassA
        baseClassInfo.put("org.zlab.ocov.tracker.TargetClass$TargetClassA", new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TargetClass$TargetClassA").put("a", "int");
        baseClassInfo.get("org.zlab.ocov.tracker.TargetClass$TargetClassA").put("b", "int");
        baseClassInfo.get("org.zlab.ocov.tracker.TargetClass$TargetClassA").put("bObj",
                "org.zlab.ocov.tracker.TargetClass$TargetClassB");

        // org.zlab.ocov.tracker.TargetClass$TargetClassB
        baseClassInfo.put("org.zlab.ocov.tracker.TargetClass$TargetClassB", new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TargetClass$TargetClassB").put("i", "int");
        baseClassInfo.get("org.zlab.ocov.tracker.TargetClass$TargetClassB").put("ids",
                "java.util.List");

        // org.zlab.ocov.tracker.TargetClass$TargetClassC
        baseClassInfo.put("org.zlab.ocov.tracker.TargetClass$TargetClassC", new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TargetClass$TargetClassC").put("c", "int");

        // org.zlab.ocov.tracker.TargetClass$TargetClassD
        baseClassInfo.put("org.zlab.ocov.tracker.TargetClass$TargetClassD", new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TargetClass$TargetClassD").put("a", "int");
        baseClassInfo.get("org.zlab.ocov.tracker.TargetClass$TargetClassD").put("bObj",
                "org.zlab.ocov.tracker.TargetClass$TargetClassB");
        baseClassInfo.get("org.zlab.ocov.tracker.TargetClass$TargetClassD").put("dObj",
                "org.zlab.ocov.tracker.TargetClass$TargetClassD");
        Utils.saveMapToFile(baseClassInfo, "input/baseClassInfo.json");

        Set<String> topObjects = new HashSet<>();
        topObjects.add("org.zlab.ocov.tracker.TargetClass$TargetClassA");
        topObjects.add("org.zlab.ocov.tracker.TargetClass$TargetClassD");
        Utils.saveSetToFile(topObjects, "input/topObjects.json");
    }

    @Test
    public void createExampleInput1() {
        Map<String, Map<String, String>> baseClassInfo = new HashMap<>();
        // org.zlab.ocov.tracker.TargetClass$TargetClassE
        baseClassInfo.put("org.zlab.ocov.tracker.TargetClass$TargetClassE", new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TargetClass$TargetClassE").put("fList",
                "java.util.List");

        // org.zlab.ocov.tracker.TargetClass$TargetClassF1
        baseClassInfo.put("org.zlab.ocov.tracker.TargetClass$TargetClassF1", new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TargetClass$TargetClassF1").put("f1", "int");

        // org.zlab.ocov.tracker.TargetClass$TargetClassF2
        baseClassInfo.put("org.zlab.ocov.tracker.TargetClass$TargetClassF2", new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TargetClass$TargetClassF2").put("f2", "int");
        Utils.saveMapToFile(baseClassInfo, "input/baseClassInfo1.json");

        Set<String> topObjects = new HashSet<>();
        topObjects.add("org.zlab.ocov.tracker.TargetClass$TargetClassE");
        Utils.saveSetToFile(topObjects, "input/topObjects1.json");
    }

    @Test
    public void createExampleInputForMap() {
        Map<String, Map<String, String>> baseClassInfo = new HashMap<>();
        // org.zlab.ocov.tracker.TargetClass$TargetClassE
        baseClassInfo.put("org.zlab.ocov.tracker.TargetClass$TargetClassWithMap", new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TargetClass$TargetClassWithMap").put("map",
                "java.util.Map");

        // org.zlab.ocov.tracker.TargetClass$TargetClassF1
        baseClassInfo.put("org.zlab.ocov.tracker.TargetClass$TargetClassF1", new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TargetClass$TargetClassF1").put("f1", "int");

        // org.zlab.ocov.tracker.TargetClass$TargetClassF2
        baseClassInfo.put("org.zlab.ocov.tracker.TargetClass$TargetClassF2", new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TargetClass$TargetClassF2").put("f2", "int");
        Utils.saveMapToFile(baseClassInfo, "input/baseClassInfo2.json");

        Set<String> topObjects = new HashSet<>();
        topObjects.add("org.zlab.ocov.tracker.TargetClass$TargetClassWithMap");
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
        // org.zlab.ocov.tracker.TargetClass$TargetClassE
        baseClassInfo.put("org.zlab.ocov.tracker.TargetClass$TargetClassForEnum", new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TargetClass$TargetClassForEnum").put("e",
                "org.zlab.ocov.tracker.TargetClass$TargetEnum");
        baseClassInfo.get("org.zlab.ocov.tracker.TargetClass$TargetClassForEnum").put("f1",
                "org.zlab.ocov.tracker.TargetClass$TargetClassF1");
        // org.zlab.ocov.tracker.TargetClass$TargetClassF1
        baseClassInfo.put("org.zlab.ocov.tracker.TargetClass$TargetClassF1", new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TargetClass$TargetClassF1").put("f1", "int");
        Utils.saveMapToFile(baseClassInfo, "input/baseClassInfoForEnum.json");

        Set<String> topObjects = new HashSet<>();
        topObjects.add("org.zlab.ocov.tracker.TargetClass$TargetClassForEnum");
        Utils.saveSetToFile(topObjects, "input/topObjectsForEnum.json");
    }

    @Test
    public void createExampleInputForEquality() {
        TestHelper helper = new TestHelper("Equality");
        String classPrefix = "org.zlab.ocov.tracker.TargetClass$";

        helper.addBaseClassInfo(classPrefix + "TargetClassEquality", "targetClassEqualityA",
                classPrefix + "TargetClassEqualityA");
        helper.addBaseClassInfo(classPrefix + "TargetClassEquality", "targetClassEqualityC",
                classPrefix + "TargetClassEqualityC");

        helper.addBaseClassInfo(classPrefix + "TargetClassEqualityA", "targetClassEqualityAA",
                classPrefix + "TargetClassEqualityAA");
        helper.addBaseClassInfo(classPrefix + "TargetClassEqualityA", "staticComp",
                classPrefix + "CompClass");
        helper.addBaseClassInfo(classPrefix + "TargetClassEqualityAA", "compClass",
                classPrefix + "CompClass");
        helper.addBaseClassInfo(classPrefix + "TargetClassEqualityC", "compClass",
                classPrefix + "CompClass");

        helper.addBaseClassInfo(classPrefix + "CompClass", "a", "int");

        helper.addTopObject(classPrefix + "TargetClassEquality");
        helper.addTopObject(classPrefix + "TargetClassEqualityA");
        helper.addComparableClass(classPrefix + "CompClass");

        helper.save();
    }

    @Test
    public void createExampleInputForIsSerialized() {
        Map<String, Map<String, String>> baseClassInfo = new HashMap<>();
        // org.zlab.ocov.tracker.TargetClass$TargetClassE
        baseClassInfo.put("org.zlab.ocov.tracker.TargetClass$TargetClassForEnum", new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TargetClass$TargetClassForEnum").put("e",
                "org.zlab.ocov.tracker.TargetClass$TargetEnum");
        baseClassInfo.get("org.zlab.ocov.tracker.TargetClass$TargetClassForEnum").put("f1",
                "org.zlab.ocov.tracker.TargetClass$TargetClassF1");
        // org.zlab.ocov.tracker.TargetClass$TargetClassF1
        baseClassInfo.put("org.zlab.ocov.tracker.TargetClass$TargetClassF1", new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TargetClass$TargetClassF1").put("f1", "int");

        // baseClassInfo
        Utils.saveMapToFile(baseClassInfo, "input/baseClassInfoForIsSerialized.json");

        // top objects
        Set<String> topObjects = new HashSet<>();
        topObjects.add("org.zlab.ocov.tracker.TargetClass$TargetClassForEnum");
        Utils.saveSetToFile(topObjects, "input/topObjectsForIsSerialized.json");

        // comparable classes
        Set<String> comparableClasses = new HashSet<>();
        Utils.saveSetToFile(comparableClasses, "input/comparableClassesForIsSerialized.json");

        // modified fields
        Map<String, Set<String>> modifiedFields = new HashMap<>();
        modifiedFields.put("org.zlab.ocov.tracker.TargetClass$TargetClassForEnum", new HashSet<>());
        modifiedFields.get("org.zlab.ocov.tracker.TargetClass$TargetClassForEnum").add("f1");
        Utils.saveModifiedFields(modifiedFields, "input/modifiedFieldsForIsSerialized.json");

        // enum
        Set<String> modifiedEnums = new HashSet<>();
        modifiedEnums.add("org.zlab.ocov.tracker.TargetClass$TargetEnum");
        Utils.saveSetToFile(modifiedEnums, "input/modifiedEnumsForIsSerialized.json");
    }

    @Test
    public void createExampleInputForSizeCompute() {
        String baseFilePath = "input/baseClassInfoForSizeCompute.json";
        String topFilePath = "input/topObjectsForSizeCompute.json";
        String comparableFilePath = "input/comparableClassesForSizeCompute.json";

        Map<String, Map<String, String>> baseClassInfo = new HashMap<>();
        // org.zlab.ocov.tracker.TargetClass$TargetClassE
        baseClassInfo.put("org.zlab.ocov.tracker.TargetClass$TargetClassWithSizeBase",
                new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TargetClass$TargetClassWithSizeBase").put("a",
                "org.zlab.ocov.tracker.TargetClass$TargetClassWithSizeA");

        baseClassInfo.put("org.zlab.ocov.tracker.TargetClass$TargetClassWithSizeA",
                new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TargetClass$TargetClassWithSizeA").put("size",
                "int");

        // baseClassInfo
        Utils.saveMapToFile(baseClassInfo, baseFilePath);
        // top objects
        Set<String> topObjects = new HashSet<>();
        topObjects.add("org.zlab.ocov.tracker.TargetClass$TargetClassWithSizeBase");
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
        baseClassInfo.put("org.zlab.ocov.tracker.TargetClass$TargetClassAccumulateSizeBase",
                new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TargetClass$TargetClassAccumulateSizeBase")
                .put("a", "org.zlab.ocov.tracker.TargetClass$TargetClassAccumulateSizeA");

        baseClassInfo.put("org.zlab.ocov.tracker.TargetClass$TargetClassAccumulateSizeA",
                new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TargetClass$TargetClassAccumulateSizeA").put("ids",
                "java.util.List");

        baseClassInfo.put("org.zlab.ocov.tracker.TargetClass$TargetClassAccumulateSizeB",
                new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TargetClass$TargetClassAccumulateSizeB")
                .put("value", "java.lang.Integer");

        // baseClassInfo
        Utils.saveMapToFile(baseClassInfo, baseFilePath);
        // top objects
        Set<String> topObjects = new HashSet<>();
        topObjects.add("org.zlab.ocov.tracker.TargetClass$TargetClassAccumulateSizeBase");
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
        baseClassInfo.put("org.zlab.ocov.tracker.TargetClass$TargetClassInvCombinationBase",
                new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TargetClass$TargetClassInvCombinationBase")
                .put("a", "org.zlab.ocov.tracker.TargetClass$TargetClassInvCombinationA");
        baseClassInfo.get("org.zlab.ocov.tracker.TargetClass$TargetClassInvCombinationBase")
                .put("b", "org.zlab.ocov.tracker.TargetClass$TargetClassInvCombinationB");

        baseClassInfo.put("org.zlab.ocov.tracker.TargetClass$TargetClassInvCombinationA",
                new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TargetClass$TargetClassInvCombinationA")
                .put("value", "java.lang.Integer");
        baseClassInfo.get("org.zlab.ocov.tracker.TargetClass$TargetClassInvCombinationA")
                .put("compClass", "org.zlab.ocov.tracker.TargetClass$CompClass");

        baseClassInfo.put("org.zlab.ocov.tracker.TargetClass$TargetClassInvCombinationB",
                new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TargetClass$TargetClassInvCombinationB")
                .put("value", "java.lang.Integer");
        baseClassInfo.get("org.zlab.ocov.tracker.TargetClass$TargetClassInvCombinationB")
                .put("compClass", "org.zlab.ocov.tracker.TargetClass$CompClass");

        // CompClass
        baseClassInfo.put("org.zlab.ocov.tracker.TargetClass$CompClass", new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.tracker.TargetClass$CompClass").put("a", "int");

        // baseClassInfo
        Utils.saveMapToFile(baseClassInfo, baseFilePath);
        // top objects
        Set<String> topObjects = new HashSet<>();
        topObjects.add("org.zlab.ocov.tracker.TargetClass$TargetClassInvCombinationBase");
        Utils.saveSetToFile(topObjects, topFilePath);
        // comparable classes
        Set<String> comparableClasses = new HashSet<>();
        comparableClasses.add("org.zlab.ocov.tracker.TargetClass$CompClass");
        Utils.saveSetToFile(comparableClasses, comparableFilePath);
    }

    @Test
    public void createExampleInputForMultiEqual() {
        TestHelper helper = new TestHelper("MultiEqual");
        String classPrefix = "org.zlab.ocov.tracker.TargetClass$";

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
        String classPrefix = "org.zlab.ocov.tracker.TargetClass$";

        helper.addBaseClassInfo(classPrefix + "TargetClassE1", "fList", "java.util.List");
        helper.addBaseClassInfo(classPrefix + "TargetClassE2", "compClass",
                classPrefix + "CompClass");

        helper.addTopObject(classPrefix + "TargetClassE1");
        helper.addTopObject(classPrefix + "TargetClassE2");

        helper.addComparableClass(classPrefix + "CompClass");
        helper.save();
    }

    @Test
    public void createInputForPreservedString() {
        TestHelper helper = new TestHelper("PreservedString");
        String classPrefix = "org.zlab.ocov.tracker.TargetClass$";

        helper.addBaseClassInfo(classPrefix + "TargetClassForPreservedString", "ksname",
                "java.lang.String");
        helper.addBaseClassInfo(classPrefix + "TargetClassForPreservedStringTopObject", "fList",
                "java.util.List");

        helper.addTopObject(classPrefix + "TargetClassForPreservedStringTopObject");

        helper.save();
    }

    @Test
    public void createInputForStacktrace() {
        TestHelper helper = new TestHelper("StackTrace");
        String classPrefix = "org.zlab.ocov.tracker.TargetClass$";

        helper.addBaseClassInfo(classPrefix + "TargetClassForStacktrace", "fList",
                "java.util.List");

        helper.addTopObject(classPrefix + "TargetClassForStacktrace");
        helper.save();
    }

    @Test
    public void createInputForLinkedType() {
        TestHelper helper = new TestHelper("LinkedType");
        String classPrefix = "org.zlab.ocov.tracker.TargetClass$";

        helper.addBaseClassInfo(classPrefix + "TargetClassForLinkedType", "next",
                classPrefix + "TargetClassForLinkedType");
        helper.addBaseClassInfo(classPrefix + "TargetClassForLinkedType", "a", "int");

        helper.addTopObject(classPrefix + "TargetClassForLinkedType");
        helper.save();
    }

    @Test
    public void createInputForLinkedType1() {
        TestHelper helper = new TestHelper("LinkedType1");
        String classPrefix = "org.zlab.ocov.tracker.TargetClass$";

        helper.addBaseClassInfo(classPrefix + "TargetClassForLinkedType1", "children",
                "java.util.List");
        helper.addBaseClassInfo(classPrefix + "TargetClassForLinkedType1", "a", "int");

        helper.addTopObject(classPrefix + "TargetClassForLinkedType1");
        helper.save();
    }

    @Test
    public void createInputForLinkedType2() {
        TestHelper helper = new TestHelper("LinkedType2");
        String classPrefix = "org.zlab.ocov.tracker.TargetClass$";

        helper.addBaseClassInfo(classPrefix + "TargetClassForLinkedType2", "children",
                "org.zlab.ocov.tracker.TargetClass$TargetClassForLinkedType2[]");
        helper.addBaseClassInfo(classPrefix + "TargetClassForLinkedType2", "a", "int");

        helper.addTopObject(classPrefix + "TargetClassForLinkedType2");
        helper.save();
    }

    @Test
    public void createInputForLinkedType3() {
        TestHelper helper = new TestHelper("LinkedType3");
        String classPrefix = "org.zlab.ocov.tracker.TargetClass$";

        helper.addBaseClassInfo(classPrefix + "TargetClassForLinkedType3", "next",
                classPrefix + "TargetClassForLinkedType3$TargetClassForLinkedType4");
        helper.addBaseClassInfo(classPrefix + "TargetClassForLinkedType3", "a", "int");

        helper.addBaseClassInfo(classPrefix + "TargetClassForLinkedType3$TargetClassForLinkedType4",
                "a", "int");

        helper.addTopObject(classPrefix + "TargetClassForLinkedType3");
        helper.save();
    }

    @Test
    public void createExampleInputForModifiedTypeHierarchy() {
        TestHelper helper = new TestHelper("ModifiedTypeHierarchy");
        String classPrefix = "org.zlab.ocov.tracker.TargetClass$";

        helper.addBaseClassInfo(classPrefix + "TargetClassF1", "f1", "int");

        helper.addBaseClassInfo(classPrefix + "TargetClassForLinkedType3", "a", "int");

        helper.addBaseClassInfo(classPrefix + "TargetClassForLinkedType3$TargetClassForLinkedType4",
                "a", "int");

        helper.addModifiedTypeHierarchy(classPrefix + "TargetClassF1");

        helper.addTopObject(classPrefix + "TargetClassF1");

        helper.save();
    }

    public static class TestHelper {
        public Map<String, Map<String, String>> baseClassInfo = new HashMap<>();
        public Set<String> topObjects = new HashSet<>();
        public Set<String> comparableClasses = new HashSet<>();

        public Map<String, Set<String>> modifiedFields = new HashMap<>();
        public Set<String> modifiedEnums = new HashSet<>();
        public Set<String> modifiedTypeHierarchy = new HashSet<>();

        public String bassClassPath;
        public String topObjectsPath;
        public String comparableClassesPath;
        public String modifiedFieldsPath;
        public String modifiedEnumsPath;
        public String modifiedTypeHierarchyPath;

        public TestHelper(String suffix) {
            bassClassPath = String.format("input/baseClassInfoFor%s.json", suffix);
            topObjectsPath = String.format("input/topObjectsFor%s.json", suffix);
            comparableClassesPath = String.format("input/comparableClassesFor%s.json", suffix);
            modifiedFieldsPath = String.format("input/modifiedFieldsFor%s.json", suffix);
            modifiedEnumsPath = String.format("input/modifiedEnumsFor%s.json", suffix);
            modifiedTypeHierarchyPath = String.format("input/modifiedTypeHierarchyFor%s.json",
                    suffix);
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

        public void addModifiedField(String className, String fieldName) {
            if (!modifiedFields.containsKey(className)) {
                modifiedFields.put(className, new HashSet<>());
            }
            modifiedFields.get(className).add(fieldName);
        }

        public void addModifiedEnum(String enumName) {
            modifiedEnums.add(enumName);
        }

        public void addModifiedTypeHierarchy(String className) {
            modifiedTypeHierarchy.add(className);
        }

        public void save() {
            Utils.saveMapToFile(baseClassInfo, bassClassPath);
            Utils.saveSetToFile(topObjects, topObjectsPath);
            Utils.saveSetToFile(comparableClasses, comparableClassesPath);
            Utils.saveModifiedFields(modifiedFields, modifiedFieldsPath);
            Utils.saveSetToFile(modifiedEnums, modifiedEnumsPath);
            Utils.saveSetToFile(modifiedTypeHierarchy, modifiedTypeHierarchyPath);
        }
    }
}
