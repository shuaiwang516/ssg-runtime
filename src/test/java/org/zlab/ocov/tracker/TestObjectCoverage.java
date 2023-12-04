package org.zlab.ocov.tracker;

import org.junit.jupiter.api.Test;
import org.zlab.ocov.dumper.TestObjectGraphDumper;

public class TestObjectCoverage {

    @Test
    public void testCoverageUpdater() {
        // obj1 and obj2 share same format, obj3 is different
        TestObjectGraphDumper.TargetClassA obj1 = new TestObjectGraphDumper.TargetClassA();
        TestObjectGraphDumper.TargetClassA obj2 = new TestObjectGraphDumper.TargetClassA();
        TestObjectGraphDumper.TargetClassA obj3 = new TestObjectGraphDumper.TargetClassA();
        obj3.a = 1000;

        boolean flag;
        ObjectCoverage coverage = new ObjectCoverage();

        flag = coverage.update(obj1);
        assert(flag);
        flag = coverage.update(obj2);
        assert(!flag);
        flag = coverage.update(obj3);
        assert(flag);
    }

}
