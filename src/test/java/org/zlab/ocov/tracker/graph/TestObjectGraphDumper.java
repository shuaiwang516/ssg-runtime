package org.zlab.ocov.tracker.graph;

import org.junit.jupiter.api.Test;
import org.zlab.ocov.Utils;
import org.zlab.ocov.tracker.TestObjectGraph;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TestObjectGraphDumper {

    @Test
    public void testEnum() {
        // Given an object graph, see whether we can create a correct dumped object
        // graph
        Path bassClassPath = Paths.get("input/baseClassInfoForEnum.json");
        Map<String, Map<String, String>> classInfo = GraphPattern.readClassInfo(bassClassPath);

        ObjectGraphDumper dumper = new ObjectGraphDumper(classInfo);

        TestObjectGraph.TargetClassForEnum targetClassForEnum = new TestObjectGraph.TargetClassForEnum();

        ObjectGraph objectGraph = dumper.dump(targetClassForEnum);

        // objectGraph.print();
    }

    @Test
    public void testCollection() {
        /**
         * Suppose a collection will be serialized, the size of it is always2. However,
         * its object type is changed, we should also capture it and report a new format
         * coverage.
         */
        Path bassClassPath = Paths.get("input/baseClassInfo1.json");
        Map<String, Map<String, String>> classInfo = Utils
                .loadMapFromFile(bassClassPath.toString());
        ObjectGraphDumper dumper = new ObjectGraphDumper(classInfo);

        TestObjectGraph.TargetClassE obj1 = new TestObjectGraph.TargetClassE();
        obj1.fList.add(new TestObjectGraph.TargetClassF2());
        obj1.fList.add(new TestObjectGraph.TargetClassF2());
        obj1.fList.add(new TestObjectGraph.TargetClassF2());

        ObjectGraph objectGraph = dumper.dump(obj1);

        objectGraph.print();

    }
}
