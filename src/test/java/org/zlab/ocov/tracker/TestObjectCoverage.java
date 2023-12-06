package org.zlab.ocov.tracker;

import org.junit.jupiter.api.Test;
import org.zlab.ocov.dumper.TestObjectGraphDumper;

import java.nio.file.Path;
import java.nio.file.Paths;

public class TestObjectCoverage {

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
        obj3.a = 1000;
        assert (coverage.update(obj3));

        TestObjectGraphDumper.TargetClassA obj4 = new TestObjectGraphDumper.TargetClassA();
        obj4.c = 10000;
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
        obj3.bObj.i = 1000;
        assert (coverage.update(obj3));

        TestObjectGraphDumper.TargetClassD obj4 = new TestObjectGraphDumper.TargetClassD();
        assert (coverage.update(obj4));

        TestObjectGraphDumper.TargetClassD obj5 = new TestObjectGraphDumper.TargetClassD();
        obj5.bObj.i = 1000;
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

}
