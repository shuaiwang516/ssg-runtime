package org.zlab.ocov.tracker.graph;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import org.jgrapht.graph.DirectedMultigraph;
import org.zlab.ocov.tracker.graph.label.LabelConstraint;
import org.zlab.ocov.tracker.graph.label.ValueConstraint;
import org.zlab.ocov.tracker.graph.structure.AccumulatedSizeConstraint;
import org.zlab.ocov.tracker.graph.structure.InDegreeConstraint;
import org.zlab.ocov.tracker.graph.structure.OutDegreeConstraint;
import org.zlab.ocov.tracker.graph.structure.StructureConstraint;
import org.zlab.ocov.tracker.inv.Invariant;
import org.zlab.ocov.tracker.inv.unary.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zlab.ocov.tracker.*;
import org.zlab.ocov.tracker.Runtime;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
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

        TargetClass.TargetClassForEnum targetClassForEnum = new TargetClass.TargetClassForEnum();
        assert objectGraphCoverage.update(targetClassForEnum);
        assert !objectGraphCoverage.update(targetClassForEnum);
        assert objectGraphCoverage1.merge(objectGraphCoverage).newFormat;
        assert !objectGraphCoverage1.merge(objectGraphCoverage).newFormat;

        TargetClass.TargetClassForEnum targetClassForEnum1 = new TargetClass.TargetClassForEnum();
        targetClassForEnum1.e = TargetClass.TargetEnum.B;

        assert objectGraphCoverage.update(targetClassForEnum1);
        assert !objectGraphCoverage.update(targetClassForEnum1);
        assert objectGraphCoverage1.merge(objectGraphCoverage).newFormat;
        assert !objectGraphCoverage1.merge(objectGraphCoverage).newFormat;

        TargetClass.TargetClassForEnum targetClassForEnum2 = new TargetClass.TargetClassForEnum();
        targetClassForEnum2.e = TargetClass.TargetEnum.A;
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

        TargetClass.TargetClassE obj2 = new TargetClass.TargetClassE();
        obj2.fList.add(new TargetClass.TargetClassF1());
        assert (coverage.update(obj2));
        assert coverage1.merge(coverage).newFormat;

        TargetClass.TargetClassE obj3 = new TargetClass.TargetClassE();
        TargetClass.TargetClassF1 tmpF31 = new TargetClass.TargetClassF1();
        tmpF31.f1 = 0;
        obj3.fList.add(tmpF31);
        assert (coverage1.update(obj3));
        assert coverage.merge(coverage1).newFormat;
        // coverage1.objCoverage.get(obj3.getClass().getName()).print();

        TargetClass.TargetClassE obj4 = new TargetClass.TargetClassE();
        TargetClass.TargetClassF1 tmpF41 = new TargetClass.TargetClassF1();
        TargetClass.TargetClassF1 tmpF42 = new TargetClass.TargetClassF1();
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

        TargetClass.TargetClassE1 obj1;
        TargetClass.TargetClassE2 obj2;
        FormatCoverageStatus status;
        /* Test1 */
        obj1 = new TargetClass.TargetClassE1();
        obj1.fList.add(new TargetClass.CompClass(1));
        obj1.fList.add(new TargetClass.CompClass(4));

        obj2 = new TargetClass.TargetClassE2(2);

        coverage.update(obj1);
        coverage.update(obj2);
        coverage.inferInvariant();
        status = coverage1.merge(coverage, 0);
        coverage.clear();
        assert status.newFormat;

        /* Test2 */
        obj1 = new TargetClass.TargetClassE1();
        obj1.fList.add(new TargetClass.CompClass(1));
        obj1.fList.add(new TargetClass.CompClass(4));

        obj2 = new TargetClass.TargetClassE2(4);
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

        TargetClass.TargetClassWithMap obj1 = new TargetClass.TargetClassWithMap();
        assert (coverage.update(obj1));

        TargetClass.TargetClassWithMap obj2 = new TargetClass.TargetClassWithMap();
        assert (!coverage.update(obj2));

        TargetClass.TargetClassWithMap obj3 = new TargetClass.TargetClassWithMap();
        obj3.map.put(1, new TargetClass.TargetClassF1());
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

        TargetClass.TargetClassA obj1 = new TargetClass.TargetClassA();
        assert (coverage.update(obj1));
        assert coverage1.merge(coverage).newFormat;

        TargetClass.TargetClassA obj2 = new TargetClass.TargetClassA();
        assert (!coverage.update(obj2));
        assert !coverage1.merge(coverage).newFormat;

        TargetClass.TargetClassA obj3 = new TargetClass.TargetClassA();
        obj3.bObj.i = 1;
        assert (coverage.update(obj3));
        assert coverage1.merge(coverage).newFormat;

        TargetClass.TargetClassD obj4 = new TargetClass.TargetClassD();
        assert (coverage.update(obj4));
        assert coverage1.merge(coverage).newFormat;

        TargetClass.TargetClassD obj5 = new TargetClass.TargetClassD();
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

        TargetClass.TargetClassD obj1 = new TargetClass.TargetClassD();
        assert (coverage1.update(obj1));

        TargetClass.TargetClassD obj2 = new TargetClass.TargetClassD();
        obj2.a = 0;
        assert (coverage2.update(obj2));
        assert coverage1.merge(coverage2).newFormat;
        // Test itinerary
        // coverage1.objCoverage.get(obj1.getClass().getName()).print();
    }

    @Test
    public void testEquality() {
        Path bassClassPath = Paths.get("input/baseClassInfoForEquality.json");
        Path topObjectsPath = Paths.get("input/topObjectsForEquality.json");
        Path comparableClassesPath = Paths.get("input/comparableClassesForEquality.json");

        ObjectGraphCoverage coverage = new ObjectGraphCoverage(bassClassPath, topObjectsPath,
                comparableClassesPath);
        ObjectGraphCoverage coverage1 = new ObjectGraphCoverage(bassClassPath, topObjectsPath,
                comparableClassesPath);

        TargetClass.TargetClassEquality obj1 = new TargetClass.TargetClassEquality();
        assert (coverage.update(obj1));
        assert !(coverage.update(obj1));
        assert coverage1.merge(coverage).newFormat;

        TargetClass.TargetClassEquality obj2 = new TargetClass.TargetClassEquality();
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
        TargetClass.TargetClassEquality obj1 = new TargetClass.TargetClassEquality();
        coverage.update(obj1);
        assert coverage1.merge(coverage).newFormat;

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
        assert coverage1.merge(coverage).newFormat;

        // 4, 5
        TargetClass.TargetClassEquality obj3 = new TargetClass.TargetClassEquality();
        obj3.targetClassEqualityA.targetClassEqualityAA.compClass.a = 4;
        obj3.targetClassEqualityC.compClass.a = 5;
        coverage.update(obj3);
        assert !coverage1.merge(coverage).newFormat;

        // obj3--->compClass == obj3--->compClass
        TargetClass.TargetClassEquality obj4 = new TargetClass.TargetClassEquality();
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
        TargetClass.TargetClassEquality obj1 = new TargetClass.TargetClassEquality();
        coverage.update(obj1);
        assert coverage1.merge(coverage).newFormat;

        // 6,4
        TargetClass.TargetClassEquality obj4 = new TargetClass.TargetClassEquality();
        obj4.targetClassEqualityA.targetClassEqualityAA.compClass.a = 6;
        obj4.targetClassEqualityC.compClass.a = 4;
        coverage.update(obj4);

        assert !coverage1.merge(coverage).newFormat;

        // 9, 4
        TargetClass.TargetClassEquality obj5 = new TargetClass.TargetClassEquality();
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
        TargetClass.TargetClassEquality obj1 = new TargetClass.TargetClassEquality();
        coverage.update(obj1);

        // 6,6
        TargetClass.TargetClassEquality obj4 = new TargetClass.TargetClassEquality();
        obj4.targetClassEqualityA.targetClassEqualityAA.compClass.a = 6;
        obj4.targetClassEqualityC.compClass.a = 6;
        coverage.update(obj4);
        assert coverage1.merge(coverage).newFormat;

        coverage.update(obj1);
        coverage.update(obj4);

        // 5,2
        TargetClass.TargetClassEquality obj5 = new TargetClass.TargetClassEquality();
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
        TargetClass.TargetClassForEnum obj1 = new TargetClass.TargetClassForEnum();
        coverage.update(obj1);
        assert coverage1.merge(coverage).newFormat;

        TargetClass.TargetClassForEnum obj2 = new TargetClass.TargetClassForEnum();
        obj2.e = TargetClass.TargetEnum.A;
        coverage.update(obj2);
        assert coverage1.merge(coverage).newFormat;

        TargetClass.TargetClassForEnum obj3 = new TargetClass.TargetClassForEnum();
        obj3.e = TargetClass.TargetEnum.A;
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

        TargetClass.TargetClassWithSizeBase targetClassWithSizeBase = new TargetClass.TargetClassWithSizeBase();

        assert objectGraphCoverage.update(targetClassWithSizeBase);

        assert objectGraphCoverage1.merge(objectGraphCoverage).newFormat;

        TargetClass.TargetClassWithSizeBase targetClassWithSizeBase1 = new TargetClass.TargetClassWithSizeBase();
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

        TargetClass.TargetClassAccumulateSizeBase targetClassAccumulateSizeBase = new TargetClass.TargetClassAccumulateSizeBase();

        assert objectGraphCoverage.update(targetClassAccumulateSizeBase);
        assert objectGraphCoverage1.merge(objectGraphCoverage).boundaryChange;

        TargetClass.TargetClassAccumulateSizeBase targetClassAccumulateSizeBase1 = new TargetClass.TargetClassAccumulateSizeBase();
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
        TargetClass.TargetClassInvCombinationBase obj = new TargetClass.TargetClassInvCombinationBase();
        coverage.update(obj);
        coverage.inferInvariant();
        assert coverage1.merge(coverage).newFormat;
        coverage.clear();

        // Test2
        // o1: break inv1
        TargetClass.TargetClassInvCombinationBase obj1 = new TargetClass.TargetClassInvCombinationBase();
        obj1.a.value = 0;
        coverage.update(obj1);
        coverage.inferInvariant();
        assert coverage1.merge(coverage).newFormat;
        coverage.clear();

        // o2: break inv2
        TargetClass.TargetClassInvCombinationBase obj2 = new TargetClass.TargetClassInvCombinationBase();
        obj2.b.value = 1;
        coverage.update(obj2);
        coverage.inferInvariant();
        assert coverage1.merge(coverage).newFormat;
        coverage.clear();

        // o3: break inv1 and inv2 at the same time
        TargetClass.TargetClassInvCombinationBase obj3 = new TargetClass.TargetClassInvCombinationBase();
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
        TargetClass.TargetClassInvCombinationBase obj = new TargetClass.TargetClassInvCombinationBase();
        coverage.update(obj);
        coverage.inferInvariant();
        assert coverage1.merge(coverage).newFormat;
        coverage.clear();

        // Test1: o1: break inv1
        TargetClass.TargetClassInvCombinationBase obj1 = new TargetClass.TargetClassInvCombinationBase();
        obj1.a.value = 0;
        coverage.update(obj1);
        coverage.inferInvariant();
        assert coverage1.merge(coverage).newFormat;
        coverage.clear();

        // Test2: o2: break inv2
        TargetClass.TargetClassInvCombinationBase obj2 = new TargetClass.TargetClassInvCombinationBase();
        obj2.b.value = 1;
        coverage.update(obj2);
        coverage.inferInvariant();
        assert coverage1.merge(coverage).newFormat;
        coverage.clear();

        // Test3: o1 == o2
        TargetClass.TargetClassInvCombinationBase obj3 = new TargetClass.TargetClassInvCombinationBase();
        obj3.a.compClass.a = 4;
        coverage.update(obj3);
        coverage.inferInvariant();
        assert coverage1.merge(coverage).newFormat;
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
        assert coverage1.merge(coverage).newFormat;
        coverage.clear();

        // Test5: o1 == o2
        TargetClass.TargetClassInvCombinationBase obj5 = new TargetClass.TargetClassInvCombinationBase();
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
        TargetClass.TargetClassMultiEqualBase obj1 = new TargetClass.TargetClassMultiEqualBase();
        obj1.b.comp1.a = 4;
        coverage.update(obj1);
        TargetClass.TargetClassMultiEqualBase obj2 = new TargetClass.TargetClassMultiEqualBase();
        coverage.update(obj2);

        coverage.inferInvariant();
        assert coverage1.merge(coverage).newFormat;
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
        assert coverage1.merge(coverage).newFormat;
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
        assert coverage1.merge(coverage).newFormat;
        coverage.clear();

        // add it again?
        TargetClass.TargetClassMultiEqualBase obj7 = new TargetClass.TargetClassMultiEqualBase();
        coverage.update(obj7);
        TargetClass.TargetClassMultiEqualBase obj8 = new TargetClass.TargetClassMultiEqualBase();
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
        TargetClass.TargetClassMultiEqualBase obj1 = new TargetClass.TargetClassMultiEqualBase();
        obj1.b.comp1.a = 4;
        coverage.update(obj1);
        TargetClass.TargetClassMultiEqualBase1 obj2 = new TargetClass.TargetClassMultiEqualBase1();
        coverage.update(obj2);

        coverage.inferInvariant();
        assert coverage1.merge(coverage).newFormat;
        coverage.clear();

        // second equality
        TargetClass.TargetClassMultiEqualBase obj3 = new TargetClass.TargetClassMultiEqualBase();
        obj3.a.comp.a = 4;
        coverage.update(obj3);
        TargetClass.TargetClassMultiEqualBase1 obj4 = new TargetClass.TargetClassMultiEqualBase1();
        coverage.update(obj4);

        coverage.inferInvariant();
        assert coverage1.merge(coverage).newFormat;
        coverage.clear();

        // both equality
        TargetClass.TargetClassMultiEqualBase obj5 = new TargetClass.TargetClassMultiEqualBase();
        coverage.update(obj5);
        TargetClass.TargetClassMultiEqualBase1 obj6 = new TargetClass.TargetClassMultiEqualBase1();
        coverage.update(obj6);

        coverage.inferInvariant();
        assert coverage1.merge(coverage).newFormat;
        coverage.clear();

        // add it again?
        TargetClass.TargetClassMultiEqualBase obj7 = new TargetClass.TargetClassMultiEqualBase();
        coverage.update(obj7);
        TargetClass.TargetClassMultiEqualBase1 obj8 = new TargetClass.TargetClassMultiEqualBase1();
        coverage.update(obj8);

        coverage.inferInvariant();
        assert !coverage1.merge(coverage).newFormat;
        coverage.clear();
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
        Path bassClassPath = Paths.get("input/baseClassInfoForEquality.json");
        Path topObjectsPath = Paths.get("input/topObjectsForEquality.json");
        Path comparableClassesPath = Paths.get("input/comparableClassesForEquality.json");

        ObjectGraphCoverage curCoverage = new ObjectGraphCoverage(bassClassPath, topObjectsPath,
                comparableClassesPath);
        ObjectGraphCoverage allCoverage = new ObjectGraphCoverage(bassClassPath, topObjectsPath,
                comparableClassesPath);

        // test1 (base test)
        TargetClass.TargetClassEqualityA obj1 = new TargetClass.TargetClassEqualityA();
        obj1.targetClassEqualityAA.compClass.a = 5;
        TargetClass.TargetClassEqualityA.staticComp.a = 0;
        curCoverage.update(obj1);

        curCoverage.inferInvariant();
        assert allCoverage.merge(curCoverage, 0).newFormat;
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
        assert !allCoverage.merge(curCoverage, 1).newFormat;
        curCoverage.clear();
    }

    @Test
    public void testFormatCoverageWithStackTrace() {
        /**
         * Two objects with different stack trace but same type, we want to make sure
         * they can be classified.
         */

    }
}
