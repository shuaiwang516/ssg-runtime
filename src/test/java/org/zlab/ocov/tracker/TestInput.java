package org.zlab.ocov.tracker;

import org.junit.jupiter.api.Test;
import org.zlab.ocov.Utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TestInput {

    @Test
    public void createExample() {
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

        Utils.saveMapToFile(baseClassInfo, "input/baseClassInfo.json");
    }

    @Test
    public void createExampleTopObjects() {
        Set<String> baseClassInfo = new HashSet<>();
        // org.zlab.ocov.dumper.TestObjectGraphDumper$TargetClassA
        Utils.saveSetToFile(baseClassInfo, "input/topObjects.json");
    }

    @Test
    public void test() {
        // Example input for baseClassInfo
        Map<String, Map<String, String>> baseClassInfo = new HashMap<>();
        Map<String, String> cfMetaData = new HashMap<>();
    }

}
