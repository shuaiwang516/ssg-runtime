package org.zlab.ocov.tracker.graph;

// import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import org.jgrapht.Graph;
import org.jgrapht.graph.DirectedMultigraph;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zlab.ocov.Utils;
import org.zlab.ocov.tracker.*;
import org.zlab.ocov.tracker.Runtime;
import org.zlab.ocov.tracker.graph.label.LabelConstraint;
import org.zlab.ocov.tracker.graph.label.ValueConstraint;
import org.zlab.ocov.tracker.graph.structure.AccumulatedSizeConstraint;
import org.zlab.ocov.tracker.graph.structure.InDegreeConstraint;
import org.zlab.ocov.tracker.graph.structure.OutDegreeConstraint;
import org.zlab.ocov.tracker.graph.structure.StructureConstraint;
import org.zlab.ocov.tracker.inv.Invariant;
import org.zlab.ocov.tracker.inv.unary.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestObjectGraphCoverage {

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
        assert objectGraphCoverage1.merge(objectGraphCoverage).newFormat;
        assert !objectGraphCoverage1.merge(objectGraphCoverage).newFormat;

        TestObjectGraph.TargetClassForEnum targetClassForEnum1 = new TestObjectGraph.TargetClassForEnum();
        targetClassForEnum1.e = TestObjectGraph.TargetEnum.B;

        assert objectGraphCoverage.update(targetClassForEnum1);
        assert !objectGraphCoverage.update(targetClassForEnum1);
        assert objectGraphCoverage1.merge(objectGraphCoverage).newFormat;
        assert !objectGraphCoverage1.merge(objectGraphCoverage).newFormat;

        TestObjectGraph.TargetClassForEnum targetClassForEnum2 = new TestObjectGraph.TargetClassForEnum();
        targetClassForEnum2.e = TestObjectGraph.TargetEnum.A;
        assert objectGraphCoverage.update(targetClassForEnum2);
        assert !objectGraphCoverage.update(targetClassForEnum2);
        assert objectGraphCoverage1.merge(objectGraphCoverage).newFormat;
        assert !objectGraphCoverage1.merge(objectGraphCoverage).newFormat;
    }

    @Test
    public void testCollection() {
        Path bassClassPath = Paths.get("input/baseClassInfo1.json");
        Path topObjectsPath = Paths.get("input/topObjects1.json");

        ObjectGraphCoverage coverage = new ObjectGraphCoverage(bassClassPath, topObjectsPath);
        ObjectGraphCoverage coverage1 = new ObjectGraphCoverage(bassClassPath, topObjectsPath);

        TestObjectGraph.TargetClassE obj2 = new TestObjectGraph.TargetClassE();
        obj2.fList.add(new TestObjectGraph.TargetClassF1());
        assert (coverage.update(obj2));
        assert coverage1.merge(coverage).newFormat;

        TestObjectGraph.TargetClassE obj3 = new TestObjectGraph.TargetClassE();
        TestObjectGraph.TargetClassF1 tmpF31 = new TestObjectGraph.TargetClassF1();
        tmpF31.f1 = 0;
        obj3.fList.add(tmpF31);
        assert (coverage1.update(obj3));
        assert coverage.merge(coverage1).newFormat;
        // coverage1.objCoverage.get(obj3.getClass().getName()).print();

        TestObjectGraph.TargetClassE obj4 = new TestObjectGraph.TargetClassE();
        TestObjectGraph.TargetClassF1 tmpF41 = new TestObjectGraph.TargetClassF1();
        TestObjectGraph.TargetClassF1 tmpF42 = new TestObjectGraph.TargetClassF1();
        tmpF41.f1 = 0;
        obj4.fList.add(tmpF41);
        tmpF42.f1 = 0;
        obj4.fList.add(tmpF42);
        assert (coverage1.update(obj4));
        assert coverage.merge(coverage1).newFormat;
    }

    @Test
    public void testCollectionFirstItem() {
        String suffix = "CollectionFirstLast";
        Path baseClassPath = Paths.get(String.format("input/baseClassInfoFor%s.json", suffix));
        Path topObjectsPath = Paths.get(String.format("input/topObjectsFor%s.json", suffix));
        Path comparableClassesPath = Paths
                .get(String.format("input/comparableClassesFor%s.json", suffix));
        ObjectGraphCoverage coverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                comparableClassesPath);
        ObjectGraphCoverage coverage1 = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                comparableClassesPath);

        TestObjectGraph.TargetClassE1 obj1;
        TestObjectGraph.TargetClassE2 obj2;
        FormatCoverageStatus status;
        /* Test1 */
        obj1 = new TestObjectGraph.TargetClassE1();
        obj1.fList.add(new TestObjectGraph.CompClass(1));
        obj1.fList.add(new TestObjectGraph.CompClass(4));

        obj2 = new TestObjectGraph.TargetClassE2(2);

        coverage.update(obj1);
        coverage.update(obj2);
        coverage.inferInvariant();
        status = coverage1.merge(coverage, 0);
        coverage.clear();
        assert status.newFormat;

        /* Test2 */
        obj1 = new TestObjectGraph.TargetClassE1();
        obj1.fList.add(new TestObjectGraph.CompClass(1));
        obj1.fList.add(new TestObjectGraph.CompClass(4));

        obj2 = new TestObjectGraph.TargetClassE2(4);
        coverage.update(obj1);
        coverage.update(obj2);
        coverage.inferInvariant();

        status = coverage1.merge(coverage, 1);
        coverage.clear();
        assert status.newFormat;

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

        assert coverage1.merge(coverage).newFormat;
        assert !coverage1.merge(coverage).newFormat;
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
        assert coverage1.merge(coverage).newFormat;

        TestObjectGraph.TargetClassA obj2 = new TestObjectGraph.TargetClassA();
        assert (!coverage.update(obj2));
        assert !coverage1.merge(coverage).newFormat;

        TestObjectGraph.TargetClassA obj3 = new TestObjectGraph.TargetClassA();
        obj3.bObj.i = 1;
        assert (coverage.update(obj3));
        assert coverage1.merge(coverage).newFormat;

        TestObjectGraph.TargetClassD obj4 = new TestObjectGraph.TargetClassD();
        assert (coverage.update(obj4));
        assert coverage1.merge(coverage).newFormat;

        TestObjectGraph.TargetClassD obj5 = new TestObjectGraph.TargetClassD();
        obj5.bObj.i = 1;
        assert (coverage.update(obj5));
        assert coverage1.merge(coverage).newFormat;
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
        assert coverage1.merge(coverage2).newFormat;
        // Test itinerary
        // coverage1.objCoverage.get(obj1.getClass().getName()).print();
    }

    @Test
    public void testEquality() {
        // FIXME: what's the return value of update()??? This is not clear for equality
        // update.
        Path bassClassPath = Paths.get("input/baseClassInfoForEquality.json");
        Path topObjectsPath = Paths.get("input/topObjectsForEquality.json");
        Path comparableClassesPath = Paths.get("input/comparableClassesForEquality.json");

        ObjectGraphCoverage coverage = new ObjectGraphCoverage(bassClassPath, topObjectsPath,
                comparableClassesPath);
        ObjectGraphCoverage coverage1 = new ObjectGraphCoverage(bassClassPath, topObjectsPath,
                comparableClassesPath);

        TestObjectGraph.TargetClassEquality obj1 = new TestObjectGraph.TargetClassEquality();
        assert (coverage.update(obj1));
        assert !(coverage.update(obj1));
        assert coverage1.merge(coverage).newFormat;

        TestObjectGraph.TargetClassEquality obj2 = new TestObjectGraph.TargetClassEquality();
        obj2.targetClassEqualityA.targetClassEqualityAA.compClass.a = 3;
        coverage.update(obj2);
        assert coverage1.merge(coverage).newFormat;
    }

    @Test
    public void testEqualityForSameObjectGraph() {
        Path bassClassPath = Paths.get("input/baseClassInfoForEquality.json");
        Path topObjectsPath = Paths.get("input/topObjectsForEquality.json");
        Path comparableClassesPath = Paths.get("input/comparableClassesForEquality.json");

        ObjectGraphCoverage coverage = new ObjectGraphCoverage(bassClassPath, topObjectsPath,
                comparableClassesPath);
        ObjectGraphCoverage coverage1 = new ObjectGraphCoverage(bassClassPath, topObjectsPath,
                comparableClassesPath);

        // obj1--->compClass == obj2--->compClass

        // 2, 3
        TestObjectGraph.TargetClassEquality obj1 = new TestObjectGraph.TargetClassEquality();
        coverage.update(obj1);
        assert coverage1.merge(coverage).newFormat;

        // 2, 3
        TestObjectGraph.TargetClassEquality obj6 = new TestObjectGraph.TargetClassEquality();
        coverage.update(obj6);

        // 4, 5
        TestObjectGraph.TargetClassEquality obj5 = new TestObjectGraph.TargetClassEquality();
        obj5.targetClassEqualityA.targetClassEqualityAA.compClass.a = 4;
        obj5.targetClassEqualityC.compClass.a = 5;
        coverage.update(obj5);

        // 10, 2
        TestObjectGraph.TargetClassEquality obj2 = new TestObjectGraph.TargetClassEquality();
        obj2.targetClassEqualityA.targetClassEqualityAA.compClass.a = 10;
        obj2.targetClassEqualityC.compClass.a = 2;
        coverage.update(obj2);
        assert coverage1.merge(coverage).newFormat;

        // 4, 5
        TestObjectGraph.TargetClassEquality obj3 = new TestObjectGraph.TargetClassEquality();
        obj3.targetClassEqualityA.targetClassEqualityAA.compClass.a = 4;
        obj3.targetClassEqualityC.compClass.a = 5;
        coverage.update(obj3);
        assert !coverage1.merge(coverage).newFormat;

        // obj3--->compClass == obj3--->compClass
        TestObjectGraph.TargetClassEquality obj4 = new TestObjectGraph.TargetClassEquality();
        obj4.targetClassEqualityA.targetClassEqualityAA.compClass.a = 3;
        coverage.update(obj4);
        assert coverage1.merge(coverage).newFormat;
        assert !coverage1.merge(coverage).newFormat;

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
    public void testEqualitySameItineraryAcrossObjectGraph() {
        Path bassClassPath = Paths.get("input/baseClassInfoForEquality.json");
        Path topObjectsPath = Paths.get("input/topObjectsForEquality.json");
        Path comparableClassesPath = Paths.get("input/comparableClassesForEquality.json");

        ObjectGraphCoverage coverage = new ObjectGraphCoverage(bassClassPath, topObjectsPath,
                comparableClassesPath);
        ObjectGraphCoverage coverage1 = new ObjectGraphCoverage(bassClassPath, topObjectsPath,
                comparableClassesPath);

        // 2,3
        TestObjectGraph.TargetClassEquality obj1 = new TestObjectGraph.TargetClassEquality();
        coverage.update(obj1);
        assert coverage1.merge(coverage).newFormat;

        // 6,4
        TestObjectGraph.TargetClassEquality obj4 = new TestObjectGraph.TargetClassEquality();
        obj4.targetClassEqualityA.targetClassEqualityAA.compClass.a = 6;
        obj4.targetClassEqualityC.compClass.a = 4;
        coverage.update(obj4);

        assert !coverage1.merge(coverage).newFormat;

        // 9, 4
        TestObjectGraph.TargetClassEquality obj5 = new TestObjectGraph.TargetClassEquality();
        obj5.targetClassEqualityA.targetClassEqualityAA.compClass.a = 9;
        obj5.targetClassEqualityC.compClass.a = 4;
        coverage.update(obj5);
        assert coverage1.merge(coverage).newFormat;
    }

    // @Test
    public void testEqualityAcrossObjectGraph() {
        // FIXME!
        Path bassClassPath = Paths.get("input/baseClassInfoForEquality.json");
        Path topObjectsPath = Paths.get("input/topObjectsForEquality.json");
        Path comparableClassesPath = Paths.get("input/comparableClassesForEquality.json");

        ObjectGraphCoverage coverage = new ObjectGraphCoverage(bassClassPath, topObjectsPath,
                comparableClassesPath);
        ObjectGraphCoverage coverage1 = new ObjectGraphCoverage(bassClassPath, topObjectsPath,
                comparableClassesPath);

        // 2,3
        TestObjectGraph.TargetClassEquality obj1 = new TestObjectGraph.TargetClassEquality();
        coverage.update(obj1);

        // 6,6
        TestObjectGraph.TargetClassEquality obj4 = new TestObjectGraph.TargetClassEquality();
        obj4.targetClassEqualityA.targetClassEqualityAA.compClass.a = 6;
        obj4.targetClassEqualityC.compClass.a = 6;
        coverage.update(obj4);
        assert coverage1.merge(coverage).newFormat;

        coverage.update(obj1);
        coverage.update(obj4);

        // 5,2
        TestObjectGraph.TargetClassEquality obj5 = new TestObjectGraph.TargetClassEquality();
        obj5.targetClassEqualityA.targetClassEqualityAA.compClass.a = 5;
        obj5.targetClassEqualityC.compClass.a = 2;
        coverage.update(obj5);

        coverage.inferInvariant();

        assert coverage1.merge(coverage).newFormat;
    }

    @Test
    public void testIsSerialized() {
        Path bassClassPath = Paths.get("input/baseClassInfoForIsSerialized.json");
        Path topObjectsPath = Paths.get("input/topObjectsForIsSerialized.json");
        Path comparableClassesPath = Paths.get("input/comparableClassesForIsSerialized.json");
        Path modifiedFieldsPath = Paths.get("input/modifiedFieldsForIsSerialized.json");
        Path modifiedEnumsPath = Paths.get("input/modifiedEnumsForIsSerialized.json");

        ObjectGraphCoverage coverage = new ObjectGraphCoverage(bassClassPath, topObjectsPath,
                comparableClassesPath, modifiedFieldsPath, modifiedEnumsPath);
        ObjectGraphCoverage coverage1 = new ObjectGraphCoverage(bassClassPath, topObjectsPath,
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
        TestObjectGraph.TargetClassForEnum obj1 = new TestObjectGraph.TargetClassForEnum();
        coverage.update(obj1);
        assert coverage1.merge(coverage).newFormat;

        TestObjectGraph.TargetClassForEnum obj2 = new TestObjectGraph.TargetClassForEnum();
        obj2.e = TestObjectGraph.TargetEnum.A;
        coverage.update(obj2);
        assert coverage1.merge(coverage).newFormat;

        TestObjectGraph.TargetClassForEnum obj3 = new TestObjectGraph.TargetClassForEnum();
        obj3.e = TestObjectGraph.TargetEnum.A;
        coverage.update(obj3);
        assert !coverage1.merge(coverage).newFormat;
    }

    @Test
    public void testSizeCompute() {
        Path bassClassPath = Paths.get("input/baseClassInfoForSizeCompute.json");
        Path topObjectsPath = Paths.get("input/topObjectsForSizeCompute.json");

        ObjectGraphCoverage objectGraphCoverage = new ObjectGraphCoverage(bassClassPath,
                topObjectsPath);
        ObjectGraphCoverage objectGraphCoverage1 = new ObjectGraphCoverage(bassClassPath,
                topObjectsPath);

        TestObjectGraph.TargetClassWithSizeBase targetClassWithSizeBase = new TestObjectGraph.TargetClassWithSizeBase();

        assert objectGraphCoverage.update(targetClassWithSizeBase);

        assert objectGraphCoverage1.merge(objectGraphCoverage).newFormat;

        TestObjectGraph.TargetClassWithSizeBase targetClassWithSizeBase1 = new TestObjectGraph.TargetClassWithSizeBase();
        targetClassWithSizeBase1.a.size = 0;

        assert objectGraphCoverage.update(targetClassWithSizeBase1);
        assert objectGraphCoverage1.merge(objectGraphCoverage).newFormat;
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
        Path bassClassPath = Paths.get("input/baseClassInfoForAccumulatedSize.json");
        Path topObjectsPath = Paths.get("input/topObjectsForAccumulatedSize.json");

        ObjectGraphCoverage objectGraphCoverage = new ObjectGraphCoverage(bassClassPath,
                topObjectsPath);
        ObjectGraphCoverage objectGraphCoverage1 = new ObjectGraphCoverage(bassClassPath,
                topObjectsPath);

        TestObjectGraph.TargetClassAccumulateSizeBase targetClassAccumulateSizeBase = new TestObjectGraph.TargetClassAccumulateSizeBase();

        assert objectGraphCoverage.update(targetClassAccumulateSizeBase);
        assert objectGraphCoverage1.merge(objectGraphCoverage).boundaryChange;

        TestObjectGraph.TargetClassAccumulateSizeBase targetClassAccumulateSizeBase1 = new TestObjectGraph.TargetClassAccumulateSizeBase();
        targetClassAccumulateSizeBase1.a.ids.get(0).value = 100;
        targetClassAccumulateSizeBase1.a.ids.get(1).value = 200;

        assert objectGraphCoverage.update(targetClassAccumulateSizeBase1);
        assert objectGraphCoverage1.merge(objectGraphCoverage).boundaryChange;
    }

    @Test
    public void testInvCombinationSingleObject() {
        if (!ObjectGraphCoverage.enableInvariantCombination)
            return;
        Path bassClassPath = Paths.get("input/baseClassInfoForInvCombination.json");
        Path topObjectsPath = Paths.get("input/topObjectsForInvCombination.json");
        Path comparableClassesPath = Paths.get("input/comparableClassesForInvCombination.json");

        ObjectGraphCoverage coverage = new ObjectGraphCoverage(bassClassPath, topObjectsPath,
                comparableClassesPath);
        ObjectGraphCoverage coverage1 = new ObjectGraphCoverage(bassClassPath, topObjectsPath,
                comparableClassesPath);

        // Test1: base obj
        TestObjectGraph.TargetClassInvCombinationBase obj = new TestObjectGraph.TargetClassInvCombinationBase();
        coverage.update(obj);
        coverage.inferInvariant();
        assert coverage1.merge(coverage).newFormat;
        coverage.clear();

        // Test2
        // o1: break inv1
        TestObjectGraph.TargetClassInvCombinationBase obj1 = new TestObjectGraph.TargetClassInvCombinationBase();
        obj1.a.value = 0;
        coverage.update(obj1);
        coverage.inferInvariant();
        assert coverage1.merge(coverage).newFormat;
        coverage.clear();

        // o2: break inv2
        TestObjectGraph.TargetClassInvCombinationBase obj2 = new TestObjectGraph.TargetClassInvCombinationBase();
        obj2.b.value = 1;
        coverage.update(obj2);
        coverage.inferInvariant();
        assert coverage1.merge(coverage).newFormat;
        coverage.clear();

        // o3: break inv1 and inv2 at the same time
        TestObjectGraph.TargetClassInvCombinationBase obj3 = new TestObjectGraph.TargetClassInvCombinationBase();
        obj3.a.value = 0;
        obj3.b.value = 1;
        coverage.update(obj3);
        coverage.inferInvariant();
        assert coverage1.merge(coverage).newFormat;
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
        Path bassClassPath = Paths.get("input/baseClassInfoForInvCombination.json");
        Path topObjectsPath = Paths.get("input/topObjectsForInvCombination.json");
        Path comparableClassesPath = Paths.get("input/comparableClassesForInvCombination.json");

        ObjectGraphCoverage coverage = new ObjectGraphCoverage(bassClassPath, topObjectsPath,
                comparableClassesPath);
        ObjectGraphCoverage coverage1 = new ObjectGraphCoverage(bassClassPath, topObjectsPath,
                comparableClassesPath);

        // Test0: base obj
        TestObjectGraph.TargetClassInvCombinationBase obj = new TestObjectGraph.TargetClassInvCombinationBase();
        coverage.update(obj);
        coverage.inferInvariant();
        assert coverage1.merge(coverage).newFormat;
        coverage.clear();

        // Test1: o1: break inv1
        TestObjectGraph.TargetClassInvCombinationBase obj1 = new TestObjectGraph.TargetClassInvCombinationBase();
        obj1.a.value = 0;
        coverage.update(obj1);
        coverage.inferInvariant();
        assert coverage1.merge(coverage).newFormat;
        coverage.clear();

        // Test2: o2: break inv2
        TestObjectGraph.TargetClassInvCombinationBase obj2 = new TestObjectGraph.TargetClassInvCombinationBase();
        obj2.b.value = 1;
        coverage.update(obj2);
        coverage.inferInvariant();
        assert coverage1.merge(coverage).newFormat;
        coverage.clear();

        // Test3: o1 == o2
        TestObjectGraph.TargetClassInvCombinationBase obj3 = new TestObjectGraph.TargetClassInvCombinationBase();
        obj3.a.compClass.a = 4;
        coverage.update(obj3);
        coverage.inferInvariant();
        assert coverage1.merge(coverage).newFormat;
        coverage.clear();

        // Test4: o1 == o2
        // o1: break inv1
        // o2: break inv2
        TestObjectGraph.TargetClassInvCombinationBase obj4 = new TestObjectGraph.TargetClassInvCombinationBase();
        obj4.a.compClass.a = 4;
        obj4.a.value = 0;
        obj4.b.value = 1;
        coverage.update(obj4);
        coverage.inferInvariant();
        assert coverage1.merge(coverage).newFormat;
        coverage.clear();

        // Test5: o1 == o2
        TestObjectGraph.TargetClassInvCombinationBase obj5 = new TestObjectGraph.TargetClassInvCombinationBase();
        obj5.a.compClass.a = 4;
        obj5.a.value = 0;
        obj5.b.value = 1;
        coverage.update(obj5);
        coverage.inferInvariant();
        assert !coverage1.merge(coverage).newFormat;
        coverage.clear();
    }

    @Test
    public void testCombination1() {
        if (!ObjectGraphCoverage.enableInvariantCombination)
            return;
        /**
         * There are 2 objects, and there are multiple equality between them
         */
        String suffix = "MultiEqual";
        Path bassClassPath = Paths.get(String.format("input/baseClassInfoFor%s.json", suffix));
        Path topObjectsPath = Paths.get(String.format("input/topObjectsFor%s.json", suffix));
        Path comparableClassesPath = Paths
                .get(String.format("input/comparableClassesFor%s.json", suffix));

        ObjectGraphCoverage coverage = new ObjectGraphCoverage(bassClassPath, topObjectsPath,
                comparableClassesPath);
        ObjectGraphCoverage coverage1 = new ObjectGraphCoverage(bassClassPath, topObjectsPath,
                comparableClassesPath);

        // first equality
        /**
         * Across two objects, itinerary is the same CompClass: Base->a->a occur twice
         */
        TestObjectGraph.TargetClassMultiEqualBase obj1 = new TestObjectGraph.TargetClassMultiEqualBase();
        obj1.b.comp1.a = 4;
        coverage.update(obj1);
        TestObjectGraph.TargetClassMultiEqualBase obj2 = new TestObjectGraph.TargetClassMultiEqualBase();
        coverage.update(obj2);

        coverage.inferInvariant();
        assert coverage1.merge(coverage).newFormat;
        coverage.clear();

        // second equality
        /**
         * Across two objects, itinerary is the same CompClass1: Base->b->a occur twice
         */
        TestObjectGraph.TargetClassMultiEqualBase obj3 = new TestObjectGraph.TargetClassMultiEqualBase();
        obj3.a.comp.a = 4;
        coverage.update(obj3);
        TestObjectGraph.TargetClassMultiEqualBase obj4 = new TestObjectGraph.TargetClassMultiEqualBase();
        coverage.update(obj4);

        coverage.inferInvariant();
        assert coverage1.merge(coverage).newFormat;
        coverage.clear();

        // both equality
        /**
         * The two invariant happens at the same time CompClass: Base->a->a occur twice
         * CompClass1: Base->b->a occur twice
         */
        TestObjectGraph.TargetClassMultiEqualBase obj5 = new TestObjectGraph.TargetClassMultiEqualBase();
        coverage.update(obj5);
        TestObjectGraph.TargetClassMultiEqualBase obj6 = new TestObjectGraph.TargetClassMultiEqualBase();
        coverage.update(obj6);

        coverage.inferInvariant();
        assert coverage1.merge(coverage).newFormat;
        coverage.clear();

        // add it again?
        TestObjectGraph.TargetClassMultiEqualBase obj7 = new TestObjectGraph.TargetClassMultiEqualBase();
        coverage.update(obj7);
        TestObjectGraph.TargetClassMultiEqualBase obj8 = new TestObjectGraph.TargetClassMultiEqualBase();
        coverage.update(obj8);

        coverage.inferInvariant();
        assert !coverage1.merge(coverage).newFormat;
        coverage.clear();
    }

    @Test
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
        TestObjectGraph.TargetClassMultiEqualBase obj1 = new TestObjectGraph.TargetClassMultiEqualBase();
        obj1.b.comp1.a = 4;
        coverage.update(obj1);
        TestObjectGraph.TargetClassMultiEqualBase1 obj2 = new TestObjectGraph.TargetClassMultiEqualBase1();
        coverage.update(obj2);

        coverage.inferInvariant();
        assert coverage1.merge(coverage).newFormat;
        coverage.clear();

        // second equality
        TestObjectGraph.TargetClassMultiEqualBase obj3 = new TestObjectGraph.TargetClassMultiEqualBase();
        obj3.a.comp.a = 4;
        coverage.update(obj3);
        TestObjectGraph.TargetClassMultiEqualBase1 obj4 = new TestObjectGraph.TargetClassMultiEqualBase1();
        coverage.update(obj4);

        coverage.inferInvariant();
        assert coverage1.merge(coverage).newFormat;
        coverage.clear();

        // both equality
        TestObjectGraph.TargetClassMultiEqualBase obj5 = new TestObjectGraph.TargetClassMultiEqualBase();
        coverage.update(obj5);
        TestObjectGraph.TargetClassMultiEqualBase1 obj6 = new TestObjectGraph.TargetClassMultiEqualBase1();
        coverage.update(obj6);

        coverage.inferInvariant();
        assert coverage1.merge(coverage).newFormat;
        coverage.clear();

        // add it again?
        TestObjectGraph.TargetClassMultiEqualBase obj7 = new TestObjectGraph.TargetClassMultiEqualBase();
        coverage.update(obj7);
        TestObjectGraph.TargetClassMultiEqualBase1 obj8 = new TestObjectGraph.TargetClassMultiEqualBase1();
        coverage.update(obj8);

        coverage.inferInvariant();
        assert !coverage1.merge(coverage).newFormat;
        coverage.clear();
    }

    @Test
    public void testPreservedString() {
        /**
         * Context obj: with "system" => index block is 2 => new format! Context obj:
         * without "system => index block is 2 => new format!
         */
        String suffix = "PreservedString";

        Path baseClassPath = Paths.get(String.format("input/baseClassInfoFor%s.json", suffix));
        Path topObjectsPath = Paths.get(String.format("input/topObjectsFor%s.json", suffix));

        ObjectGraphCoverage allCoverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath);
        ObjectGraphCoverage curCoverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath);

        TestObjectGraph.TargetClassForPreservedString contextObj1 = new TestObjectGraph.TargetClassForPreservedString(
                "system");
        TestObjectGraph.TargetClassForPreservedStringTopObject obj1 = new TestObjectGraph.TargetClassForPreservedStringTopObject();
        obj1.fList.add(0);
        obj1.fList.add(1);

        curCoverage.update(obj1, 0, contextObj1);
        assert allCoverage.merge(curCoverage).newFormat;

        curCoverage.update(obj1, 0, contextObj1);
        assert !allCoverage.merge(curCoverage).newFormat;

        // Change context...
        TestObjectGraph.TargetClassForPreservedString contextObj2 = new TestObjectGraph.TargetClassForPreservedString(
                "user");
        TestObjectGraph.TargetClassForPreservedStringTopObject obj2 = new TestObjectGraph.TargetClassForPreservedStringTopObject();
        obj2.fList.add(0);
        obj2.fList.add(1);

        curCoverage.update(obj2, 0, contextObj2);
        assert allCoverage.merge(curCoverage).newFormat;

        curCoverage.update(obj2, 0, contextObj2);
        assert !allCoverage.merge(curCoverage).newFormat;

        Gson gson = Utils.constructGson();
        String jsonStr = gson.toJson(allCoverage);
        // System.out.println(jsonStr);
        ObjectGraphCoverage coverageFromGson = gson.fromJson(jsonStr, ObjectGraphCoverage.class);
    }

    @Test
    public void test() {
        Set<String> s1 = new HashSet<>();
        s1.add("a");
        s1.add("b");
        Set<String> s2 = new HashSet<>();
        s2.add("b");
        s2.add("a");

        Set<Set<String>> set = new HashSet<>();
        set.add(s1);
        assert set.contains(s2);
    }

}
