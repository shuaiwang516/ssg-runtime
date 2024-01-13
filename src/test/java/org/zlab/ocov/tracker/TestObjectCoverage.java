package org.zlab.ocov.tracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zlab.ocov.dumper.TestObjectGraphDumper;
import org.zlab.ocov.tracker.type.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class TestObjectCoverage {

    @BeforeAll
    public static void init() {
        Runtime.initWriter();
    }

    @Test
    public void testObjectCoverageUpdater() {
        /**
         * Test condition: if there are 2 differences, both of them have a field with
         * classC. We want to track them differently since they might manifest
         * differently.
         */
        // obj1 and obj2 share same format, obj3 is different
        Path bassClassPath = Paths.get("input/baseClassInfo.json");
        Path topObjectsPath = Paths.get("input/topObjects.json");

        ObjectCoverage coverage = new ObjectCoverage(bassClassPath, topObjectsPath);
        TestObjectGraphDumper.TargetClassA obj1 = new TestObjectGraphDumper.TargetClassA();
        assert (coverage.update(obj1));

        TestObjectGraphDumper.TargetClassA obj2 = new TestObjectGraphDumper.TargetClassA();
        assert (!coverage.update(obj2));

        TestObjectGraphDumper.TargetClassA obj3 = new TestObjectGraphDumper.TargetClassA();
        obj3.a = -1;
        assert (coverage.update(obj3));

        TestObjectGraphDumper.TargetClassA obj4 = new TestObjectGraphDumper.TargetClassA();
        obj4.c = -1;
        assert (!coverage.update(obj4));
    }

    @Test
    public void testSubObjectUpdater() {
        /**
         * Test condition: if there are 2 differences, both of them have a field with
         * classC. We want to track them differently since they might manifest
         * differently.
         */
        // obj1 and obj2 share same format, obj3 is different

        Path bassClassPath = Paths.get("input/baseClassInfo.json");
        Path topObjectsPath = Paths.get("input/topObjects.json");

        ObjectCoverage coverage = new ObjectCoverage(bassClassPath, topObjectsPath);
        TestObjectGraphDumper.TargetClassA obj1 = new TestObjectGraphDumper.TargetClassA();
        assert (coverage.update(obj1));

        TestObjectGraphDumper.TargetClassA obj2 = new TestObjectGraphDumper.TargetClassA();
        assert (!coverage.update(obj2));

        TestObjectGraphDumper.TargetClassA obj3 = new TestObjectGraphDumper.TargetClassA();
        obj3.bObj.i = 1;
        assert (coverage.update(obj3));

        TestObjectGraphDumper.TargetClassD obj4 = new TestObjectGraphDumper.TargetClassD();
        assert (coverage.update(obj4));

        TestObjectGraphDumper.TargetClassD obj5 = new TestObjectGraphDumper.TargetClassD();
        obj5.bObj.i = 1;
        assert (coverage.update(obj5));
    }

    @Test
    public void testCollectionCoverageUpdater() {
        /**
         * Test condition: if there are 2 differences, both of them have a field with
         * classC. We want to track them differently since they might manifest
         * differently.
         */
        // obj1 and obj2 share same format, obj3 is different

        Path bassClassPath = Paths.get("input/baseClassInfo.json");
        Path topObjectsPath = Paths.get("input/topObjects.json");

        ObjectCoverage coverage = new ObjectCoverage(bassClassPath, topObjectsPath);
        TestObjectGraphDumper.TargetClassA obj1 = new TestObjectGraphDumper.TargetClassA();
        assert (coverage.update(obj1));

        TestObjectGraphDumper.TargetClassA obj2 = new TestObjectGraphDumper.TargetClassA();
        assert (!coverage.update(obj2));

        TestObjectGraphDumper.TargetClassA obj3 = new TestObjectGraphDumper.TargetClassA();
        obj3.bObj.ids.clear();
        assert (coverage.update(obj3));

        TestObjectGraphDumper.TargetClassA obj4 = new TestObjectGraphDumper.TargetClassA();
        obj4.bObj.ids.add(2);
        // assert (coverage.update(obj4));
        // assert (!coverage.update(obj4));
    }

    @Test
    public void testTmp() {
        int[] a = new int[10];
        f(a);
    }

    public void f(Object o) {
        if (o instanceof Object[]) {
            System.out.println("yes1");
        } else if (o instanceof int[]) {
            System.out.println("yes2");
        }
    }

    @Test
    public void testRecursiveTracking() {
        Path bassClassPath = Paths.get("input/baseClassInfo.json");
        Path topObjectsPath = Paths.get("input/topObjects.json");
        ObjectCoverage coverage = new ObjectCoverage(bassClassPath, topObjectsPath);
        TestObjectGraphDumper.TargetClassD obj = new TestObjectGraphDumper.TargetClassD();
        TestObjectGraphDumper.TargetClassD obj2 = new TestObjectGraphDumper.TargetClassD();
        obj.dObj = obj2;
        assert (coverage.update(obj));
    }

    @Test
    public void testCoverageMerging() {
        Path bassClassPath = Paths.get("input/baseClassInfo.json");
        Path topObjectsPath = Paths.get("input/topObjects.json");
        ObjectCoverage coverage1 = new ObjectCoverage(bassClassPath, topObjectsPath);
        TestObjectGraphDumper.TargetClassD obj1 = new TestObjectGraphDumper.TargetClassD();
        assert (coverage1.update(obj1));

        ObjectCoverage coverage2 = new ObjectCoverage(bassClassPath, topObjectsPath);

        TestObjectGraphDumper.TargetClassD obj2 = new TestObjectGraphDumper.TargetClassD();
        obj2.a = 0;
        assert (coverage2.update(obj2));

        assert (coverage1.merge(coverage2));

    }

    @Test
    public void testCollectionItemObjectCoverage() {
        /**
         * Suppose a collection will be serialized, the size of it is always2. However,
         * its object type is changed, we should also capture it and report a new format
         * coverage.
         */
        Path bassClassPath = Paths.get("input/baseClassInfo1.json");
        Path topObjectsPath = Paths.get("input/topObjects1.json");

        ObjectCoverage coverage = new ObjectCoverage(bassClassPath, topObjectsPath);
        TestObjectGraphDumper.TargetClassE obj1 = new TestObjectGraphDumper.TargetClassE();
        assert (coverage.update(obj1));

        TestObjectGraphDumper.TargetClassE obj2 = new TestObjectGraphDumper.TargetClassE();
        obj2.fList.add(new TestObjectGraphDumper.TargetClassF1());
        assert (coverage.update(obj2));

        TestObjectGraphDumper.TargetClassE obj3 = new TestObjectGraphDumper.TargetClassE();
        obj3.fList.add(new TestObjectGraphDumper.TargetClassF2());
        assert (coverage.update(obj3));
        assert (!coverage.update(obj3));
        obj3.fList.add(new TestObjectGraphDumper.TargetClassF2());
        assert (coverage.update(obj3));

        TestObjectGraphDumper.TargetClassE obj4 = new TestObjectGraphDumper.TargetClassE();
        TestObjectGraphDumper.TargetClassF1 f1 = new TestObjectGraphDumper.TargetClassF1();
        obj4.fList.add(f1);
        assert (!coverage.update(obj4));
        f1.f1 = 0;
        assert (coverage.update(obj4));
        assert (!coverage.update(obj4));

        ObjectCoverage coverage1 = new ObjectCoverage(bassClassPath, topObjectsPath);
        assert coverage1.merge(coverage);
        assert !coverage.merge(coverage1);
    }

    @Test
    public void testCollectionItemMerge() {
        /**
         * Suppose a collection will be serialized, the size of it is always2. However,
         * its object type is changed, we should also capture it and report a new format
         * coverage.
         */
        Path bassClassPath = Paths.get("input/baseClassInfo1.json");
        Path topObjectsPath = Paths.get("input/topObjects1.json");

        ObjectCoverage coverage = new ObjectCoverage(bassClassPath, topObjectsPath);

        TestObjectGraphDumper.TargetClassE obj2 = new TestObjectGraphDumper.TargetClassE();
        obj2.fList.add(new TestObjectGraphDumper.TargetClassF1());
        assert (coverage.update(obj2));

        TestObjectGraphDumper.TargetClassE obj3 = new TestObjectGraphDumper.TargetClassE();
        obj3.fList.add(new TestObjectGraphDumper.TargetClassF2());
        assert (coverage.update(obj3));

        ObjectCoverage coverage1 = new ObjectCoverage(bassClassPath, topObjectsPath);
        assert coverage1.merge(coverage);
        assert !coverage.merge(coverage1);
    }

    @Test
    public void testCollectionRecursiveMerge() {
        /**
         * Suppose a collection will be serialized, it always contains a single object.
         * If this object's field contains a new format, it should also report a new
         * format coverage. E -> [tmpF1] tmpF1 -> f1
         */
        Path bassClassPath = Paths.get("input/baseClassInfo1.json");
        Path topObjectsPath = Paths.get("input/topObjects1.json");

        ObjectCoverage coverage = new ObjectCoverage(bassClassPath, topObjectsPath);
        TestObjectGraphDumper.TargetClassE obj2 = new TestObjectGraphDumper.TargetClassE();
        obj2.fList.add(new TestObjectGraphDumper.TargetClassF1());
        assert (coverage.update(obj2));

        ObjectCoverage coverage1 = new ObjectCoverage(bassClassPath, topObjectsPath);
        TestObjectGraphDumper.TargetClassE obj3 = new TestObjectGraphDumper.TargetClassE();
        TestObjectGraphDumper.TargetClassF1 tmpF1 = new TestObjectGraphDumper.TargetClassF1();
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
        TestObjectGraphDumper.TargetClassWithMap obj1 = new TestObjectGraphDumper.TargetClassWithMap();
        assert (coverage.update(obj1));

        TestObjectGraphDumper.TargetClassWithMap obj2 = new TestObjectGraphDumper.TargetClassWithMap();
        assert (!coverage.update(obj2));

        obj2.map.put(1, new TestObjectGraphDumper.TargetClassF1());
        assert (coverage.update(obj2));

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

        TestObjectGraphDumper.TargetClassForEnum obj1 = new TestObjectGraphDumper.TargetClassForEnum();
        assert (coverage.update(obj1));

        TestObjectGraphDumper.TargetClassForEnum obj2 = new TestObjectGraphDumper.TargetClassForEnum();
        obj2.e = TestObjectGraphDumper.TargetEnum.A;
        assert (coverage.update(obj2));
        assert coverage1.merge(coverage);

        obj2.e = TestObjectGraphDumper.TargetEnum.B;
        assert (coverage.update(obj2));
        assert coverage1.merge(coverage);
        assert !coverage1.merge(coverage);

        obj2.e = TestObjectGraphDumper.TargetEnum.A;
        assert (!coverage.update(obj2));
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

        TestObjectGraphDumper.TargetClassEquality obj1 = new TestObjectGraphDumper.TargetClassEquality();
        assert (coverage.update(obj1));
        assert !(coverage.update(obj1));
        assert coverage1.merge(coverage);

        TestObjectGraphDumper.TargetClassEquality obj2 = new TestObjectGraphDumper.TargetClassEquality();
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
        TestObjectGraphDumper.TargetClassEquality obj1 = new TestObjectGraphDumper.TargetClassEquality();
        coverage.update(obj1);
        assert coverage1.merge(coverage);

        // 4, 5
        TestObjectGraphDumper.TargetClassEquality obj5 = new TestObjectGraphDumper.TargetClassEquality();
        obj5.targetClassEqualityA.targetClassEqualityAA.compClass.a = 4;
        obj5.targetClassEqualityC.compClass.a = 5;
        coverage.update(obj5);
        assert !coverage1.merge(coverage);

        // 10, 2
        TestObjectGraphDumper.TargetClassEquality obj2 = new TestObjectGraphDumper.TargetClassEquality();
        obj2.targetClassEqualityA.targetClassEqualityAA.compClass.a = 10;
        obj2.targetClassEqualityC.compClass.a = 2;
        coverage.update(obj2);
        assert coverage1.merge(coverage);

        // 4, 5
        TestObjectGraphDumper.TargetClassEquality obj3 = new TestObjectGraphDumper.TargetClassEquality();
        obj3.targetClassEqualityA.targetClassEqualityAA.compClass.a = 4;
        obj3.targetClassEqualityC.compClass.a = 5;
        coverage.update(obj3);
        assert !coverage1.merge(coverage);

        // obj3--->compClass == obj3--->compClass
        TestObjectGraphDumper.TargetClassEquality obj4 = new TestObjectGraphDumper.TargetClassEquality();
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

    // @Test
    public void testJson() {
        Path bassClassPath = Paths.get("input/baseClassInfo.json");
        Path topObjectsPath = Paths.get("input/topObjects.json");
        ObjectCoverage coverage1 = new ObjectCoverage(bassClassPath, topObjectsPath);
        TestObjectGraphDumper.TargetClassD obj1 = new TestObjectGraphDumper.TargetClassD();
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

}
