package org.zlab.ocov.tracker.graph;

import org.junit.jupiter.api.Test;
import org.zlab.ocov.Utils;
import org.zlab.ocov.tracker.TargetClass;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class TestObjectGraphDumper {

    @Test
    public void testEnum() {
        // TODO
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

        TargetClass.TargetClassE obj1 = new TargetClass.TargetClassE();
        obj1.fList.add(new TargetClass.TargetClassF2());
        obj1.fList.add(new TargetClass.TargetClassF2());
        obj1.fList.add(new TargetClass.TargetClassF2());

        ObjectGraph objectGraph = dumper.dump(obj1, -1);

        objectGraph.print();

    }
}
