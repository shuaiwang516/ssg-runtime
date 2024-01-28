package org.zlab.ocov.tracker.graph;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import org.jgrapht.graph.DirectedMultigraph;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zlab.ocov.tracker.ObjectGraphCoverage;
import org.zlab.ocov.tracker.Runtime;
import org.zlab.ocov.tracker.TestObjectGraph;
import org.zlab.ocov.tracker.graph.label.LabelConstraint;
import org.zlab.ocov.tracker.graph.label.ValueConstraint;
import org.zlab.ocov.tracker.inv.Invariant;
import org.zlab.ocov.tracker.inv.unary.*;

import java.nio.file.Path;
import java.nio.file.Paths;

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
        /**
         * Conditions Same object graph equality Cond1: Should be captured by equality
         * across multiple object graphs obj1.a obj2.b
         *
         * Cond2: Should be captured by equality within the same object graph obj3.a
         * obj4.b
         */
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

        RuntimeTypeAdapterFactory<LabelConstraint> typeFactory = RuntimeTypeAdapterFactory
                .of(LabelConstraint.class, "LabelConstraint") // "type" is a field in JSON that
                                                              // tells us what the
                .registerSubtype(ValueConstraint.class, "ValueConstraint");

        /**
         * UnaryInvariant (org.zlab.ocov.tracker.inv.unary) IntegerUpperBound
         * (org.zlab.ocov.tracker.inv.unary) IntegerLowerBound
         * (org.zlab.ocov.tracker.inv.unary)
         *
         * EmptyStringOnce (org.zlab.ocov.tracker.inv.unary) TrueOnce
         * (org.zlab.ocov.tracker.inv.unary) RestOnce (org.zlab.ocov.tracker.inv.unary)
         * FalseOnce (org.zlab.ocov.tracker.inv.unary) NegativeOneOnce
         * (org.zlab.ocov.tracker.inv.unary) OneCharStringOnce
         * (org.zlab.ocov.tracker.inv.unary) NullOnce (org.zlab.ocov.tracker.inv.unary)
         * OneOnce (org.zlab.ocov.tracker.inv.unary) ZeroOnce
         * (org.zlab.ocov.tracker.inv.unary) RestStringSizeOnce
         * (org.zlab.ocov.tracker.inv.unary) EnumConstant
         * (org.zlab.ocov.tracker.inv.unary) LongLowerBound
         * (org.zlab.ocov.tracker.inv.unary) LongUpperBound
         * (org.zlab.ocov.tracker.inv.unary)
         */
        RuntimeTypeAdapterFactory<Invariant> typeFactory1 = RuntimeTypeAdapterFactory
                .of(Invariant.class, "Invariant") // "type" is a field in JSON that tells us what
                                                  // the
                .registerSubtype(UnaryInvariant.class, "UnaryInvariant");
        RuntimeTypeAdapterFactory<UnaryInvariant> typeFactory2 = RuntimeTypeAdapterFactory
                .of(UnaryInvariant.class, "UnaryInvariant") // "type" is a field in JSON that tells
                                                            // us what the
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

        Gson gson = new GsonBuilder().registerTypeAdapterFactory(typeFactory)
                .registerTypeAdapterFactory(typeFactory1).registerTypeAdapterFactory(typeFactory2)
                .registerTypeAdapter(DirectedMultigraph.class, new GraphSerializer())
                .registerTypeAdapter(DirectedMultigraph.class, new GraphDeserializer()).create();

        String json = gson.toJson(graph);
        System.out.println(json);

        // DirectedMultigraph<GraphPattern.Vertex, GraphPattern.Edge> graphFromGson =
        // gson.fromJson(json, new TypeToken<DirectedMultigraph<GraphPattern.Vertex,
        // GraphPattern.Edge>>(){}.getType());

        // Serialize coverage1 to a file and deserialize it back using GSON
        // RuntimeTypeAdapterFactory<TypeInfo> typeFactory = RuntimeTypeAdapterFactory
        // .of(Supplier.class, "supplier") // "type" is a field in JSON that tells us
        // what the
        // // actual type is
        // .registerSubtype(ObjectGraph.Edge.class, "array")
        // .registerSubtype(BooleanType.class, "boolean")
        // .registerSubtype(CollectionType.class, "collection")
        // .registerSubtype(DoubleType.class, "double")
        // .registerSubtype(FloatType.class, "float")
        // .registerSubtype(IntegerType.class, "integer")
        // .registerSubtype(LongType.class, "long").registerSubtype(ObjectType.class,
        // "object")
        // .registerSubtype(ShortType.class, "short")
        // .registerSubtype(StringType.class, "string");

        // Gson gson = new
        // GsonBuilder().registerTypeAdapterFactory(typeFactory).create();
        // String jsonStr = gson.toJson(coverage1);
        // System.out.println(jsonStr);
        // ObjectGraphCoverage coverage2 = gson.fromJson(jsonStr,
        // ObjectGraphCoverage.class);

        /**
         * Unable to create instance of interface java.util.function.Supplier.
         * Registering an InstanceCreator or a TypeAdapter for this type, or adding a
         * no-args constructor may fix this problem. java.lang.RuntimeException: Unable
         * to create instance of interface java.util.function.Supplier. Registering an
         * InstanceCreator or a TypeAdapter for this type, or adding a no-args
         * constructor may fix this problem. at
         * com.google.gson.internal.ConstructorConstructor$16.construct(ConstructorConstructor.java:275)
         * at
         * com.google.gson.internal.bind.ReflectiveTypeAdapterFactory$Adapter.read(ReflectiveTypeAdapterFactory.java:211)
         * at
         * com.google.gson.internal.bind.ReflectiveTypeAdapterFactory$1.read(ReflectiveTypeAdapterFactory.java:130)
         * at
         * com.google.gson.internal.bind.ReflectiveTypeAdapterFactory$Adapter.read(ReflectiveTypeAdapterFactory.java:221)
         * at
         * com.google.gson.internal.bind.ReflectiveTypeAdapterFactory$1.read(ReflectiveTypeAdapterFactory.java:130)
         * at
         * com.google.gson.internal.bind.ReflectiveTypeAdapterFactory$Adapter.read(ReflectiveTypeAdapterFactory.java:221)
         * at
         * com.google.gson.internal.bind.TypeAdapterRuntimeTypeWrapper.read(TypeAdapterRuntimeTypeWrapper.java:41)
         */

        // assert !coverage2.merge(coverage1);
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
    public void test() {
        DirectedMultigraph<GraphPattern.Vertex, GraphPattern.Edge> graph = new DirectedMultigraph<>(
                GraphPattern.Edge.class);
        GraphPattern.Vertex v = GraphPattern.createBaseVertex("a");
        graph.addVertex(v);

        assert graph.containsVertex(v);

        GraphPattern.Vertex v1 = GraphPattern.createBaseVertex("a");
        // assert graph.containsVertex(v1);
    }

}
