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
        assert (coverage.update(obj4));
        assert (!coverage.update(obj4));
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
