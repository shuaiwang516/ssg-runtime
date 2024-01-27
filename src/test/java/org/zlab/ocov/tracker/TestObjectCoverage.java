package org.zlab.ocov.tracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zlab.ocov.tracker.type.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestObjectCoverage {

    @BeforeAll
    public static void init() {
        Runtime.initWriter();
    }

    @Test
    public void testBasic() {
        /**
         * Test condition: if there are 2 differences, both of them have a field with
         * classC. We want to track them differently since they might manifest
         * differently.
         */
        // obj1 and obj2 share same format, obj3 is different
        Path bassClassPath = Paths.get("input/baseClassInfo.json");
        Path topObjectsPath = Paths.get("input/topObjects.json");

        ObjectCoverage coverage = new ObjectCoverage(bassClassPath, topObjectsPath);
        TestObjectGraph.TargetClassA obj1 = new TestObjectGraph.TargetClassA();
        assert (coverage.update(obj1));

        TestObjectGraph.TargetClassA obj2 = new TestObjectGraph.TargetClassA();
        assert (!coverage.update(obj2));

        TestObjectGraph.TargetClassA obj3 = new TestObjectGraph.TargetClassA();
        obj3.a = -1;
        assert (coverage.update(obj3));

        TestObjectGraph.TargetClassA obj4 = new TestObjectGraph.TargetClassA();
        obj4.c = -1;
        assert (!coverage.update(obj4));
    }

    @Test
    public void testSubObject() {
        // obj1 and obj2 share same format, obj3 is different
        Path bassClassPath = Paths.get("input/baseClassInfo.json");
        Path topObjectsPath = Paths.get("input/topObjects.json");

        ObjectCoverage coverage = new ObjectCoverage(bassClassPath, topObjectsPath);
        ObjectCoverage coverage1 = new ObjectCoverage(bassClassPath, topObjectsPath);

        TestObjectGraph.TargetClassA obj1 = new TestObjectGraph.TargetClassA();
        assert (coverage.update(obj1));
        assert coverage1.merge(coverage);

        TestObjectGraph.TargetClassA obj2 = new TestObjectGraph.TargetClassA();
        assert (!coverage.update(obj2));
        assert !coverage1.merge(coverage);

        TestObjectGraph.TargetClassA obj3 = new TestObjectGraph.TargetClassA();
        obj3.bObj.i = 1;
        assert (coverage.update(obj3));
        assert coverage1.merge(coverage);

        TestObjectGraph.TargetClassD obj4 = new TestObjectGraph.TargetClassD();
        assert (coverage.update(obj4));
        assert coverage1.merge(coverage);

        TestObjectGraph.TargetClassD obj5 = new TestObjectGraph.TargetClassD();
        obj5.bObj.i = 1;
        assert (coverage.update(obj5));
        assert coverage1.merge(coverage);
    }

    @Test
    public void testCollectionCoverage() {
        /**
         * Test condition: if there are 2 differences, both of them have a field with
         * classC. We want to track them differently since they might manifest
         * differently.
         */
        // obj1 and obj2 share same format, obj3 is different

        Path bassClassPath = Paths.get("input/baseClassInfo.json");
        Path topObjectsPath = Paths.get("input/topObjects.json");

        ObjectCoverage coverage = new ObjectCoverage(bassClassPath, topObjectsPath);
        TestObjectGraph.TargetClassA obj1 = new TestObjectGraph.TargetClassA();
        assert (coverage.update(obj1));

        TestObjectGraph.TargetClassA obj2 = new TestObjectGraph.TargetClassA();
        assert (!coverage.update(obj2));

        TestObjectGraph.TargetClassA obj3 = new TestObjectGraph.TargetClassA();
        obj3.bObj.ids.clear();
        assert (coverage.update(obj3));

        TestObjectGraph.TargetClassA obj4 = new TestObjectGraph.TargetClassA();
        obj4.bObj.ids.add(2);
        // assert (coverage.update(obj4));
        // assert (!coverage.update(obj4));
    }

    @Test
    public void testCoverageMerge() {
        Path bassClassPath = Paths.get("input/baseClassInfo.json");
        Path topObjectsPath = Paths.get("input/topObjects.json");
        ObjectCoverage coverage1 = new ObjectCoverage(bassClassPath, topObjectsPath);
        TestObjectGraph.TargetClassD obj1 = new TestObjectGraph.TargetClassD();
        assert (coverage1.update(obj1));

        ObjectCoverage coverage2 = new ObjectCoverage(bassClassPath, topObjectsPath);

        TestObjectGraph.TargetClassD obj2 = new TestObjectGraph.TargetClassD();
        obj2.a = 0;
        assert (coverage2.update(obj2));
        assert (coverage1.merge(coverage2));
    }

    @Test
    public void testCollectionItem1() {
        /**
         * Suppose a collection will be serialized, the size of it is always2. However,
         * its object type is changed, we should also capture it and report a new format
         * coverage.
         */
        Path bassClassPath = Paths.get("input/baseClassInfo1.json");
        Path topObjectsPath = Paths.get("input/topObjects1.json");

        ObjectCoverage coverage = new ObjectCoverage(bassClassPath, topObjectsPath);
        TestObjectGraph.TargetClassE obj1 = new TestObjectGraph.TargetClassE();
        assert (coverage.update(obj1));

        TestObjectGraph.TargetClassE obj2 = new TestObjectGraph.TargetClassE();
        obj2.fList.add(new TestObjectGraph.TargetClassF1());
        assert (coverage.update(obj2));

        TestObjectGraph.TargetClassE obj3 = new TestObjectGraph.TargetClassE();
        obj3.fList.add(new TestObjectGraph.TargetClassF2());
        assert (coverage.update(obj3));
        assert (!coverage.update(obj3));

        TestObjectGraph.TargetClassE obj4 = new TestObjectGraph.TargetClassE();
        obj4.fList.add(new TestObjectGraph.TargetClassF2());
        obj4.fList.add(new TestObjectGraph.TargetClassF2());
        assert (coverage.update(obj4));

        TestObjectGraph.TargetClassE obj5 = new TestObjectGraph.TargetClassE();
        TestObjectGraph.TargetClassF1 f1 = new TestObjectGraph.TargetClassF1();
        obj5.fList.add(f1);
        assert (!coverage.update(obj5));

        TestObjectGraph.TargetClassE obj6 = new TestObjectGraph.TargetClassE();
        TestObjectGraph.TargetClassF1 f2 = new TestObjectGraph.TargetClassF1();
        obj6.fList.add(f2);
        f2.f1 = 0;
        assert (coverage.update(obj6));
        assert (!coverage.update(obj6));

        ObjectCoverage coverage1 = new ObjectCoverage(bassClassPath, topObjectsPath);
        assert coverage1.merge(coverage);
        assert !coverage.merge(coverage1);
    }

    @Test
    public void testCollectionItem2() {
        /**
         * Suppose a collection will be serialized, the size of it is always2. However,
         * its object type is changed, we should also capture it and report a new format
         * coverage.
         */
        Path bassClassPath = Paths.get("input/baseClassInfo1.json");
        Path topObjectsPath = Paths.get("input/topObjects1.json");

        ObjectCoverage coverage = new ObjectCoverage(bassClassPath, topObjectsPath);

        TestObjectGraph.TargetClassE obj2 = new TestObjectGraph.TargetClassE();
        obj2.fList.add(new TestObjectGraph.TargetClassF1());
        assert (coverage.update(obj2));

        TestObjectGraph.TargetClassE obj3 = new TestObjectGraph.TargetClassE();
        obj3.fList.add(new TestObjectGraph.TargetClassF2());
        assert (coverage.update(obj3));

        ObjectCoverage coverage1 = new ObjectCoverage(bassClassPath, topObjectsPath);
        assert coverage1.merge(coverage);
        assert !coverage.merge(coverage1);
    }

    @Test
    public void testCollectionRecursive() {
        /**
         * Suppose a collection will be serialized, it always contains a single object.
         * If this object's field contains a new format, it should also report a new
         * format coverage. E -> [tmpF1] tmpF1 -> f1
         */
        Path bassClassPath = Paths.get("input/baseClassInfo1.json");
        Path topObjectsPath = Paths.get("input/topObjects1.json");

        ObjectCoverage coverage = new ObjectCoverage(bassClassPath, topObjectsPath);
        TestObjectGraph.TargetClassE obj2 = new TestObjectGraph.TargetClassE();
        obj2.fList.add(new TestObjectGraph.TargetClassF1());
        assert (coverage.update(obj2));

        ObjectCoverage coverage1 = new ObjectCoverage(bassClassPath, topObjectsPath);
        TestObjectGraph.TargetClassE obj3 = new TestObjectGraph.TargetClassE();
        TestObjectGraph.TargetClassF1 tmpF1 = new TestObjectGraph.TargetClassF1();
        tmpF1.f1 = 0;
        obj3.fList.add(tmpF1);
        assert (coverage1.update(obj3));

        assert coverage.merge(coverage1);
    }

    @Test
    public void testMap() {
        /**
         * Suppose a collection will be serialized, the size of it is always2. However,
         * its object type is changed, we should also capture it and report a new format
         * coverage.
         */
        Path bassClassPath = Paths.get("input/baseClassInfo2.json");
        Path topObjectsPath = Paths.get("input/topObjects2.json");

        ObjectCoverage coverage = new ObjectCoverage(bassClassPath, topObjectsPath);
        TestObjectGraph.TargetClassWithMap obj1 = new TestObjectGraph.TargetClassWithMap();
        assert (coverage.update(obj1));

        TestObjectGraph.TargetClassWithMap obj2 = new TestObjectGraph.TargetClassWithMap();
        assert (!coverage.update(obj2));

        TestObjectGraph.TargetClassWithMap obj3 = new TestObjectGraph.TargetClassWithMap();
        obj3.map.put(1, new TestObjectGraph.TargetClassF1());
        assert (coverage.update(obj3));

        ObjectCoverage coverage1 = new ObjectCoverage(bassClassPath, topObjectsPath);
        assert coverage1.merge(coverage);
        assert !coverage1.merge(coverage);
    }

    @Test
    public void testEnum() {
        Path bassClassPath = Paths.get("input/baseClassInfoForEnum.json");
        Path topObjectsPath = Paths.get("input/topObjectsForEnum.json");

        ObjectCoverage coverage = new ObjectCoverage(bassClassPath, topObjectsPath);
        ObjectCoverage coverage1 = new ObjectCoverage(bassClassPath, topObjectsPath);

        TestObjectGraph.TargetClassForEnum obj1 = new TestObjectGraph.TargetClassForEnum();
        assert (coverage.update(obj1));

        TestObjectGraph.TargetClassForEnum obj2 = new TestObjectGraph.TargetClassForEnum();
        obj2.e = TestObjectGraph.TargetEnum.A;
        assert (coverage.update(obj2));
        assert coverage1.merge(coverage);

        TestObjectGraph.TargetClassForEnum obj3 = new TestObjectGraph.TargetClassForEnum();
        obj3.e = TestObjectGraph.TargetEnum.B;
        assert (coverage.update(obj3));
        assert coverage1.merge(coverage);
        assert !coverage1.merge(coverage);

        TestObjectGraph.TargetClassForEnum obj4 = new TestObjectGraph.TargetClassForEnum();
        obj4.e = TestObjectGraph.TargetEnum.A;
        assert (!coverage.update(obj4));
        assert !coverage1.merge(coverage);
    }

    @Test
    public void testEquality() {
        // FIXME: what's the return value of update()??? This is not clear for equality
        // update.
        Path bassClassPath = Paths.get("input/baseClassInfoForEquality.json");
        Path topObjectsPath = Paths.get("input/topObjectsForEquality.json");
        Path comparableClassesPath = Paths.get("input/comparableClassesForEquality.json");

        ObjectCoverage coverage = new ObjectCoverage(bassClassPath, topObjectsPath,
                comparableClassesPath);
        ObjectCoverage coverage1 = new ObjectCoverage(bassClassPath, topObjectsPath,
                comparableClassesPath);

        TestObjectGraph.TargetClassEquality obj1 = new TestObjectGraph.TargetClassEquality();
        assert (coverage.update(obj1));
        assert !(coverage.update(obj1));
        assert coverage1.merge(coverage);

        TestObjectGraph.TargetClassEquality obj2 = new TestObjectGraph.TargetClassEquality();
        obj2.targetClassEqualityA.targetClassEqualityAA.compClass.a = 3;
        coverage.update(obj2);
        assert coverage1.merge(coverage);
    }

    @Test
    public void testEqualityForSameObjectGraph() {
        /**
         * Conditions Same object graph equality Cond1: Should be captured by equality
         * across multiple object graphs obj1.a obj2.b
         *
         * Cond2: Should be captured by equality within the same object graph obj3.a
         * obj4.b
         */
        Path bassClassPath = Paths.get("input/baseClassInfoForEquality.json");
        Path topObjectsPath = Paths.get("input/topObjectsForEquality.json");
        Path comparableClassesPath = Paths.get("input/comparableClassesForEquality.json");

        ObjectCoverage coverage = new ObjectCoverage(bassClassPath, topObjectsPath,
                comparableClassesPath);
        ObjectCoverage coverage1 = new ObjectCoverage(bassClassPath, topObjectsPath,
                comparableClassesPath);

        // obj1--->compClass == obj2--->compClass

        // 2, 3
        TestObjectGraph.TargetClassEquality obj1 = new TestObjectGraph.TargetClassEquality();
        coverage.update(obj1);
        assert coverage1.merge(coverage);

        // 2, 3
        TestObjectGraph.TargetClassEquality obj6 = new TestObjectGraph.TargetClassEquality();
        coverage.update(obj6);

        // 4, 5
        TestObjectGraph.TargetClassEquality obj5 = new TestObjectGraph.TargetClassEquality();
        obj5.targetClassEqualityA.targetClassEqualityAA.compClass.a = 4;
        obj5.targetClassEqualityC.compClass.a = 5;
        coverage.update(obj5);

        // 10, 2
        TestObjectGraph.TargetClassEquality obj2 = new TestObjectGraph.TargetClassEquality();
        obj2.targetClassEqualityA.targetClassEqualityAA.compClass.a = 10;
        obj2.targetClassEqualityC.compClass.a = 2;
        coverage.update(obj2);
        assert coverage1.merge(coverage);

        // 4, 5
        TestObjectGraph.TargetClassEquality obj3 = new TestObjectGraph.TargetClassEquality();
        obj3.targetClassEqualityA.targetClassEqualityAA.compClass.a = 4;
        obj3.targetClassEqualityC.compClass.a = 5;
        coverage.update(obj3);
        assert !coverage1.merge(coverage);

        // obj3--->compClass == obj3--->compClass
        TestObjectGraph.TargetClassEquality obj4 = new TestObjectGraph.TargetClassEquality();
        obj4.targetClassEqualityA.targetClassEqualityAA.compClass.a = 3;
        coverage.update(obj4);
        assert coverage1.merge(coverage);

        assert !coverage1.merge(coverage);

        // Serialize coverage1 to a file and deserialize it back using GSON
        RuntimeTypeAdapterFactory<TypeInfo> typeFactory = RuntimeTypeAdapterFactory
                .of(TypeInfo.class, "type") // "type" is a field in JSON that tells us what the
                // actual type is
                .registerSubtype(ArrayType.class, "array")
                .registerSubtype(BooleanType.class, "boolean")
                .registerSubtype(CollectionType.class, "collection")
                .registerSubtype(DoubleType.class, "double")
                .registerSubtype(FloatType.class, "float")
                .registerSubtype(IntegerType.class, "integer")
                .registerSubtype(LongType.class, "long").registerSubtype(ObjectType.class, "object")
                .registerSubtype(ShortType.class, "short")
                .registerSubtype(StringType.class, "string");

        Gson gson = new GsonBuilder().registerTypeAdapterFactory(typeFactory).create();
        String jsonStr = gson.toJson(coverage1);
        System.out.println(jsonStr);

        ObjectCoverage coverage2 = gson.fromJson(jsonStr, ObjectCoverage.class);
        assert !coverage2.merge(coverage1);
    }

    // @Test
    public void testEqualityAcrossObjectGraph() {
        // FIXME!
        Path bassClassPath = Paths.get("input/baseClassInfoForEquality.json");
        Path topObjectsPath = Paths.get("input/topObjectsForEquality.json");
        Path comparableClassesPath = Paths.get("input/comparableClassesForEquality.json");

        ObjectCoverage coverage = new ObjectCoverage(bassClassPath, topObjectsPath,
                comparableClassesPath);
        ObjectCoverage coverage1 = new ObjectCoverage(bassClassPath, topObjectsPath,
                comparableClassesPath);

        // 2,3
        TestObjectGraph.TargetClassEquality obj1 = new TestObjectGraph.TargetClassEquality();
        coverage.update(obj1);

        // 6,6
        TestObjectGraph.TargetClassEquality obj4 = new TestObjectGraph.TargetClassEquality();
        obj4.targetClassEqualityA.targetClassEqualityAA.compClass.a = 6;
        obj4.targetClassEqualityC.compClass.a = 6;
        coverage.update(obj4);
        assert coverage1.merge(coverage);

        coverage.update(obj1);
        coverage.update(obj4);

        /**
         * Across: compClass -> {6: {iti1, iti2}} What should be? compClass -> {6:
         * {{iti1}, {iti2}}} Map<String, Map<Integer, Set<String>>> => Map<String,
         * Set<Set<String>>>
         *
         * Also merge for every update? Merge only once? For every update, we record the
         * itinerary of the corresponding class. When update finish, we merge it into
         * (1) same and (2) across.
         */

        // 5,2
        TestObjectGraph.TargetClassEquality obj5 = new TestObjectGraph.TargetClassEquality();
        obj5.targetClassEqualityA.targetClassEqualityAA.compClass.a = 5;
        obj5.targetClassEqualityC.compClass.a = 2;
        coverage.update(obj5);
        assert coverage1.merge(coverage);
    }

    @Test
    public void testEqualitySameItineraryAcrossObjectGraph() {
        Path bassClassPath = Paths.get("input/baseClassInfoForEquality.json");
        Path topObjectsPath = Paths.get("input/topObjectsForEquality.json");
        Path comparableClassesPath = Paths.get("input/comparableClassesForEquality.json");

        ObjectCoverage coverage = new ObjectCoverage(bassClassPath, topObjectsPath,
                comparableClassesPath);
        ObjectCoverage coverage1 = new ObjectCoverage(bassClassPath, topObjectsPath,
                comparableClassesPath);

        // 2,3
        TestObjectGraph.TargetClassEquality obj1 = new TestObjectGraph.TargetClassEquality();
        coverage.update(obj1);
        assert coverage1.merge(coverage);

        // 6,4
        TestObjectGraph.TargetClassEquality obj4 = new TestObjectGraph.TargetClassEquality();
        obj4.targetClassEqualityA.targetClassEqualityAA.compClass.a = 6;
        obj4.targetClassEqualityC.compClass.a = 4;
        coverage.update(obj4);

        assert !coverage1.merge(coverage);

        // 9, 4
        TestObjectGraph.TargetClassEquality obj5 = new TestObjectGraph.TargetClassEquality();
        obj5.targetClassEqualityA.targetClassEqualityAA.compClass.a = 9;
        obj5.targetClassEqualityC.compClass.a = 4;
        coverage.update(obj5);
        assert coverage1.merge(coverage);
    }

    @Test
    public void testLog() {
        Set<EqualitySet.SetMapping> sets = new HashSet<>();
        Set<String> v1 = new HashSet<>();
        v1.add("a");

        Set<String> v2 = new HashSet<>();
        v2.add("b");

        sets.add(new EqualitySet.SetMapping(v1, 1));
        sets.add(new EqualitySet.SetMapping(v2, 2));

        String log = "EqualitySet: " + sets;
        System.out.println(log);
    }

    @Test
    public void testIsSerialized() {
        Path bassClassPath = Paths.get("input/baseClassInfoForIsSerialized.json");
        Path topObjectsPath = Paths.get("input/topObjectsForIsSerialized.json");
        Path comparableClassesPath = Paths.get("input/comparableClassesForIsSerialized.json");
        Path modifiedFieldsPath = Paths.get("input/modifiedFieldsForIsSerialized.json");
        Path modifiedEnumsPath = Paths.get("input/modifiedEnumsForIsSerialized.json");

        ObjectCoverage coverage = new ObjectCoverage(bassClassPath, topObjectsPath,
                comparableClassesPath, modifiedFieldsPath, modifiedEnumsPath);
        ObjectCoverage coverage1 = new ObjectCoverage(bassClassPath, topObjectsPath,
                comparableClassesPath, modifiedFieldsPath, modifiedEnumsPath);
        /**
         * Check whether isSerialized will alert us if we meet a new class being
         * serialized or a new enum constant being serialized
         *
         * Let's test the new class first,
         *
         * manually create a Map<String, Set<String>> as modifiedFields.json and test it
         * Later, we create a Set<String> to test enums
         */
        TestObjectGraph.TargetClassForEnum obj1 = new TestObjectGraph.TargetClassForEnum();
        coverage.update(obj1);
        assert coverage1.merge(coverage);

        TestObjectGraph.TargetClassForEnum obj2 = new TestObjectGraph.TargetClassForEnum();
        obj2.e = TestObjectGraph.TargetEnum.A;
        coverage.update(obj2);
        assert coverage1.merge(coverage);

        TestObjectGraph.TargetClassForEnum obj3 = new TestObjectGraph.TargetClassForEnum();
        obj3.e = TestObjectGraph.TargetEnum.A;
        coverage.update(obj3);
        assert !coverage1.merge(coverage);
    }

    // @Test
    public void testJson() {
        Path bassClassPath = Paths.get("input/baseClassInfo.json");
        Path topObjectsPath = Paths.get("input/topObjects.json");
        ObjectCoverage coverage1 = new ObjectCoverage(bassClassPath, topObjectsPath);
        TestObjectGraph.TargetClassD obj1 = new TestObjectGraph.TargetClassD();
        assert (coverage1.update(obj1));

        RuntimeTypeAdapterFactory<TypeInfo> typeFactory = RuntimeTypeAdapterFactory
                .of(TypeInfo.class, "type") // "type" is a field in JSON that tells us what the
                // actual type is
                .registerSubtype(ArrayType.class, "array")
                .registerSubtype(BooleanType.class, "boolean")
                .registerSubtype(CollectionType.class, "collection")
                .registerSubtype(DoubleType.class, "double")
                .registerSubtype(FloatType.class, "float")
                .registerSubtype(IntegerType.class, "integer")
                .registerSubtype(LongType.class, "long").registerSubtype(ObjectType.class, "object")
                .registerSubtype(ShortType.class, "short")
                .registerSubtype(StringType.class, "string");

        Gson gson = new GsonBuilder().registerTypeAdapterFactory(typeFactory).create();
        String jsonStr = gson.toJson(coverage1);
        System.out.println(jsonStr);

        ObjectCoverage coverage2 = gson.fromJson(jsonStr, ObjectCoverage.class);
    }

    @Test
    public void test() {

    }

}
