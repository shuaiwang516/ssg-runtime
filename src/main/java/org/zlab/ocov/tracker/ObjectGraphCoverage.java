package org.zlab.ocov.tracker;

// import com.google.gson.Gson;
// import com.google.gson.GsonBuilder;
// import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import org.apache.commons.lang3.SerializationUtils;
import org.jgrapht.graph.DirectedMultigraph;
import org.zlab.ocov.Utils;
import org.zlab.ocov.tracker.graph.*;
import org.zlab.ocov.tracker.graph.label.LabelConstraint;
import org.zlab.ocov.tracker.graph.label.ValueConstraint;
import org.zlab.ocov.tracker.graph.structure.AccumulatedSizeConstraint;
import org.zlab.ocov.tracker.graph.structure.InDegreeConstraint;
import org.zlab.ocov.tracker.graph.structure.OutDegreeConstraint;
import org.zlab.ocov.tracker.graph.structure.StructureConstraint;
import org.zlab.ocov.tracker.inv.Invariant;
import org.zlab.ocov.tracker.inv.unary.*;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.*;

public class ObjectGraphCoverage implements Serializable {
    private static final long serialVersionUID = 20231215L;

    // ----------------------- Debug -----------------------
    public final static boolean useFixedObjectGraph = false;

    // ----------------------- Config -----------------------
    public final static boolean enableInvariantCombination = false;
    public final static boolean avoidObjectGraphDump = true;
    // If object with the same addr occur twice, avoid processing it
    public final static boolean avoidRecordObjectWithSameAddress = false;

    // Core coverage
    // Only contain the top level objects: class name -> class info
    public Map<Integer, Map<String, GraphPattern>> dumpId2ObjCoverage = new HashMap<>();
    // public Map<String, GraphPattern> objCoverage = new HashMap<>();

    public Set<Integer> visitedObjects = new HashSet<>();

    public Map<String, GraphPattern> baseClassInfo;
    public Set<String> topObjects;

    public EqualitySet equalitySet;
    public IsSerialize isSerialized;
    public InvariantCombination invariantCombination;

    // FIXME: this is not merged
    public Boundary boundary;

    // Graph Implementation
    ObjectGraphDumper objectGraphDumper;

    long totalTime1 = 0;
    long totalTime2 = 0;
    long totalTime3 = 0;

    int dumpedObjectCount = 0;
    int dupObjectCount = 0;

    Map<String, Integer> classDupCount = new HashMap<>();
    Map<String, Integer> classDumpCount = new HashMap<>();

    // Debug
    ObjectGraph tmpObjectGraph;

    public ObjectGraphCoverage() {
        // for json
    }

    public ObjectGraphCoverage(Path baseClassInfoPath, Path topObjectsPath) {
        this(baseClassInfoPath, topObjectsPath, null, null, null, null);
    }

    public ObjectGraphCoverage(Path baseClassInfoPath, Path topObjectsPath,
            Path comparableClassesPath) {
        this(baseClassInfoPath, topObjectsPath, comparableClassesPath, null, null, null);
    }

    public ObjectGraphCoverage(Path baseClassInfoPath, Path topObjectsPath,
            Path comparableClassesPath, Path modifiedFieldsPath, Path modifiedEnumsPath) {
        this(baseClassInfoPath, topObjectsPath, comparableClassesPath, modifiedFieldsPath,
                modifiedEnumsPath, null);
    }

    public ObjectGraphCoverage(Path baseClassInfoPath, Path topObjectsPath,
            Path comparableClassesPath, Path modifiedFieldsPath, Path modifiedEnumsPath,
            Path branch2CollectionPath) {
        Map<String, Map<String, String>> classInfoOri = Utils
                .loadMapFromFile(baseClassInfoPath.toString());
        baseClassInfo = GraphPattern.createGraphPatterns(classInfoOri);
        topObjects = Utils.loadSetFromFile(topObjectsPath.toString());
        Set<String> comparableClasses = null;
        if (comparableClassesPath != null && comparableClassesPath.toFile().exists()) {
            comparableClasses = Utils.loadSetFromFile(comparableClassesPath.toString());
        }
        if (comparableClasses != null) {
            equalitySet = new EqualitySet(comparableClasses);
            objectGraphDumper = new ObjectGraphDumper(classInfoOri, comparableClasses);
        } else {
            objectGraphDumper = new ObjectGraphDumper(classInfoOri);
        }
        if (modifiedFieldsPath != null && modifiedFieldsPath.toFile().exists()
                && modifiedEnumsPath != null && modifiedEnumsPath.toFile().exists()) {
            isSerialized = constructIsSerialize(modifiedFieldsPath, modifiedEnumsPath);
        }
        boundary = new Boundary();
        if (enableInvariantCombination)
            invariantCombination = new InvariantCombination();
    }

    public GraphPattern getGraphPattern(String className, int dumpId) {
        if (!dumpId2ObjCoverage.containsKey(dumpId)) {
            dumpId2ObjCoverage.put(dumpId, new HashMap<>());
        }
        Map<String, GraphPattern> objCoverage = dumpId2ObjCoverage.get(dumpId);
        if (!objCoverage.containsKey(className)) {
            objCoverage.put(className, SerializationUtils.clone(baseClassInfo.get(className)));
        }
        return objCoverage.get(className);
    }

    public boolean update(Object obj) {
        return update(obj, -1);
    }

    public boolean update(Object obj, int dumpId) {
        if (obj == null)
            return false;
        String className = obj.getClass().getName();
        if (!topObjects.contains(className) || !baseClassInfo.containsKey(className))
            return false;

        int objId = System.identityHashCode(obj);
        if (avoidRecordObjectWithSameAddress) {
            if (visitedObjects.contains(objId)) {
                return false;
            }
            visitedObjects.add(objId);
        }

        GraphPattern classInfo = getGraphPattern(className, dumpId);
        if (classInfo == null)
            throw new RuntimeException("ClassInfo not found for " + className);
        // long time1 = System.nanoTime();
        boolean ret;
        Set<String> brokenInvs = new HashSet<>();
        LogInfo logInfo = new LogInfo(dumpId);

        if (avoidObjectGraphDump) {
            ret = classInfo.update(obj, baseClassInfo, logInfo, equalitySet, isSerialized,
                    brokenInvs, objId);
        } else {
            ObjectGraph objectGraph;

            if (useFixedObjectGraph) {
                if (tmpObjectGraph == null) {
                    tmpObjectGraph = objectGraphDumper.dump(obj, dumpId);
                }
                objectGraph = tmpObjectGraph;
            } else {
                objectGraph = objectGraphDumper.dump(obj, dumpId);
            }

            // long time2 = System.nanoTime();

            ret = classInfo.update(objectGraph, baseClassInfo, logInfo, equalitySet, isSerialized,
                    brokenInvs);
        }

        if (!brokenInvs.isEmpty() && enableInvariantCombination) {
            invariantCombination.record(objId, brokenInvs);
        }

        // long time3 = System.nanoTime();

        if (equalitySet != null)
            equalitySet.dumpSameObjectGraph(dumpId, objId);

        // long time4 = System.nanoTime();

        // totalTime1 += time2 - time1;
        // totalTime2 += time3 - time2;
        // totalTime3 += time4 - time3;
        //
        // Runtime.log(String.format("Time1: %d ms, Time2: %d ms, Time3: %d ms" +
        // "", totalTime1/1_000_000, totalTime2/1_000_000, totalTime3/1_000_000));

        // debugLog();
        return ret;
    }

    // ----Boundary Related----
    public boolean updateBranch(Object obj, int id) {
        if (boundary == null)
            return false;
        return boundary.updateBranch(obj, id);
    }

    public boolean updateBranch(Object lhsOp, Object rhsOp, String operator, int dumpId) {
        if (boundary == null)
            return false;
        return boundary.updateBranch(lhsOp, rhsOp, operator, dumpId);
    }

    public void inferInvariant() {
        // this should be invoked for every test
        if (equalitySet != null)
            equalitySet.infer();
        if (enableInvariantCombination)
            invariantCombination.infer(equalitySet);
    }

    public void clear() {
        // try to separate format coverage across tests
        visitedObjects.clear();
        // dumpedObjectCount = 0;
        // dupObjectCount = 0;
        if (equalitySet != null)
            equalitySet.clear();
        if (isSerialized != null)
            isSerialized.clear();
        if (enableInvariantCombination)
            invariantCombination.clear();
        boundary.clear();
    }

    // Only record, and infer at last
    List<ObjectGraph> objectGraphs = new ArrayList<>();

    public boolean dump(Object obj, int dumpId) {
        if (obj == null)
            return false;
        String className = obj.getClass().getName();
        Integer objId = System.identityHashCode(obj);
        if (avoidRecordObjectWithSameAddress) {
            if (visitedObjects.contains(objId)) {
                return false;
            }
            visitedObjects.add(objId);
        }

        GraphPattern classInfo = getGraphPattern(className, dumpId);
        if (classInfo == null)
            return false;

        ObjectGraph objectGraph = objectGraphDumper.dump(obj, dumpId);

        // Testing
        // objectGraphs.clear();

        objectGraphs.add(objectGraph);
        return true;
    }

    public void debugLog() {
        Runtime.log(String.format("Dumped object count: %d, Dup object count: %d",
                dumpedObjectCount, dupObjectCount));
        // print classDupCount, sorted with the value and then print from max to min
        // Convert the map to a list of entries
        Runtime.log("Class Dup count:");
        List<Map.Entry<String, Integer>> list = new ArrayList<>(classDupCount.entrySet());
        list.sort((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));
        for (Map.Entry<String, Integer> entry : list) {
            Runtime.log(entry.getKey() + ": " + entry.getValue());
        }
        Runtime.log("");

        Runtime.log("Class Dump count:");
        list = new ArrayList<>(classDumpCount.entrySet());
        list.sort((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));
        for (Map.Entry<String, Integer> entry : list) {
            Runtime.log(entry.getKey() + ": " + entry.getValue());
        }
        Runtime.log("");

        Map<Integer, String> timesMap = new HashMap<>();
        for (String cName : classDupCount.keySet()) {
            timesMap.put(classDupCount.get(cName) / classDumpCount.get(cName), cName);
        }

        Runtime.log("Ratio:");
        timesMap.entrySet().stream().sorted(Map.Entry.<Integer, String>comparingByKey().reversed())
                .forEach(entry -> Runtime.log(entry.getKey() + ": " + entry.getValue()));

        Runtime.log("");
    }

    public FormatCoverageStatus merge(ObjectGraphCoverage otherObjCoverage) {
        return merge(otherObjCoverage, -1);
    }

    public FormatCoverageStatus merge(ObjectGraphCoverage otherObjCoverage, int testId) {
        FormatCoverageStatus formatCoverageStatus = new FormatCoverageStatus();
        if (otherObjCoverage == null)
            return formatCoverageStatus;

        for (int dumpId : otherObjCoverage.dumpId2ObjCoverage.keySet()) {
            Map<String, GraphPattern> otherClassInfo = otherObjCoverage.dumpId2ObjCoverage
                    .get(dumpId);
            if (otherClassInfo == null)
                continue;

            if (!dumpId2ObjCoverage.containsKey(dumpId)) {
                dumpId2ObjCoverage.put(dumpId, new HashMap<>());
                for (String className : otherClassInfo.keySet()) {
                    GraphPattern otherGraphPattern = otherClassInfo.get(className);
                    if (otherGraphPattern == null)
                        continue;
                    dumpId2ObjCoverage.get(dumpId).put(className,
                            SerializationUtils.clone(otherGraphPattern));
                }
                formatCoverageStatus.newFormat = true;
                continue;
            }

            Map<String, GraphPattern> classInfo = dumpId2ObjCoverage.get(dumpId);
            for (String className : otherClassInfo.keySet()) {
                GraphPattern otherGraphPattern = otherClassInfo.get(className);
                if (otherGraphPattern == null)
                    continue;
                GraphPattern graphPattern = classInfo.get(className);
                if (graphPattern == null) {
                    Runtime.log("[hklog] Add new graphPattern for " + className);
                    classInfo.put(className, SerializationUtils.clone(otherGraphPattern));
                    formatCoverageStatus.newFormat = true;
                } else {
                    formatCoverageStatus.incorporate(graphPattern.merge(otherGraphPattern));
                }
            }
        }
        if (equalitySet == null) {
            if (otherObjCoverage.equalitySet != null) {
                equalitySet = SerializationUtils.clone(otherObjCoverage.equalitySet);
                formatCoverageStatus.newFormat = true;
            }
        } else {
            if (equalitySet.merge(otherObjCoverage.equalitySet)) {
                formatCoverageStatus.newFormat = true;
            }
        }
        if (isSerialized == null) {
            if (otherObjCoverage.isSerialized != null) {
                isSerialized = SerializationUtils.clone(otherObjCoverage.isSerialized);
                formatCoverageStatus.newFormat = true;
            }
        } else {
            if (isSerialized.merge(otherObjCoverage.isSerialized)) {
                formatCoverageStatus.newFormat = true;
            }
        }
        if (enableInvariantCombination
                && invariantCombination.merge(otherObjCoverage.invariantCombination)) {
            formatCoverageStatus.newFormat = true;
        }
        // Data boundary
        if (boundary == null) {
            if (otherObjCoverage.boundary != null) {
                boundary = SerializationUtils.clone(otherObjCoverage.boundary);
                formatCoverageStatus.boundaryChange = true;
            }
        } else {
            if (boundary.merge(otherObjCoverage.boundary)) {
                formatCoverageStatus.boundaryChange = true;
            }
        }
        if (formatCoverageStatus.isChanged()) {
            Runtime.log(
                    String.format("[hklog] --- Merged new coverage from testId: %d ---", testId));
        }
        return formatCoverageStatus;
    }

    public static IsSerialize constructIsSerialize(Path modifiedFieldsPath,
            Path modifiedEnumsPath) {
        Map<String, Set<String>> modifiedFields = Utils
                .loadModifiedFields(modifiedFieldsPath.toString());
        Set<String> modifiedEnums = Utils.loadSetFromFile(modifiedEnumsPath.toString());
        return new IsSerialize(modifiedFields, modifiedEnums);
    }

    // public static Gson constructGson() {
    // RuntimeTypeAdapterFactory<LabelConstraint> typeFactory1 =
    // RuntimeTypeAdapterFactory
    // .of(LabelConstraint.class, "LabelConstraint")
    // .registerSubtype(ValueConstraint.class, "ValueConstraint");
    //
    // RuntimeTypeAdapterFactory<Invariant> typeFactory2 = RuntimeTypeAdapterFactory
    // .of(Invariant.class, "Invariant")
    // .registerSubtype(UnaryInvariant.class, "UnaryInvariant");
    // RuntimeTypeAdapterFactory<UnaryInvariant> typeFactory3 =
    // RuntimeTypeAdapterFactory
    // .of(UnaryInvariant.class, "UnaryInvariant")
    // .registerSubtype(IntegerLowerBound.class, "IntegerLowerBound")
    // .registerSubtype(IntegerUpperBound.class, "IntegerUpperBound")
    // .registerSubtype(EmptyStringOnce.class, "EmptyStringOnce")
    // .registerSubtype(TrueOnce.class, "TrueOnce")
    // .registerSubtype(RestOnce.class, "RestOnce")
    // .registerSubtype(FalseOnce.class, "FalseOnce")
    // .registerSubtype(NegativeOneOnce.class, "NegativeOneOnce")
    // .registerSubtype(OneCharStringOnce.class, "OneCharStringOnce")
    // .registerSubtype(NullOnce.class, "NullOnce")
    // .registerSubtype(OneOnce.class, "OneOnce")
    // .registerSubtype(ZeroOnce.class, "ZeroOnce")
    // .registerSubtype(RestStringSizeOnce.class, "RestStringSizeOnce")
    // .registerSubtype(EnumConstant.class, "EnumConstant")
    // .registerSubtype(LongLowerBound.class, "LongLowerBound")
    // .registerSubtype(LongUpperBound.class, "LongUpperBound");
    //
    // RuntimeTypeAdapterFactory<StructureConstraint> typeFactory4 =
    // RuntimeTypeAdapterFactory
    // .of(StructureConstraint.class, "StructureConstraint")
    // .registerSubtype(InDegreeConstraint.class, "InDegreeConstraint")
    // .registerSubtype(OutDegreeConstraint.class, "OutDegreeConstraint")
    // .registerSubtype(AccumulatedSizeConstraint.class,
    // "AccumulatedSizeConstraint");
    //
    // return new GsonBuilder().registerTypeAdapterFactory(typeFactory1)
    // .registerTypeAdapterFactory(typeFactory2).registerTypeAdapterFactory(typeFactory3)
    // .registerTypeAdapterFactory(typeFactory4)
    // .registerTypeAdapter(DirectedMultigraph.class, new GraphSerializer())
    // .registerTypeAdapter(DirectedMultigraph.class, new
    // GraphDeserializer()).create();
    // }

}
