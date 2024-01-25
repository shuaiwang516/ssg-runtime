package org.zlab.ocov.tracker.graph;

import org.junit.jupiter.api.Test;
import org.zlab.ocov.tracker.TestObjectGraph;
import org.zlab.ocov.tracker.inv.unary.LogInfo;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class TestGraphPattern {
    /**
     * 1. Test create a graph pattern for each class 2. Test update graph pattern
     * given an object graph
     */
    @Test
    public void testCreateGraphPattern() {
        Path bassClassPath = Paths.get("input/baseClassInfoForEnum.json");
        Map<String, Map<String, String>> classInfo = GraphPattern.readClassInfo(bassClassPath);
        Map<String, GraphPattern> graphPatterns = GraphPattern.createGraphPatterns(classInfo);

        // print all
        for (Map.Entry<String, GraphPattern> entry : graphPatterns.entrySet()) {
            System.out.println(entry.getKey());
            entry.getValue().print();
            System.out.println();
        }

        // update with an object graph
        ObjectGraphDumper dumper = new ObjectGraphDumper(classInfo);
        TestObjectGraph.TargetClassForEnum targetClassForEnum = new TestObjectGraph.TargetClassForEnum();
        ObjectGraph objectGraph = dumper.dump(targetClassForEnum);

        GraphPattern graphPattern = graphPatterns.get(objectGraph.root.type);

        LogInfo logInfo = new LogInfo(-1);
        assert graphPattern.update(objectGraph, graphPatterns, logInfo);
        assert !graphPattern.update(objectGraph, graphPatterns, logInfo);

        targetClassForEnum.e = TestObjectGraph.TargetEnum.B;
        ObjectGraph objectGraph1 = dumper.dump(targetClassForEnum);
        assert graphPattern.update(objectGraph1, graphPatterns, logInfo);
        assert !graphPattern.update(objectGraph1, graphPatterns, logInfo);

        // Test merge!
        Map<String, GraphPattern> graphPatterns1 = GraphPattern.createGraphPatterns(classInfo);
        GraphPattern graphPattern1 = graphPatterns1.get(objectGraph.root.type);

        assert graphPattern1.merge(graphPattern);
        assert !graphPattern1.merge(graphPattern);

    }

    enum TestEnum {
        A, B, C;
        int a;
    }

    @Test
    public void test() {
        TestEnum a = TestEnum.A;
        a.a = 1;
        TestEnum b = TestEnum.B;
        TestEnum c = TestEnum.C;
        System.out.println(a.a);
        System.out.println(b.a);
        System.out.println(c.a);
    }

}
