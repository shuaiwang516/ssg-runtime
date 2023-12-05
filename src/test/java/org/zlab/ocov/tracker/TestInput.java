package org.zlab.ocov.tracker;

import org.junit.jupiter.api.Test;
import org.zlab.ocov.Utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TestInput {

    @Test
    public void createExampleInput() {
        Map<String, Map<String, String>> baseClassInfo = new HashMap<>();
        // org.zlab.ocov.dumper.TestObjectGraphDumper$TargetClassA
        baseClassInfo.put("org.zlab.ocov.dumper.TestObjectGraphDumper$TargetClassA",
                new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.dumper.TestObjectGraphDumper$TargetClassA").put("a",
                "int");
        baseClassInfo.get("org.zlab.ocov.dumper.TestObjectGraphDumper$TargetClassA").put("b",
                "int");
        baseClassInfo.get("org.zlab.ocov.dumper.TestObjectGraphDumper$TargetClassA").put("bObj",
                "org.zlab.ocov.dumper.TestObjectGraphDumper$TargetClassB");

        // org.zlab.ocov.dumper.TestObjectGraphDumper$TargetClassB
        baseClassInfo.put("org.zlab.ocov.dumper.TestObjectGraphDumper$TargetClassB",
                new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.dumper.TestObjectGraphDumper$TargetClassB").put("i",
                "int");
        baseClassInfo.get("org.zlab.ocov.dumper.TestObjectGraphDumper$TargetClassB").put("ids",
                "java.util.List");

        // org.zlab.ocov.dumper.TestObjectGraphDumper$TargetClassC
        baseClassInfo.put("org.zlab.ocov.dumper.TestObjectGraphDumper$TargetClassC",
                new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.dumper.TestObjectGraphDumper$TargetClassC").put("c",
                "int");

        // org.zlab.ocov.dumper.TestObjectGraphDumper$TargetClassD
        baseClassInfo.put("org.zlab.ocov.dumper.TestObjectGraphDumper$TargetClassD",
                new HashMap<>());
        baseClassInfo.get("org.zlab.ocov.dumper.TestObjectGraphDumper$TargetClassD").put("a",
                "int");
        baseClassInfo.get("org.zlab.ocov.dumper.TestObjectGraphDumper$TargetClassD").put("bObj",
                "org.zlab.ocov.dumper.TestObjectGraphDumper$TargetClassB");
        baseClassInfo.get("org.zlab.ocov.dumper.TestObjectGraphDumper$TargetClassD").put("dObj",
                "org.zlab.ocov.dumper.TestObjectGraphDumper$TargetClassD");
        Utils.saveMapToFile(baseClassInfo, "input/baseClassInfo.json");

        Set<String> topObjects = new HashSet<>();
        topObjects.add("org.zlab.ocov.dumper.TestObjectGraphDumper$TargetClassA");
        topObjects.add("org.zlab.ocov.dumper.TestObjectGraphDumper$TargetClassD");
        Utils.saveSetToFile(topObjects, "input/topObjects.json");
    }

    @Test
    public void createExampleTopObjectsCassandra() {
        Set<String> topObjects = new HashSet<>();
        topObjects.add("org.apache.cassandra.config.CFMetaData");
        topObjects.add("org.apache.cassandra.db.Mutation");
        Utils.saveSetToFile(topObjects, "input/topObjects_cass.json");
    }

    @Test
    public void test() {
        // Example input for baseClassInfo
        ObjectCoverage coverage = new ObjectCoverage(Runtime.baseClassPath, Runtime.topObjectsPath);
    }

}
