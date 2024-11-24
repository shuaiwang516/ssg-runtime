package org.zlab.ocov.tracker.graph;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import org.jgrapht.graph.DirectedMultigraph;
import org.zlab.ocov.Utils;
import org.zlab.ocov.tracker.graph.label.LabelConstraint;
import org.zlab.ocov.tracker.graph.label.ValueConstraint;
import org.zlab.ocov.tracker.graph.structure.AccumulatedSizeConstraint;
import org.zlab.ocov.tracker.graph.structure.InDegreeConstraint;
import org.zlab.ocov.tracker.graph.structure.OutDegreeConstraint;
import org.zlab.ocov.tracker.graph.structure.StructureConstraint;
import org.zlab.ocov.tracker.inv.Invariant;
import org.zlab.ocov.tracker.inv.InvariantBrokenFrequency;
import org.zlab.ocov.tracker.inv.unary.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zlab.ocov.tracker.*;
import org.zlab.ocov.tracker.Runtime;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class TestObjectGraphCoverage {

    @BeforeAll
    public static void init() {
        Runtime.initWriter();
    }

    @Test
    public void testCreateGraphPattern() {
        Path baseClassPath = Paths.get("input/baseClassInfoForEnum.json");
        Path topObjectsPath = Paths.get("input/topObjectsForEnum.json");

        ObjectGraphCoverage objectGraphCoverage = new ObjectGraphCoverage(baseClassPath,
                topObjectsPath);
        ObjectGraphCoverage objectGraphCoverage1 = new ObjectGraphCoverage(baseClassPath,
                topObjectsPath);

        TargetClass.TargetClassForEnum targetClassForEnum = new TargetClass.TargetClassForEnum();
        assert objectGraphCoverage.update(targetClassForEnum);
        assert !objectGraphCoverage.update(targetClassForEnum);
        assert objectGraphCoverage1.merge(objectGraphCoverage).isNewFormat();
        assert !objectGraphCoverage1.merge(objectGraphCoverage).isNewFormat();

        TargetClass.TargetClassForEnum targetClassForEnum1 = new TargetClass.TargetClassForEnum();
        targetClassForEnum1.e = TargetClass.TargetEnum.B;

        assert objectGraphCoverage.update(targetClassForEnum1);
        assert !objectGraphCoverage.update(targetClassForEnum1);
        assert objectGraphCoverage1.merge(objectGraphCoverage).isNewFormat();
        assert !objectGraphCoverage1.merge(objectGraphCoverage).isNewFormat();

        TargetClass.TargetClassForEnum targetClassForEnum2 = new TargetClass.TargetClassForEnum();
        targetClassForEnum2.e = TargetClass.TargetEnum.A;
        assert objectGraphCoverage.update(targetClassForEnum2);
        assert !objectGraphCoverage.update(targetClassForEnum2);
        assert objectGraphCoverage1.merge(objectGraphCoverage).isNewFormat();
        assert !objectGraphCoverage1.merge(objectGraphCoverage).isNewFormat();
    }

    @Test
    public void testCollection() {
        Path baseClassPath = Paths.get("input/baseClassInfo1.json");
        Path topObjectsPath = Paths.get("input/topObjects1.json");

        ObjectGraphCoverage coverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath);
        ObjectGraphCoverage coverage1 = new ObjectGraphCoverage(baseClassPath, topObjectsPath);

        TargetClass.TargetClassE obj2 = new TargetClass.TargetClassE();
        obj2.fList.add(new TargetClass.TargetClassF1());
        assert (coverage.update(obj2));
        assert coverage1.merge(coverage).isNewFormat();

        TargetClass.TargetClassE obj3 = new TargetClass.TargetClassE();
        TargetClass.TargetClassF1 tmpF31 = new TargetClass.TargetClassF1();
        tmpF31.f1 = 0;
        obj3.fList.add(tmpF31);
        assert (coverage1.update(obj3));
        assert coverage.merge(coverage1).isNewFormat();
        // coverage1.objCoverage.get(obj3.getClass().getName()).print();

        TargetClass.TargetClassE obj4 = new TargetClass.TargetClassE();
        TargetClass.TargetClassF1 tmpF41 = new TargetClass.TargetClassF1();
        TargetClass.TargetClassF1 tmpF42 = new TargetClass.TargetClassF1();
        tmpF41.f1 = 0;
        obj4.fList.add(tmpF41);
        tmpF42.f1 = 0;
        obj4.fList.add(tmpF42);
        assert (coverage1.update(obj4));
        assert coverage.merge(coverage1).isNewFormat();
    }

    @Test
    public void testMap() {
        Path baseClassPath = Paths.get("input/baseClassInfo2.json");
        Path topObjectsPath = Paths.get("input/topObjects2.json");

        ObjectGraphCoverage coverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath);
        ObjectGraphCoverage coverage1 = new ObjectGraphCoverage(baseClassPath, topObjectsPath);

        TargetClass.TargetClassWithMap obj1 = new TargetClass.TargetClassWithMap();
        assert (coverage.update(obj1));

        TargetClass.TargetClassWithMap obj2 = new TargetClass.TargetClassWithMap();
        assert (!coverage.update(obj2));

        TargetClass.TargetClassWithMap obj3 = new TargetClass.TargetClassWithMap();
        obj3.map.put(1, new TargetClass.TargetClassF1());
        assert (coverage.update(obj3));

        assert coverage1.merge(coverage).isNewFormat();
        assert !coverage1.merge(coverage).isNewFormat();
    }

    @Test
    public void testSubObject() {
        // obj1 and obj2 share same format, obj3 is different
        Path baseClassPath = Paths.get("input/baseClassInfo.json");
        Path topObjectsPath = Paths.get("input/topObjects.json");

        ObjectGraphCoverage coverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath);
        ObjectGraphCoverage coverage1 = new ObjectGraphCoverage(baseClassPath, topObjectsPath);

        TargetClass.TargetClassA obj1 = new TargetClass.TargetClassA();
        assert (coverage.update(obj1));
        assert coverage1.merge(coverage).isNewFormat();

        TargetClass.TargetClassA obj2 = new TargetClass.TargetClassA();
        assert (!coverage.update(obj2));
        assert !coverage1.merge(coverage).isNewFormat();

        TargetClass.TargetClassA obj3 = new TargetClass.TargetClassA();
        obj3.bObj.i = 1;
        assert (coverage.update(obj3));
        assert coverage1.merge(coverage).isNewFormat();

        TargetClass.TargetClassD obj4 = new TargetClass.TargetClassD();
        assert (coverage.update(obj4));
        assert coverage1.merge(coverage).isNewFormat();

        TargetClass.TargetClassD obj5 = new TargetClass.TargetClassD();
        obj5.bObj.i = 1;
        assert (coverage.update(obj5));
        assert coverage1.merge(coverage).isNewFormat();
    }

    @Test
    public void testCoverageMerge() {
        Path baseClassPath = Paths.get("input/baseClassInfo.json");
        Path topObjectsPath = Paths.get("input/topObjects.json");
        ObjectGraphCoverage coverage1 = new ObjectGraphCoverage(baseClassPath, topObjectsPath);
        ObjectGraphCoverage coverage2 = new ObjectGraphCoverage(baseClassPath, topObjectsPath);

        TargetClass.TargetClassD obj1 = new TargetClass.TargetClassD();
        assert (coverage1.update(obj1));

        TargetClass.TargetClassD obj2 = new TargetClass.TargetClassD();
        obj2.a = 0;
        assert (coverage2.update(obj2));
        assert coverage1.merge(coverage2).isNewFormat();
        // Test itinerary
        // coverage1.objCoverage.get(obj1.getClass().getName()).print();
    }

    // @Test
    public void testEqualityForSameObjectGraph() {
        if (!Runtime.enableEqualityLikelyInvariant || !EqualitySet.enableSameObjEquality)
            return;
        Path baseClassPath = Paths.get("input/baseClassInfoForEquality.json");
        Path topObjectsPath = Paths.get("input/topObjectsForEquality.json");
        Path comparableClassesPath = Paths.get("input/comparableClassesForEquality.json");

        ObjectGraphCoverage coverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                comparableClassesPath);
        ObjectGraphCoverage coverage1 = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                comparableClassesPath);

        // obj1--->compClass == obj2--->compClass

        // 2, 3
        TargetClass.TargetClassEquality obj1 = new TargetClass.TargetClassEquality();
        coverage.update(obj1);
        coverage.inferInvariant();
        assert coverage1.merge(coverage).isNewFormat();

        // 2, 3
        TargetClass.TargetClassEquality obj6 = new TargetClass.TargetClassEquality();
        coverage.update(obj6);

        // 4, 5
        TargetClass.TargetClassEquality obj5 = new TargetClass.TargetClassEquality();
        obj5.targetClassEqualityA.targetClassEqualityAA.compClass.a = 4;
        obj5.targetClassEqualityC.compClass.a = 5;
        coverage.update(obj5);

        // 10, 2
        TargetClass.TargetClassEquality obj2 = new TargetClass.TargetClassEquality();
        obj2.targetClassEqualityA.targetClassEqualityAA.compClass.a = 10;
        obj2.targetClassEqualityC.compClass.a = 2;
        coverage.update(obj2);

        coverage.inferInvariant();
        assert coverage1.merge(coverage).isNewFormat();

        // 4, 5
        TargetClass.TargetClassEquality obj3 = new TargetClass.TargetClassEquality();
        obj3.targetClassEqualityA.targetClassEqualityAA.compClass.a = 4;
        obj3.targetClassEqualityC.compClass.a = 5;
        coverage.update(obj3);
        coverage.inferInvariant();
        assert !coverage1.merge(coverage).isNewFormat();

        // obj3--->compClass == obj3--->compClass
        TargetClass.TargetClassEquality obj4 = new TargetClass.TargetClassEquality();
        obj4.targetClassEqualityA.targetClassEqualityAA.compClass.a = 3;
        coverage.update(obj4);
        coverage.inferInvariant();
        assert coverage1.merge(coverage).isNewFormat();
        assert !coverage1.merge(coverage).isNewFormat();

        // test a single graph pattern ser/de
        // DirectedMultigraph<GraphPattern.Vertex, GraphPattern.Edge> graph =
        // coverage1.dumpId2ObjCoverage
        // .get(-1).get(obj1.getClass().getName()).graph;

        // Gson gson = ObjectGraphCoverage.constructGson();
        // String jsonStr = gson.toJson(graph);
        // // System.out.println(jsonStr);
        // DirectedMultigraph<GraphPattern.Vertex, GraphPattern.Edge> graphFromGson =
        // gson.fromJson(
        // jsonStr,
        // new TypeToken<DirectedMultigraph<GraphPattern.Vertex, GraphPattern.Edge>>() {
        // }.getType());
    }

    @Test
    public void testEqualityForSameObjectGraph1() {
        if (!Runtime.enableEqualityLikelyInvariant || !EqualitySet.enableSameObjEquality)
            return;
        Path baseClassPath = Paths.get("input/baseClassInfoForEquality.json");
        Path topObjectsPath = Paths.get("input/topObjectsForEquality.json");
        Path comparableClassesPath = Paths.get("input/comparableClassesForEquality.json");

        ObjectGraphCoverage coverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                comparableClassesPath);
        ObjectGraphCoverage coverage1 = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                comparableClassesPath);

        // obj1--->compClass == obj2--->compClass

        // 2, 3
        TargetClass.TargetClassEquality obj1 = new TargetClass.TargetClassEquality();
        coverage.update(obj1);
        assert coverage1.merge(coverage).isNewFormat();

        // 2, 3
        TargetClass.TargetClassEquality obj2 = new TargetClass.TargetClassEquality();
        coverage.update(obj2);
        assert !coverage1.merge(coverage).isNewFormat();

        // 2, 2
        TargetClass.TargetClassEquality obj3 = new TargetClass.TargetClassEquality();
        obj3.targetClassEqualityC.compClass.a = 2;
        coverage.update(obj3);
        assert coverage1.merge(coverage).isNewFormat();

        // // test a single graph pattern ser/de
        // DirectedMultigraph<GraphPattern.Vertex, GraphPattern.Edge> graph =
        // coverage1.dumpId2ObjCoverageWithContext.get(-1).
        // get("").get(obj1.getClass().getName()).graph;
        //
        // String jsonStr = Utils.gson.toJson(graph);
        // // System.out.println(jsonStr);
        // DirectedMultigraph<GraphPattern.Vertex, GraphPattern.Edge> graphFromGson =
        // Utils.gson.fromJson(
        // jsonStr,
        // new TypeToken<DirectedMultigraph<GraphPattern.Vertex, GraphPattern.Edge>>() {
        // }.getType());
    }

    // @Test
    public void testEqualityForSameObjectGraph1MatchableFormat() {
        if (!Runtime.enableEqualityLikelyInvariant || !EqualitySet.enableSameObjEquality)
            return;
        Path baseClassPath = Paths.get("input/baseClassInfoForEquality.json");
        Path topObjectsPath = Paths.get("input/topObjectsForEquality.json");
        Path comparableClassesPath = Paths.get("input/comparableClassesForEquality.json");

        ObjectGraphCoverage coverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                comparableClassesPath);
        ObjectGraphCoverage coverage1 = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                comparableClassesPath);

        Map<String, Map<String, String>> matchableClassInfo = new HashMap<>();
        // org.zlab.ocov.tracker.TargetClass$TargetClassE.fList.collection_firstItem->org.zlab.ocov.tracker.TargetClass$TargetClassF1
        matchableClassInfo.put("org.zlab.ocov.tracker.TargetClass$TargetClassEquality",
                new HashMap<>());
        matchableClassInfo.get("org.zlab.ocov.tracker.TargetClass$TargetClassEquality").put(
                "targetClassEqualityA", "org.zlab.ocov.tracker.TargetClass$TargetClassEqualityA");
        coverage1.setMatchableClassInfo(matchableClassInfo);

        // obj1--->compClass == obj2--->compClass

        // 2, 3
        TargetClass.TargetClassEquality obj1 = new TargetClass.TargetClassEquality();
        coverage.update(obj1);
        assert coverage1.merge(coverage).isNewFormat();

        // 2, 2
        TargetClass.TargetClassEquality obj3 = new TargetClass.TargetClassEquality();
        obj3.targetClassEqualityC.compClass.a = 2;
        coverage.update(obj3);
        FormatCoverageStatus formatCoverageStatus = coverage1.merge(coverage);
        assert formatCoverageStatus.isNewFormat();
        assert formatCoverageStatus.isNonMatchableNewFormat();
    }

    // @Test
    public void testEqualityForSameObjectGraph1MatchableFormat1() {
        if (!Runtime.enableEqualityLikelyInvariant || !EqualitySet.enableSameObjEquality)
            return;
        Path baseClassPath = Paths.get("input/baseClassInfoForEquality.json");
        Path topObjectsPath = Paths.get("input/topObjectsForEquality.json");
        Path comparableClassesPath = Paths.get("input/comparableClassesForEquality.json");

        ObjectGraphCoverage coverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                comparableClassesPath);
        ObjectGraphCoverage coverage1 = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                comparableClassesPath);

        Map<String, Map<String, String>> matchableClassInfo = Utils.loadMapFromFile(baseClassPath);
        coverage1.setMatchableClassInfo(matchableClassInfo);

        // obj1--->compClass == obj2--->compClass

        // 2, 3
        TargetClass.TargetClassEquality obj1 = new TargetClass.TargetClassEquality();
        coverage.update(obj1);
        assert coverage1.merge(coverage).isNewFormat();

        // 2, 2
        TargetClass.TargetClassEquality obj3 = new TargetClass.TargetClassEquality();
        obj3.targetClassEqualityC.compClass.a = 2;
        coverage.update(obj3);
        FormatCoverageStatus formatCoverageStatus = coverage1.merge(coverage);
        assert formatCoverageStatus.isNewFormat();
        assert !formatCoverageStatus.isNonMatchableNewFormat();
    }

    @Test
    public void testIsSerialized() {
        Path baseClassPath = Paths.get("input/baseClassInfoForIsSerialized.json");
        Path topObjectsPath = Paths.get("input/topObjectsForIsSerialized.json");
        Path comparableClassesPath = Paths.get("input/comparableClassesForIsSerialized.json");
        Path modifiedFieldsPath = Paths.get("input/modifiedFieldsForIsSerialized.json");
        Path modifiedEnumsPath = Paths.get("input/modifiedEnumsForIsSerialized.json");

        ObjectGraphCoverage coverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                comparableClassesPath, modifiedFieldsPath, modifiedEnumsPath);
        ObjectGraphCoverage coverage1 = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                comparableClassesPath, modifiedFieldsPath, modifiedEnumsPath);
        /**
         * Check whether isSerialized will alert us if we meet a new class being
         * serialized or a new enum constant being serialized
         *
         * Let's test the new class first,
         *
         * manually create a Map<String, Set<String>> as modifiedFields.json and test it
         * Later, we create a Set<String> to test enums
         */
        TargetClass.TargetClassForEnum obj1 = new TargetClass.TargetClassForEnum();
        coverage.update(obj1);
        assert coverage1.merge(coverage).isNewFormat();
        coverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath, comparableClassesPath,
                modifiedFieldsPath, modifiedEnumsPath);

        TargetClass.TargetClassForEnum obj2 = new TargetClass.TargetClassForEnum();
        obj2.e = TargetClass.TargetEnum.A;
        coverage.update(obj2);
        assert coverage1.merge(coverage).isNewFormat();
        coverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath, comparableClassesPath,
                modifiedFieldsPath, modifiedEnumsPath);

        TargetClass.TargetClassForEnum obj3 = new TargetClass.TargetClassForEnum();
        obj3.e = TargetClass.TargetEnum.A;
        coverage.update(obj3);
        assert !coverage1.merge(coverage).isNewFormat();
    }

    @Test
    public void testSizeCompute() {
        Path baseClassPath = Paths.get("input/baseClassInfoForSizeCompute.json");
        Path topObjectsPath = Paths.get("input/topObjectsForSizeCompute.json");

        ObjectGraphCoverage objectGraphCoverage = new ObjectGraphCoverage(baseClassPath,
                topObjectsPath);
        ObjectGraphCoverage objectGraphCoverage1 = new ObjectGraphCoverage(baseClassPath,
                topObjectsPath);

        TargetClass.TargetClassWithSizeBase targetClassWithSizeBase = new TargetClass.TargetClassWithSizeBase();

        assert objectGraphCoverage.update(targetClassWithSizeBase);

        assert objectGraphCoverage1.merge(objectGraphCoverage).isNewFormat();

        TargetClass.TargetClassWithSizeBase targetClassWithSizeBase1 = new TargetClass.TargetClassWithSizeBase();
        targetClassWithSizeBase1.a.size = 0;

        assert objectGraphCoverage.update(targetClassWithSizeBase1);
        assert objectGraphCoverage1.merge(objectGraphCoverage).isNewFormat();
    }

    @Test
    public void testGraphPatternVertexEqual() {
        DirectedMultigraph<GraphPattern.Vertex, GraphPattern.Edge> graph = new DirectedMultigraph<>(
                GraphPattern.Edge.class);
        GraphPattern.Vertex v = GraphPattern.createBaseVertex("a");
        graph.addVertex(v);

        Set<GraphPattern.Vertex> set = new HashSet<>();
        set.add(v);

        assert graph.containsVertex(v);

        GraphPattern.Vertex v1 = GraphPattern.createBaseVertex("a");
        assert graph.containsVertex(v1);

        set.add(v1);
        assert set.size() == 1;
    }

    /**
     * If test this, set GraphPattern.enableSequenceBoundaryCheck to true
     */
    // @Test
    public void testAccumulatedSize() {
        if (!GraphPattern.enableAccumulatedSizeCheck)
            return;
        Path baseClassPath = Paths.get("input/baseClassInfoForAccumulatedSize.json");
        Path topObjectsPath = Paths.get("input/topObjectsForAccumulatedSize.json");

        ObjectGraphCoverage objectGraphCoverage = new ObjectGraphCoverage(baseClassPath,
                topObjectsPath);
        ObjectGraphCoverage objectGraphCoverage1 = new ObjectGraphCoverage(baseClassPath,
                topObjectsPath);

        TargetClass.TargetClassAccumulateSizeBase targetClassAccumulateSizeBase = new TargetClass.TargetClassAccumulateSizeBase();

        assert objectGraphCoverage.update(targetClassAccumulateSizeBase);
        assert objectGraphCoverage1.merge(objectGraphCoverage).isBoundaryChange();

        TargetClass.TargetClassAccumulateSizeBase targetClassAccumulateSizeBase1 = new TargetClass.TargetClassAccumulateSizeBase();
        targetClassAccumulateSizeBase1.a.ids.get(0).value = 100;
        targetClassAccumulateSizeBase1.a.ids.get(1).value = 200;

        assert objectGraphCoverage.update(targetClassAccumulateSizeBase1);
        assert objectGraphCoverage1.merge(objectGraphCoverage).isBoundaryChange();
    }

    @Test
    public void testInvCombinationSingleObject() {
        if (!ObjectGraphCoverage.enableInvariantCombination)
            return;
        Path baseClassPath = Paths.get("input/baseClassInfoForInvCombination.json");
        Path topObjectsPath = Paths.get("input/topObjectsForInvCombination.json");
        Path comparableClassesPath = Paths.get("input/comparableClassesForInvCombination.json");

        ObjectGraphCoverage coverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                comparableClassesPath);
        ObjectGraphCoverage coverage1 = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                comparableClassesPath);

        // Test1: base obj
        TargetClass.TargetClassInvCombinationBase obj = new TargetClass.TargetClassInvCombinationBase();
        coverage.update(obj);
        coverage.inferInvariant();
        assert coverage1.merge(coverage).isNewFormat();
        coverage.clear();

        // Test2
        // o1: break inv1
        TargetClass.TargetClassInvCombinationBase obj1 = new TargetClass.TargetClassInvCombinationBase();
        obj1.a.value = 0;
        coverage.update(obj1);
        coverage.inferInvariant();
        assert coverage1.merge(coverage).isNewFormat();
        coverage.clear();

        // o2: break inv2
        TargetClass.TargetClassInvCombinationBase obj2 = new TargetClass.TargetClassInvCombinationBase();
        obj2.b.value = 1;
        coverage.update(obj2);
        coverage.inferInvariant();
        assert coverage1.merge(coverage).isNewFormat();
        coverage.clear();

        // o3: break inv1 and inv2 at the same time
        TargetClass.TargetClassInvCombinationBase obj3 = new TargetClass.TargetClassInvCombinationBase();
        obj3.a.value = 0;
        obj3.b.value = 1;
        coverage.update(obj3);
        coverage.inferInvariant();
        assert coverage1.merge(coverage).isNewFormat();
        coverage.clear();
    }

    @Test
    public void testInvariantCombinationMultiObjects() {
        if (!ObjectGraphCoverage.enableInvariantCombination)
            return;
        /**
         * T1 Class1, o1 break inv1
         *
         * T2 Class1, o2 break inv2
         *
         * T3 <Equality> o1 == o2
         *
         * T4 Class1, o1 break inv1 Class2, o2 break inv2 <Equality> o1 == o2
         */
        Path baseClassPath = Paths.get("input/baseClassInfoForInvCombination.json");
        Path topObjectsPath = Paths.get("input/topObjectsForInvCombination.json");
        Path comparableClassesPath = Paths.get("input/comparableClassesForInvCombination.json");

        ObjectGraphCoverage coverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                comparableClassesPath);
        ObjectGraphCoverage coverage1 = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                comparableClassesPath);

        // Test0: base obj
        TargetClass.TargetClassInvCombinationBase obj = new TargetClass.TargetClassInvCombinationBase();
        coverage.update(obj);
        coverage.inferInvariant();
        assert coverage1.merge(coverage).isNewFormat();
        coverage.clear();

        // Test1: o1: break inv1
        TargetClass.TargetClassInvCombinationBase obj1 = new TargetClass.TargetClassInvCombinationBase();
        obj1.a.value = 0;
        coverage.update(obj1);
        coverage.inferInvariant();
        assert coverage1.merge(coverage).isNewFormat();
        coverage.clear();

        // Test2: o2: break inv2
        TargetClass.TargetClassInvCombinationBase obj2 = new TargetClass.TargetClassInvCombinationBase();
        obj2.b.value = 1;
        coverage.update(obj2);
        coverage.inferInvariant();
        assert coverage1.merge(coverage).isNewFormat();
        coverage.clear();

        // Test3: o1 == o2
        TargetClass.TargetClassInvCombinationBase obj3 = new TargetClass.TargetClassInvCombinationBase();
        obj3.a.compClass.a = 4;
        coverage.update(obj3);
        coverage.inferInvariant();
        assert coverage1.merge(coverage).isNewFormat();
        coverage.clear();

        // Test4: o1 == o2
        // o1: break inv1
        // o2: break inv2
        TargetClass.TargetClassInvCombinationBase obj4 = new TargetClass.TargetClassInvCombinationBase();
        obj4.a.compClass.a = 4;
        obj4.a.value = 0;
        obj4.b.value = 1;
        coverage.update(obj4);
        coverage.inferInvariant();
        assert coverage1.merge(coverage).isNewFormat();
        coverage.clear();

        // Test5: o1 == o2
        TargetClass.TargetClassInvCombinationBase obj5 = new TargetClass.TargetClassInvCombinationBase();
        obj5.a.compClass.a = 4;
        obj5.a.value = 0;
        obj5.b.value = 1;
        coverage.update(obj5);
        coverage.inferInvariant();
        assert !coverage1.merge(coverage).isNewFormat();
        coverage.clear();
    }

    // @Test
    public void testCombination1() {
        if (!ObjectGraphCoverage.enableInvariantCombination)
            return;
        /**
         * There are 2 objects, and there are multiple equality between them
         */
        String suffix = "MultiEqual";
        Path baseClassPath = Paths.get(String.format("input/baseClassInfoFor%s.json", suffix));
        Path topObjectsPath = Paths.get(String.format("input/topObjectsFor%s.json", suffix));
        Path comparableClassesPath = Paths
                .get(String.format("input/comparableClassesFor%s.json", suffix));

        ObjectGraphCoverage coverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                comparableClassesPath);
        ObjectGraphCoverage coverage1 = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                comparableClassesPath);

        // first equality
        /**
         * Across two objects, itinerary is the same CompClass: Base->a->a occur twice
         */
        TargetClass.TargetClassMultiEqualBase obj1 = new TargetClass.TargetClassMultiEqualBase();
        obj1.b.comp1.a = 4;
        coverage.update(obj1);
        TargetClass.TargetClassMultiEqualBase obj2 = new TargetClass.TargetClassMultiEqualBase();
        coverage.update(obj2);

        coverage.inferInvariant();
        assert coverage1.merge(coverage).isNewFormat();
        coverage.clear();

        // second equality
        /**
         * Across two objects, itinerary is the same CompClass1: Base->b->a occur twice
         */
        TargetClass.TargetClassMultiEqualBase obj3 = new TargetClass.TargetClassMultiEqualBase();
        obj3.a.comp.a = 4;
        coverage.update(obj3);
        TargetClass.TargetClassMultiEqualBase obj4 = new TargetClass.TargetClassMultiEqualBase();
        coverage.update(obj4);

        coverage.inferInvariant();
        assert coverage1.merge(coverage).isNewFormat();
        coverage.clear();

        // both equality
        /**
         * The two invariant happens at the same time CompClass: Base->a->a occur twice
         * CompClass1: Base->b->a occur twice
         */
        TargetClass.TargetClassMultiEqualBase obj5 = new TargetClass.TargetClassMultiEqualBase();
        coverage.update(obj5);
        TargetClass.TargetClassMultiEqualBase obj6 = new TargetClass.TargetClassMultiEqualBase();
        coverage.update(obj6);

        coverage.inferInvariant();
        assert coverage1.merge(coverage).isNewFormat();
        coverage.clear();

        // add it again?
        TargetClass.TargetClassMultiEqualBase obj7 = new TargetClass.TargetClassMultiEqualBase();
        coverage.update(obj7);
        TargetClass.TargetClassMultiEqualBase obj8 = new TargetClass.TargetClassMultiEqualBase();
        coverage.update(obj8);

        coverage.inferInvariant();
        assert !coverage1.merge(coverage).isNewFormat();
        coverage.clear();
    }

    // @Test
    public void testCombination2() {
        if (!ObjectGraphCoverage.enableInvariantCombination)
            return;
        String suffix = "MultiEqual";
        Path baseClassPath = Paths.get(String.format("input/baseClassInfoFor%s.json", suffix));
        Path topObjectsPath = Paths.get(String.format("input/topObjectsFor%s.json", suffix));
        Path comparableClassesPath = Paths
                .get(String.format("input/comparableClassesFor%s.json", suffix));

        ObjectGraphCoverage coverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                comparableClassesPath);
        ObjectGraphCoverage coverage1 = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                comparableClassesPath);

        // first equality
        TargetClass.TargetClassMultiEqualBase obj1 = new TargetClass.TargetClassMultiEqualBase();
        obj1.b.comp1.a = 4;
        coverage.update(obj1);
        TargetClass.TargetClassMultiEqualBase1 obj2 = new TargetClass.TargetClassMultiEqualBase1();
        coverage.update(obj2);

        coverage.inferInvariant();
        assert coverage1.merge(coverage).isNewFormat();
        coverage.clear();

        // second equality
        TargetClass.TargetClassMultiEqualBase obj3 = new TargetClass.TargetClassMultiEqualBase();
        obj3.a.comp.a = 4;
        coverage.update(obj3);
        TargetClass.TargetClassMultiEqualBase1 obj4 = new TargetClass.TargetClassMultiEqualBase1();
        coverage.update(obj4);

        coverage.inferInvariant();
        assert coverage1.merge(coverage).isNewFormat();
        coverage.clear();

        // both equality
        TargetClass.TargetClassMultiEqualBase obj5 = new TargetClass.TargetClassMultiEqualBase();
        coverage.update(obj5);
        TargetClass.TargetClassMultiEqualBase1 obj6 = new TargetClass.TargetClassMultiEqualBase1();
        coverage.update(obj6);

        coverage.inferInvariant();
        assert coverage1.merge(coverage).isNewFormat();
        coverage.clear();

        // add it again?
        TargetClass.TargetClassMultiEqualBase obj7 = new TargetClass.TargetClassMultiEqualBase();
        coverage.update(obj7);
        TargetClass.TargetClassMultiEqualBase1 obj8 = new TargetClass.TargetClassMultiEqualBase1();
        coverage.update(obj8);

        coverage.inferInvariant();
        assert !coverage1.merge(coverage).isNewFormat();
        coverage.clear();
    }

    @Test
    public void testInvariantCombination3() {
        if (!ObjectGraphCoverage.enableInvariantCombination)
            return;
        String suffix = "InvariantCombination";
        Path baseClassPath = Paths.get(String.format("input/baseClassInfoFor%s.json", suffix));
        Path topObjectsPath = Paths.get(String.format("input/topObjectsFor%s.json", suffix));
        Path comparableClassesPath = Paths
                .get(String.format("input/comparableClassesFor%s.json", suffix));

        ObjectGraphCoverage coverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                comparableClassesPath);
        ObjectGraphCoverage coverage1 = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                comparableClassesPath);

        TargetClass.TargetClassForInvariantCombination obj1 = new TargetClass.TargetClassForInvariantCombination(
                0, 10);
        coverage.update(obj1);
        assert coverage1.merge(coverage).isNewFormat();

        TargetClass.TargetClassForInvariantCombination obj2 = new TargetClass.TargetClassForInvariantCombination(
                10, 0);
        coverage.update(obj2);
        assert coverage1.merge(coverage).isNewFormat();

        TargetClass.TargetClassForInvariantCombination obj3 = new TargetClass.TargetClassForInvariantCombination(
                0, 0);
        coverage.update(obj3);
        assert coverage1.merge(coverage).isNewFormat();
    }

    /**
     * Test with equality likely invariants
     *
     * test1 inv1 test1 inv2 (equality) test1 inv1, inv2 (equality)
     *
     * a == 0, b1 = 10, b2 = 11 a == 1, b1 = 10, b2 = 11 a == 0, b1 = 10, b2 = 10
     * Equality (assert true) a == 1, b1 = 10, b2 = 10 Equality (assert true)
     */
    @Test
    public void testInvariantCombination4() {
        if (!ObjectGraphCoverage.enableInvariantCombination)
            return;
        String suffix = "InvariantCombination4";
        Path baseClassPath = Paths.get(String.format("input/baseClassInfoFor%s.json", suffix));
        Path topObjectsPath = Paths.get(String.format("input/topObjectsFor%s.json", suffix));
        Path comparableClassesPath = Paths
                .get(String.format("input/comparableClassesFor%s.json", suffix));

        ObjectGraphCoverage coverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                comparableClassesPath);
        ObjectGraphCoverage coverage1 = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                comparableClassesPath);

        TargetClass.TargetClassForInvariantCombination4 obj1 = new TargetClass.TargetClassForInvariantCombination4(
                0, 10, 11);
        coverage.update(obj1);
        assert coverage1.merge(coverage).isNewFormat();
        coverage.clear();

        TargetClass.TargetClassForInvariantCombination4 obj2 = new TargetClass.TargetClassForInvariantCombination4(
                1, 10, 11);
        coverage.update(obj2);
        assert coverage1.merge(coverage).isNewFormat();
        coverage.clear();

        TargetClass.TargetClassForInvariantCombination4 obj3 = new TargetClass.TargetClassForInvariantCombination4(
                0, 10, 10);
        coverage.update(obj3);
        assert coverage1.merge(coverage).isNewFormat();
        coverage.clear();

        TargetClass.TargetClassForInvariantCombination4 obj4 = new TargetClass.TargetClassForInvariantCombination4(
                1, 10, 10);
        coverage.update(obj4);
        assert coverage1.merge(coverage).isNewFormat();
        coverage.clear();
    }

    public static Gson constructGson() {
        RuntimeTypeAdapterFactory<LabelConstraint> typeFactory1 = RuntimeTypeAdapterFactory
                .of(LabelConstraint.class, "LabelConstraint")
                .registerSubtype(ValueConstraint.class, "ValueConstraint");

        RuntimeTypeAdapterFactory<Invariant> typeFactory2 = RuntimeTypeAdapterFactory
                .of(Invariant.class, "Invariant")
                .registerSubtype(UnaryInvariant.class, "UnaryInvariant");
        RuntimeTypeAdapterFactory<UnaryInvariant> typeFactory3 = RuntimeTypeAdapterFactory
                .of(UnaryInvariant.class, "UnaryInvariant")
                .registerSubtype(IntegerLowerBound.class, "IntegerLowerBound")
                .registerSubtype(IntegerUpperBound.class, "IntegerUpperBound")
                .registerSubtype(EmptyStringOnce.class, "EmptyStringOnce")
                .registerSubtype(TrueOnce.class, "TrueOnce")
                .registerSubtype(RestOnce.class, "RestOnce")
                .registerSubtype(FalseOnce.class, "FalseOnce")
                .registerSubtype(NegativeOneOnce.class, "NegativeOneOnce")
                .registerSubtype(OneCharStringOnce.class, "OneCharStringOnce")
                .registerSubtype(NullOnce.class, "NullOnce")
                .registerSubtype(OneOnce.class, "OneOnce")
                .registerSubtype(ZeroOnce.class, "ZeroOnce")
                .registerSubtype(PreservedStringOnce.class, "PreservedStringOnce")
                .registerSubtype(RestStringSizeOnce.class, "RestStringSizeOnce")
                .registerSubtype(EnumConstant.class, "EnumConstant")
                .registerSubtype(LongLowerBound.class, "LongLowerBound")
                .registerSubtype(LongUpperBound.class, "LongUpperBound");

        RuntimeTypeAdapterFactory<StructureConstraint> typeFactory4 = RuntimeTypeAdapterFactory
                .of(StructureConstraint.class, "StructureConstraint")
                .registerSubtype(InDegreeConstraint.class, "InDegreeConstraint")
                .registerSubtype(OutDegreeConstraint.class, "OutDegreeConstraint")
                .registerSubtype(AccumulatedSizeConstraint.class, "AccumulatedSizeConstraint");

        return new GsonBuilder().registerTypeAdapterFactory(typeFactory1)
                .registerTypeAdapterFactory(typeFactory2).registerTypeAdapterFactory(typeFactory3)
                .registerTypeAdapterFactory(typeFactory4)
                .registerTypeAdapter(DirectedMultigraph.class, new GraphSerializer())
                .registerTypeAdapter(DirectedMultigraph.class, new GraphDeserializer()).create();
    }

    @Test
    public void testEqualityForStaticField() {
        Path baseClassPath = Paths.get("input/baseClassInfoForEquality.json");
        Path topObjectsPath = Paths.get("input/topObjectsForEquality.json");
        Path comparableClassesPath = Paths.get("input/comparableClassesForEquality.json");

        ObjectGraphCoverage curCoverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                comparableClassesPath);
        ObjectGraphCoverage allCoverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                comparableClassesPath);

        // test1 (base test)
        TargetClass.TargetClassEqualityA obj1 = new TargetClass.TargetClassEqualityA();
        obj1.targetClassEqualityAA.compClass.a = 5;
        TargetClass.TargetClassEqualityA.staticComp.a = 0;
        curCoverage.update(obj1);

        curCoverage.inferInvariant();
        assert allCoverage.merge(curCoverage, 0).isNewFormat();
        curCoverage.clear();

        // test2
        TargetClass.TargetClassEqualityA obj2 = new TargetClass.TargetClassEqualityA();
        TargetClass.TargetClassEqualityA.staticComp.a = 0;
        obj2.targetClassEqualityAA.compClass.a = 5;
        curCoverage.update(obj2);

        TargetClass.TargetClassEqualityA obj3 = new TargetClass.TargetClassEqualityA();
        obj2.targetClassEqualityAA.compClass.a = 4;
        TargetClass.TargetClassEqualityA.staticComp.a = 0;
        curCoverage.update(obj3);

        curCoverage.inferInvariant();
        assert !allCoverage.merge(curCoverage, 1).isNewFormat();
        curCoverage.clear();
    }

    @Test
    public void testFormatCoverageWithStackTrace() {
        /**
         * Two objects with different stack trace but same type, we want to make sure
         * they can be classified.
         */
        String suffix = "StackTrace";
        Path baseClassPath = Paths.get(String.format("input/baseClassInfoFor%s.json", suffix));
        Path topObjectsPath = Paths.get(String.format("input/topObjectsFor%s.json", suffix));
        Path comparableClassesPath = Paths
                .get(String.format("input/comparableClassesFor%s.json", suffix));

        ObjectGraphCoverage curCoverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                comparableClassesPath);
        ObjectGraphCoverage allCoverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                comparableClassesPath);

        f1(curCoverage, allCoverage, true);
        f2(curCoverage, allCoverage, true);
    }

    public void f1(ObjectGraphCoverage curCoverage, ObjectGraphCoverage allCoverage,
            boolean newFormat) {
        TargetClass.TargetClassForStacktrace obj = new TargetClass.TargetClassForStacktrace();
        curCoverage.monitorCreationContext(obj);
        obj.fList.add(1);

        curCoverage.update(obj);
        assert allCoverage.merge(curCoverage, 0).isNewFormat() == newFormat;
        curCoverage.clear();
    }

    public void f2(ObjectGraphCoverage curCoverage, ObjectGraphCoverage allCoverage,
            boolean newFormat) {
        TargetClass.TargetClassForStacktrace obj = new TargetClass.TargetClassForStacktrace();
        curCoverage.monitorCreationContext(obj);
        obj.fList.add(1);

        curCoverage.update(obj);
        assert allCoverage.merge(curCoverage, 1).isNewFormat() == newFormat;
        curCoverage.clear();
    }

    @Test
    public void testFormatCoverageWithStackTrace1() {
        /**
         * Two objects with the same creation stack trace should use the same graph
         * pattern
         */
        String suffix = "StackTrace";
        Path baseClassPath = Paths.get(String.format("input/baseClassInfoFor%s.json", suffix));
        Path topObjectsPath = Paths.get(String.format("input/topObjectsFor%s.json", suffix));
        Path comparableClassesPath = Paths
                .get(String.format("input/comparableClassesFor%s.json", suffix));

        ObjectGraphCoverage curCoverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                comparableClassesPath);
        ObjectGraphCoverage allCoverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                comparableClassesPath);

        f3(curCoverage, allCoverage);
    }

    public void f3(ObjectGraphCoverage curCoverage, ObjectGraphCoverage allCoverage) {
        TargetClass.TargetClassForStacktrace obj1 = new TargetClass.TargetClassForStacktrace();
        curCoverage.monitorCreationContext(obj1);
        String stackTraceString = curCoverage.getObjectCreationStacktrace()
                .get(System.identityHashCode(obj1));
        obj1.fList.add(1);

        curCoverage.update(obj1);
        assert allCoverage.merge(curCoverage, 0).isNewFormat();
        curCoverage.clear();

        TargetClass.TargetClassForStacktrace obj2 = new TargetClass.TargetClassForStacktrace();
        curCoverage.monitorCreationContext(obj2);
        curCoverage.getObjectCreationStacktrace().put(System.identityHashCode(obj2),
                stackTraceString);
        obj2.fList.add(1);

        curCoverage.update(obj2);
        assert !allCoverage.merge(curCoverage, 0).isNewFormat();
        curCoverage.clear();
    }

    @Test
    public void testLinkedType() {
        String suffix = "LinkedType";
        Path baseClassPath = Paths.get(String.format("input/baseClassInfoFor%s.json", suffix));
        Path topObjectsPath = Paths.get(String.format("input/topObjectsFor%s.json", suffix));
        Path comparableClassesPath = null;

        ObjectGraphCoverage curCoverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                comparableClassesPath);
        ObjectGraphCoverage allCoverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                comparableClassesPath);

        TargetClass.TargetClassForLinkedType obj1 = new TargetClass.TargetClassForLinkedType(0);
        TargetClass.TargetClassForLinkedType child1 = new TargetClass.TargetClassForLinkedType(0);
        // child shouldn't be processed!
        obj1.next = child1;

        curCoverage.update(obj1);
        assert allCoverage.merge(curCoverage, 0).isNewFormat();
        curCoverage.clear();

        TargetClass.TargetClassForLinkedType obj2 = new TargetClass.TargetClassForLinkedType(0);
        TargetClass.TargetClassForLinkedType child2 = new TargetClass.TargetClassForLinkedType(1);
        // child shouldn't be processed!
        obj2.next = child2;

        curCoverage.update(obj2);
        FormatCoverageStatus status = allCoverage.merge(curCoverage, 1);
        assert !status.isNewFormat();
        curCoverage.clear();
    }

    @Test
    public void testLinkedType1() {
        String suffix = "LinkedType1";
        Path baseClassPath = Paths.get(String.format("input/baseClassInfoFor%s.json", suffix));
        Path topObjectsPath = Paths.get(String.format("input/topObjectsFor%s.json", suffix));
        Path comparableClassesPath = null;

        ObjectGraphCoverage curCoverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                comparableClassesPath);
        ObjectGraphCoverage allCoverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                comparableClassesPath);

        TargetClass.TargetClassForLinkedType1 obj1 = new TargetClass.TargetClassForLinkedType1(0);
        TargetClass.TargetClassForLinkedType1 child1 = new TargetClass.TargetClassForLinkedType1(0);
        // child shouldn't be processed!
        obj1.children.add(child1);

        curCoverage.update(obj1);
        assert allCoverage.merge(curCoverage, 0).isNewFormat();
        curCoverage.clear();

        TargetClass.TargetClassForLinkedType1 obj2 = new TargetClass.TargetClassForLinkedType1(0);
        TargetClass.TargetClassForLinkedType1 child2 = new TargetClass.TargetClassForLinkedType1(1);
        // child shouldn't be processed!
        obj2.children.add(child2);

        curCoverage.update(obj2);
        assert !allCoverage.merge(curCoverage, 1).isNewFormat();
        curCoverage.clear();
    }

    @Test
    public void testLinkedType2() {
        String suffix = "LinkedType2";
        Path baseClassPath = Paths.get(String.format("input/baseClassInfoFor%s.json", suffix));
        Path topObjectsPath = Paths.get(String.format("input/topObjectsFor%s.json", suffix));
        Path comparableClassesPath = null;

        ObjectGraphCoverage curCoverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                comparableClassesPath);
        ObjectGraphCoverage allCoverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                comparableClassesPath);

        TargetClass.TargetClassForLinkedType2 obj1 = new TargetClass.TargetClassForLinkedType2(0);
        TargetClass.TargetClassForLinkedType2 child1 = new TargetClass.TargetClassForLinkedType2(0);
        // child shouldn't be processed!
        obj1.children = new TargetClass.TargetClassForLinkedType2[1];
        obj1.children[0] = child1;

        curCoverage.update(obj1);
        assert allCoverage.merge(curCoverage, 0).isNewFormat();
        curCoverage.clear();

        TargetClass.TargetClassForLinkedType2 obj2 = new TargetClass.TargetClassForLinkedType2(0);
        TargetClass.TargetClassForLinkedType2 child2 = new TargetClass.TargetClassForLinkedType2(1);
        // child shouldn't be processed!
        obj2.children = new TargetClass.TargetClassForLinkedType2[1];
        obj2.children[0] = child2;

        curCoverage.update(obj2);
        assert !allCoverage.merge(curCoverage, 1).isNewFormat();
        curCoverage.clear();
    }

    @Test
    public void testLinkedType3() {
        String suffix = "LinkedType3";
        Path baseClassPath = Paths.get(String.format("input/baseClassInfoFor%s.json", suffix));
        Path topObjectsPath = Paths.get(String.format("input/topObjectsFor%s.json", suffix));
        Path comparableClassesPath = null;

        ObjectGraphCoverage curCoverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                comparableClassesPath);
        ObjectGraphCoverage allCoverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                comparableClassesPath);

        TargetClass.TargetClassForLinkedType3 obj1 = new TargetClass.TargetClassForLinkedType3(0);
        TargetClass.TargetClassForLinkedType3.TargetClassForLinkedType4 child1 = new TargetClass.TargetClassForLinkedType3.TargetClassForLinkedType4(
                0);
        // child should be processed!
        obj1.next = child1;

        curCoverage.update(obj1);
        assert allCoverage.merge(curCoverage, 0).isNewFormat();
        curCoverage.clear();

        TargetClass.TargetClassForLinkedType3 obj2 = new TargetClass.TargetClassForLinkedType3(0);
        TargetClass.TargetClassForLinkedType3.TargetClassForLinkedType4 child2 = new TargetClass.TargetClassForLinkedType3.TargetClassForLinkedType4(
                1);
        // child should be processed!
        obj2.next = child2;

        curCoverage.update(obj2);
        assert allCoverage.merge(curCoverage, 1).isNewFormat();
        curCoverage.clear();
    }

    @Test
    public void testModifiedTypeHierarchy() {
        String suffix = "ModifiedTypeHierarchy";
        Path baseClassPath = Paths.get(String.format("input/baseClassInfoFor%s.json", suffix));
        Path topObjectsPath = Paths.get(String.format("input/topObjectsFor%s.json", suffix));
        Path modifiedFieldsPath = Paths
                .get(String.format("input/modifiedFieldsFor%s.json", suffix));
        Path modifiedEnumsPath = Paths.get(String.format("input/modifiedEnumsFor%s.json", suffix));
        Path modifiedTypeHierarchyPath = Paths
                .get(String.format("input/modifiedTypeHierarchyFor%s.json", suffix));

        ObjectGraphCoverage coverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath, null,
                modifiedFieldsPath, modifiedEnumsPath, modifiedTypeHierarchyPath, null);
        ObjectGraphCoverage coverage1 = new ObjectGraphCoverage(baseClassPath, topObjectsPath, null,
                modifiedFieldsPath, modifiedEnumsPath, modifiedTypeHierarchyPath, null);

        TargetClass.TargetClassF1 obj1 = new TargetClass.TargetClassF1();
        coverage.update(obj1);
        assert coverage1.merge(coverage).isNewFormat();
    }

    @Test
    public void testInheritedField() {
        String suffix = "InheritedField";
        Path baseClassPath = Paths.get(String.format("input/baseClassInfoFor%s.json", suffix));
        Path topObjectsPath = Paths.get(String.format("input/topObjectsFor%s.json", suffix));

        ObjectGraphCoverage coverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath, null);
        ObjectGraphCoverage coverage1 = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                null);

        TargetClass.TargetClassForInheritedFields1 obj1 = new TargetClass.TargetClassForInheritedFields1();
        coverage.update(obj1);
        assert coverage1.merge(coverage).isNewFormat();

        TargetClass.TargetClassForInheritedFields1 obj2 = new TargetClass.TargetClassForInheritedFields1();
        obj2.a = 1;
        coverage.update(obj2);
        assert coverage1.merge(coverage).isNewFormat();
    }

    // We do not handle this corner case for now
    // @Test
    public void testInheritedPrivateField() {
        String suffix = "InheritedPrivateField";
        Path baseClassPath = Paths.get(String.format("input/baseClassInfoFor%s.json", suffix));
        Path topObjectsPath = Paths.get(String.format("input/topObjectsFor%s.json", suffix));

        ObjectGraphCoverage coverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath, null);
        ObjectGraphCoverage coverage1 = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                null);

        TargetClass.TargetClassForInheritedPrivateField1 obj1 = new TargetClass.TargetClassForInheritedPrivateField1();
        coverage.update(obj1);
        assert coverage1.merge(coverage).isNewFormat();

        TargetClass.TargetClassForInheritedPrivateField1 obj2 = new TargetClass.TargetClassForInheritedPrivateField1();
        obj2.changeA(1);
        coverage.update(obj2);
        assert coverage1.merge(coverage).isNewFormat();
    }

    @Test
    public void testFrequencyComputing() {
        InvariantBrokenFrequency frequency = new InvariantBrokenFrequency();

        // Test1
        Map<Integer, Set<String>> brokenInvs = new HashMap<>();
        brokenInvs.put(1, new HashSet<>(Arrays.asList("inv1", "inv3")));
        frequency.update(brokenInvs);

        // Test2
        brokenInvs = new HashMap<>();
        brokenInvs.put(1, new HashSet<>(Arrays.asList("inv2", "inv3")));
        frequency.update(brokenInvs);

        // Test3
        brokenInvs = new HashMap<>();
        brokenInvs.put(1, new HashSet<>(Arrays.asList("inv3")));
        frequency.update(brokenInvs);

        Map<Integer, Set<String>> result = frequency.getMostInfrequentInvariants(2);
        assert result.get(1).contains("inv1");
        assert result.get(1).contains("inv2");
    }

    @Test
    public void testInvariantCombinationWithFrequency() {
        if (!ObjectGraphCoverage.enableInvariantCombination
                || !ObjectGraphCoverage.enableInvariantCombinationWithFrequency)
            return;
        String suffix = "InvariantCombinationWithFrequency";
        Path baseClassPath = Paths.get(String.format("input/baseClassInfoFor%s.json", suffix));
        Path topObjectsPath = Paths.get(String.format("input/topObjectsFor%s.json", suffix));
        Path comparableClassesPath = Paths
                .get(String.format("input/comparableClassesFor%s.json", suffix));

        ObjectGraphCoverage coverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                comparableClassesPath);
        ObjectGraphCoverage coverage1 = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                comparableClassesPath);
        coverage1.topNLessFrequentBrokenInvariant = 2;

        // inv1: a = 0 5
        // inv2: a = 1 4
        // inv3: b = 0 5
        // inv4: b = 1 4
        // inv5: c = 0 9
        // inv6: c = 1 0

        for (int i = 0; i < 4; i++) {
            TargetClass.TargetClassForInvariantCombinationWithFrequency obj1 = new TargetClass.TargetClassForInvariantCombinationWithFrequency(
                    0, 1, 0);
            coverage.update(obj1);
            coverage1.merge(coverage, 1, true, true);
            coverage.clear();

            TargetClass.TargetClassForInvariantCombinationWithFrequency obj2 = new TargetClass.TargetClassForInvariantCombinationWithFrequency(
                    1, 0, 0);
            coverage.update(obj2);
            coverage1.merge(coverage, 1, true, true);
            coverage.clear();
        }

        TargetClass.TargetClassForInvariantCombinationWithFrequency obj3 = new TargetClass.TargetClassForInvariantCombinationWithFrequency(
                0, 0, 0);
        coverage.update(obj3);
        assert !coverage1.merge(coverage, 3, true, true).isNewFormat();
        coverage.clear();
    }

    @Test
    public void testMatchableFormatChecking() {
        // Modified from testCollection
        Path baseClassPath = Paths.get("input/baseClassInfo1.json");
        Path topObjectsPath = Paths.get("input/topObjects1.json");

        ObjectGraphCoverage coverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath);
        ObjectGraphCoverage coverage1 = new ObjectGraphCoverage(baseClassPath, topObjectsPath);

        Map<String, Map<String, String>> matchableClassInfo = new HashMap<>();
        // org.zlab.ocov.tracker.TargetClass$TargetClassE.fList.collection_firstItem->org.zlab.ocov.tracker.TargetClass$TargetClassF1
        matchableClassInfo.put("org.zlab.ocov.tracker.TargetClass$TargetClassE", new HashMap<>());
        matchableClassInfo.get("org.zlab.ocov.tracker.TargetClass$TargetClassE").put("fList",
                "java.util.List");
        matchableClassInfo.put("org.zlab.ocov.tracker.TargetClass$TargetClassF1", new HashMap<>());
        matchableClassInfo.get("org.zlab.ocov.tracker.TargetClass$TargetClassF1").put("f1", "int");
        coverage1.setMatchableClassInfo(matchableClassInfo);

        // Testing
        TargetClass.TargetClassE obj2 = new TargetClass.TargetClassE();
        obj2.fList.add(new TargetClass.TargetClassF1());
        assert (coverage.update(obj2));
        assert coverage1.merge(coverage).isNewFormat();

        TargetClass.TargetClassE obj3 = new TargetClass.TargetClassE();
        TargetClass.TargetClassF1 tmpF31 = new TargetClass.TargetClassF1();
        tmpF31.f1 = 0;
        obj3.fList.add(tmpF31);
        coverage.update(obj3);
        FormatCoverageStatus formatCoverageStatus = coverage1.merge(coverage, 1, true, false);
        assert formatCoverageStatus.isNewFormat();
        assert formatCoverageStatus.isMatchableNewFormat();

        TargetClass.TargetClassE obj4 = new TargetClass.TargetClassE();
        TargetClass.TargetClassF1 tmpF41 = new TargetClass.TargetClassF1();
        tmpF41.f1 = 2;
        obj4.fList.add(tmpF41);
        coverage.update(obj4);
        formatCoverageStatus = coverage1.merge(coverage, 1, true, false);
        assert formatCoverageStatus.isNewFormat();
        assert formatCoverageStatus.isMatchableNewFormat();

        TargetClass.TargetClassE obj5 = new TargetClass.TargetClassE();
        TargetClass.TargetClassF1 tmpF51 = new TargetClass.TargetClassF1();
        TargetClass.TargetClassF1 tmpF52 = new TargetClass.TargetClassF1();
        tmpF51.f1 = 0;
        obj5.fList.add(tmpF51);
        tmpF52.f1 = 0;
        obj5.fList.add(tmpF52);
        assert (coverage.update(obj5));
        assert coverage1.merge(coverage).isNewFormat();
    }

    // @Test
    public void testNonMatchableFormatChecking() {
        /**
         * This test is disabled because we use a probabilistic method to decide whether
         * a format is non-matchable.
         */
        // Modified from testCollection
        Path baseClassPath = Paths.get("input/baseClassInfo1.json");
        Path topObjectsPath = Paths.get("input/topObjects1.json");

        ObjectGraphCoverage coverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath);
        ObjectGraphCoverage coverage1 = new ObjectGraphCoverage(baseClassPath, topObjectsPath);

        Map<String, Map<String, String>> matchableClassInfo = new HashMap<>();
        // org.zlab.ocov.tracker.TargetClass$TargetClassE.fList.collection_firstItem->org.zlab.ocov.tracker.TargetClass$TargetClassF1
        matchableClassInfo.put("org.zlab.ocov.tracker.TargetClass$TargetClassE", new HashMap<>());
        matchableClassInfo.get("org.zlab.ocov.tracker.TargetClass$TargetClassE").put("fList",
                "java.util.List");
        coverage1.setMatchableClassInfo(matchableClassInfo);

        // Testing
        TargetClass.TargetClassE obj2 = new TargetClass.TargetClassE();
        obj2.fList.add(new TargetClass.TargetClassF1());
        assert (coverage.update(obj2));
        assert coverage1.merge(coverage).isNewFormat();

        TargetClass.TargetClassE obj3 = new TargetClass.TargetClassE();
        TargetClass.TargetClassF1 tmpF31 = new TargetClass.TargetClassF1();
        tmpF31.f1 = 0;
        obj3.fList.add(tmpF31);
        coverage.update(obj3);
        FormatCoverageStatus formatCoverageStatus = coverage1.merge(coverage, 1, true, false);
        assert formatCoverageStatus.isNewFormat();
        assert formatCoverageStatus.isNonMatchableNewFormat();

        TargetClass.TargetClassE obj4 = new TargetClass.TargetClassE();
        TargetClass.TargetClassF1 tmpF41 = new TargetClass.TargetClassF1();
        tmpF41.f1 = 2;
        obj4.fList.add(tmpF41);
        coverage.update(obj4);
        formatCoverageStatus = coverage1.merge(coverage, 1, true, false);
        assert formatCoverageStatus.isNewFormat();
        assert formatCoverageStatus.isNonMatchableNewFormat();
    }

    @Test
    public void testIsSerialize() {
        // Modified from testCollection
        Path baseClassPath = Paths.get("input/baseClassInfo1.json");
        Path topObjectsPath = Paths.get("input/topObjects1.json");

        ObjectGraphCoverage coverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath);
        ObjectGraphCoverage coverage1 = new ObjectGraphCoverage(baseClassPath, topObjectsPath);

        Set<String> changedClasses = new HashSet<>();
        // org.zlab.ocov.tracker.TargetClass$TargetClassE.fList.collection_firstItem->org.zlab.ocov.tracker.TargetClass$TargetClassF1
        changedClasses.add("org.zlab.ocov.tracker.TargetClass$TargetClassE");
        changedClasses.add("org.zlab.ocov.tracker.TargetClass$TargetClassF1");
        coverage1.setChangedClasses(changedClasses);

        // Testing
        TargetClass.TargetClassE obj1 = new TargetClass.TargetClassE();
        coverage.update(obj1);
        FormatCoverageStatus formatCoverageStatus = coverage1.merge(coverage, 1, true, false);
        assert formatCoverageStatus.isNewFormat();
        assert formatCoverageStatus.isNewIsSerialize();
        assert !formatCoverageStatus.isMatchableNewFormat();

        TargetClass.TargetClassE obj2 = new TargetClass.TargetClassE();
        obj2.fList.add(new TargetClass.TargetClassF1());
        coverage.update(obj2);
        formatCoverageStatus = coverage1.merge(coverage, 1, true, false);
        assert formatCoverageStatus.isNewFormat();
        assert formatCoverageStatus.isNewIsSerialize();
        assert !formatCoverageStatus.isMatchableNewFormat();

        TargetClass.TargetClassE obj3 = new TargetClass.TargetClassE();
        obj3.fList.add(new TargetClass.TargetClassF1());
        coverage.update(obj3);
        formatCoverageStatus = coverage1.merge(coverage, 1, true, false);
        assert !formatCoverageStatus.isNewFormat();
        assert !formatCoverageStatus.isNewIsSerialize();
        assert !formatCoverageStatus.isMatchableNewFormat();

        TargetClass.TargetClassE obj4 = new TargetClass.TargetClassE();

        TargetClass.TargetClassF1 tmpF41 = new TargetClass.TargetClassF1();
        tmpF41.f1 = 0;
        obj4.fList.add(tmpF41);
        coverage.update(obj4);
        formatCoverageStatus = coverage1.merge(coverage, 1, true, false);
        assert formatCoverageStatus.isNewFormat();
        assert !formatCoverageStatus.isNewIsSerialize();
    }

    @Test
    public void testExtractRefPath() {
        String inv = "<Int>, iti = a->b";
        String iti = InvariantCombination.extractRefPath(inv);
        assert iti.equals("a->b");
    }
}
