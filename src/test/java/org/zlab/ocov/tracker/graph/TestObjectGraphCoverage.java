package org.zlab.ocov.tracker.graph;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import org.jgrapht.graph.DirectedMultigraph;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zlab.ocov.tracker.ObjectCoverage;
import org.zlab.ocov.tracker.ObjectGraphCoverage;
import org.zlab.ocov.tracker.Runtime;
import org.zlab.ocov.tracker.TestObjectGraph;
import org.zlab.ocov.tracker.graph.label.LabelConstraint;
import org.zlab.ocov.tracker.graph.label.ValueConstraint;
import org.zlab.ocov.tracker.inv.Invariant;
import org.zlab.ocov.tracker.inv.unary.*;

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

        TestObjectGraph.TargetClassForEnum targetClassForEnum2 = new TestObjectGraph.TargetClassForEnum();
        targetClassForEnum2.e = TestObjectGraph.TargetEnum.A;
        assert objectGraphCoverage.update(targetClassForEnum2);
        assert !objectGraphCoverage.update(targetClassForEnum2);
        assert objectGraphCoverage1.merge(objectGraphCoverage);
        assert !objectGraphCoverage1.merge(objectGraphCoverage);
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
        assert coverage1.merge(coverage);

        TestObjectGraph.TargetClassE obj3 = new TestObjectGraph.TargetClassE();
        TestObjectGraph.TargetClassF1 tmpF31 = new TestObjectGraph.TargetClassF1();
        tmpF31.f1 = 0;
        obj3.fList.add(tmpF31);
        assert (coverage1.update(obj3));
        assert coverage.merge(coverage1);
        // coverage1.objCoverage.get(obj3.getClass().getName()).print();

        TestObjectGraph.TargetClassE obj4 = new TestObjectGraph.TargetClassE();
        TestObjectGraph.TargetClassF1 tmpF41 = new TestObjectGraph.TargetClassF1();
        TestObjectGraph.TargetClassF1 tmpF42 = new TestObjectGraph.TargetClassF1();
        tmpF41.f1 = 0;
        obj4.fList.add(tmpF41);
        tmpF42.f1 = 0;
        obj4.fList.add(tmpF42);
        assert (coverage1.update(obj4));
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
        assert coverage1.merge(coverage);

        TestObjectGraph.TargetClassEquality obj2 = new TestObjectGraph.TargetClassEquality();
        obj2.targetClassEqualityA.targetClassEqualityAA.compClass.a = 3;
        coverage.update(obj2);
        assert coverage1.merge(coverage);
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
        assert coverage1.merge(coverage);

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
        assert coverage1.merge(coverage);

        // 4, 5
        TestObjectGraph.TargetClassEquality obj3 = new TestObjectGraph.TargetClassEquality();
        obj3.targetClassEqualityA.targetClassEqualityAA.compClass.a = 4;
        obj3.targetClassEqualityC.compClass.a = 5;
        coverage.update(obj3);
        assert !coverage1.merge(coverage);

        // obj3--->compClass == obj3--->compClass
        TestObjectGraph.TargetClassEquality obj4 = new TestObjectGraph.TargetClassEquality();
        obj4.targetClassEqualityA.targetClassEqualityAA.compClass.a = 3;
        coverage.update(obj4);
        assert coverage1.merge(coverage);
        assert !coverage1.merge(coverage);

        // test a single graph pattern ser/de
        DirectedMultigraph<GraphPattern.Vertex, GraphPattern.Edge> graph = coverage1.objCoverage
                .get(obj1.getClass().getName()).graph;

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
                .registerSubtype(RestStringSizeOnce.class, "RestStringSizeOnce")
                .registerSubtype(EnumConstant.class, "EnumConstant")
                .registerSubtype(LongLowerBound.class, "LongLowerBound")
                .registerSubtype(LongUpperBound.class, "LongUpperBound");
        Gson gson = new GsonBuilder().registerTypeAdapterFactory(typeFactory1)
                .registerTypeAdapterFactory(typeFactory2).registerTypeAdapterFactory(typeFactory3)
                .registerTypeAdapter(DirectedMultigraph.class, new GraphSerializer())
                .registerTypeAdapter(DirectedMultigraph.class, new GraphDeserializer()).create();
        String jsonStr = gson.toJson(graph);
        System.out.println(jsonStr);
        DirectedMultigraph<GraphPattern.Vertex, GraphPattern.Edge> graphFromGson = gson.fromJson(
                jsonStr,
                new TypeToken<DirectedMultigraph<GraphPattern.Vertex, GraphPattern.Edge>>() {
                }.getType());
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
        assert coverage1.merge(coverage);

        // 6,4
        TestObjectGraph.TargetClassEquality obj4 = new TestObjectGraph.TargetClassEquality();
        obj4.targetClassEqualityA.targetClassEqualityAA.compClass.a = 6;
        obj4.targetClassEqualityC.compClass.a = 4;
        coverage.update(obj4);

        assert !coverage1.merge(coverage);

        // 9, 4
        TestObjectGraph.TargetClassEquality obj5 = new TestObjectGraph.TargetClassEquality();
        obj5.targetClassEqualityA.targetClassEqualityAA.compClass.a = 9;
        obj5.targetClassEqualityC.compClass.a = 4;
        coverage.update(obj5);
        assert coverage1.merge(coverage);
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
        assert coverage1.merge(coverage);

        TestObjectGraph.TargetClassForEnum obj2 = new TestObjectGraph.TargetClassForEnum();
        obj2.e = TestObjectGraph.TargetEnum.A;
        coverage.update(obj2);
        assert coverage1.merge(coverage);

        TestObjectGraph.TargetClassForEnum obj3 = new TestObjectGraph.TargetClassForEnum();
        obj3.e = TestObjectGraph.TargetEnum.A;
        coverage.update(obj3);
        assert !coverage1.merge(coverage);
    }

    @Test
    public void test() {
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

}
