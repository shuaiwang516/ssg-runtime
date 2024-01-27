package org.zlab.ocov.tracker.graph;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zlab.ocov.tracker.ObjectCoverage;
import org.zlab.ocov.tracker.ObjectGraphCoverage;
import org.zlab.ocov.tracker.Runtime;
import org.zlab.ocov.tracker.TestObjectGraph;

import java.nio.file.Path;
import java.nio.file.Paths;

public class TestGraphPattern {

    @BeforeAll
    public static void init() {
        Runtime.initWriter();
    }

    @Test
    public void testCreateGraphPattern() {
        Path bassClassPath = Paths.get("input/baseClassInfoForEnum.json");
        Path topObjectsPath = Paths.get("input/topObjectsForEnum.json");

        ObjectGraphCoverage objectGraphCoverage = new ObjectGraphCoverage(bassClassPath,
                topObjectsPath);
        ObjectGraphCoverage objectGraphCoverage1 = new ObjectGraphCoverage(bassClassPath,
                topObjectsPath);

        TestObjectGraph.TargetClassForEnum targetClassForEnum = new TestObjectGraph.TargetClassForEnum();
        assert objectGraphCoverage.update(targetClassForEnum);
        assert !objectGraphCoverage.update(targetClassForEnum);
        assert objectGraphCoverage1.merge(objectGraphCoverage);
        assert !objectGraphCoverage1.merge(objectGraphCoverage);

        TestObjectGraph.TargetClassForEnum targetClassForEnum1 = new TestObjectGraph.TargetClassForEnum();
        targetClassForEnum1.e = TestObjectGraph.TargetEnum.B;

        assert objectGraphCoverage.update(targetClassForEnum1);
        assert !objectGraphCoverage.update(targetClassForEnum1);
        assert objectGraphCoverage1.merge(objectGraphCoverage);
        assert !objectGraphCoverage1.merge(objectGraphCoverage);
    }

    @Test
    public void testCollection() {
        Path bassClassPath = Paths.get("input/baseClassInfo1.json");
        Path topObjectsPath = Paths.get("input/topObjects1.json");

        ObjectGraphCoverage coverage = new ObjectGraphCoverage(bassClassPath, topObjectsPath);
        TestObjectGraph.TargetClassE obj2 = new TestObjectGraph.TargetClassE();
        obj2.fList.add(new TestObjectGraph.TargetClassF1());
        assert (coverage.update(obj2));

        ObjectGraphCoverage coverage1 = new ObjectGraphCoverage(bassClassPath, topObjectsPath);
        TestObjectGraph.TargetClassE obj3 = new TestObjectGraph.TargetClassE();
        TestObjectGraph.TargetClassF1 tmpF1 = new TestObjectGraph.TargetClassF1();
        tmpF1.f1 = 0;
        obj3.fList.add(tmpF1);
        assert (coverage1.update(obj3));
        assert coverage.merge(coverage1);
    }

    @Test
    public void testMap() {
        Path bassClassPath = Paths.get("input/baseClassInfo2.json");
        Path topObjectsPath = Paths.get("input/topObjects2.json");

        ObjectGraphCoverage coverage = new ObjectGraphCoverage(bassClassPath, topObjectsPath);
        ObjectGraphCoverage coverage1 = new ObjectGraphCoverage(bassClassPath, topObjectsPath);

        TestObjectGraph.TargetClassWithMap obj1 = new TestObjectGraph.TargetClassWithMap();
        assert (coverage.update(obj1));

        TestObjectGraph.TargetClassWithMap obj2 = new TestObjectGraph.TargetClassWithMap();
        assert (!coverage.update(obj2));

        TestObjectGraph.TargetClassWithMap obj3 = new TestObjectGraph.TargetClassWithMap();
        obj3.map.put(1, new TestObjectGraph.TargetClassF1());
        assert (coverage.update(obj3));

        assert coverage1.merge(coverage);
        assert !coverage1.merge(coverage);
    }

    @Test
    public void testSubObject() {
        // obj1 and obj2 share same format, obj3 is different
        Path bassClassPath = Paths.get("input/baseClassInfo.json");
        Path topObjectsPath = Paths.get("input/topObjects.json");

        ObjectGraphCoverage coverage = new ObjectGraphCoverage(bassClassPath, topObjectsPath);
        ObjectGraphCoverage coverage1 = new ObjectGraphCoverage(bassClassPath, topObjectsPath);

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
    public void testCoverageMerge() {
        Path bassClassPath = Paths.get("input/baseClassInfo.json");
        Path topObjectsPath = Paths.get("input/topObjects.json");
        ObjectGraphCoverage coverage1 = new ObjectGraphCoverage(bassClassPath, topObjectsPath);
        ObjectGraphCoverage coverage2 = new ObjectGraphCoverage(bassClassPath, topObjectsPath);

        TestObjectGraph.TargetClassD obj1 = new TestObjectGraph.TargetClassD();
        assert (coverage1.update(obj1));

        TestObjectGraph.TargetClassD obj2 = new TestObjectGraph.TargetClassD();
        obj2.a = 0;
        assert (coverage2.update(obj2));
        assert (coverage1.merge(coverage2));
    }

}
