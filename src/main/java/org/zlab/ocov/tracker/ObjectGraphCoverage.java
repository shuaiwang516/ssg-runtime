package org.zlab.ocov.tracker;

import org.apache.commons.lang3.SerializationUtils;
import org.zlab.ocov.Utils;
import org.zlab.ocov.tracker.graph.*;
import org.zlab.ocov.tracker.inv.unary.*;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.*;

public class ObjectGraphCoverage implements Serializable {
    private static final long serialVersionUID = 20231215L;

    // ----------------------- Config -----------------------
    public final static boolean enableInvariantCombination = false;
    // If object with the same addr occur twice, avoid processing it
    public final static boolean avoidRecordObjectWithSameAddress = false;

    // DumpId -> class name -> graph pattern
    public Map<Integer, Map<String, GraphPattern>> dumpId2ObjCoverage = new HashMap<>();
    // Dump id -> ArgId -> classname -> graph pattern: this could include non-top
    // objects
    public Map<Integer, Map<Integer, Map<String, GraphPattern>>> dumpId2ContextObjCoverage = new HashMap<>();

    public Set<Integer> visitedObjects = new HashSet<>();

    public Map<String, GraphPattern> baseClassInfo;
    public Set<String> topObjects;

    public EqualitySet equalitySet;
    public IsSerialize isSerialized;
    public InvariantCombination invariantCombination;

    public Boundary boundary;

    // Graph Implementation
    ObjectGraphDumper objectGraphDumper;

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

    public boolean update(Object obj, int dumpId, Object... contextArgs) {
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

        boolean changed = false;
        if (updateTopObjectGraphPattern(dumpId, obj, className, objId))
            changed = true;
        if (updateContextGraphPattern(dumpId, contextArgs))
            changed = true;

        // debugLog();
        return changed;
    }

    public boolean updateTopObjectGraphPattern(int dumpId, Object obj, String className,
            int objId) {
        GraphPattern classInfo = getGraphPattern(className, dumpId);
        if (classInfo == null)
            throw new RuntimeException("ClassInfo not found for " + className);

        Set<String> brokenInvs = new HashSet<>();
        LogInfo logInfo = new LogInfo(dumpId);

        boolean changed = classInfo.update(obj, baseClassInfo, logInfo, equalitySet, isSerialized,
                brokenInvs, objId);

        if (!brokenInvs.isEmpty() && enableInvariantCombination) {
            invariantCombination.record(objId, brokenInvs);
        }

        if (equalitySet != null)
            equalitySet.dumpSameObjectGraph(dumpId, objId);

        return changed;
    }

    public boolean updateContextGraphPattern(int dumpId, Object... contextArgs) {
        if (contextArgs == null || contextArgs.length == 0)
            return false;
        boolean changed = false;
        if (!dumpId2ContextObjCoverage.containsKey(dumpId)) {
            dumpId2ContextObjCoverage.put(dumpId, new HashMap<>());
        }
        Map<Integer, Map<String, GraphPattern>> contextObjCoverage = dumpId2ContextObjCoverage
                .get(dumpId);
        for (int i = 0; i < contextArgs.length; i++) {
            Object contextObj = contextArgs[i];
            if (contextObj == null)
                continue;
            String className = contextObj.getClass().getName();
            if (!baseClassInfo.containsKey(className))
                continue;
            int objId = System.identityHashCode(contextObj);
            if (avoidRecordObjectWithSameAddress) {
                if (visitedObjects.contains(objId)) {
                    continue;
                }
                visitedObjects.add(objId);
            }
            if (!contextObjCoverage.containsKey(i)) {
                contextObjCoverage.put(i, new HashMap<>());
            }
            Map<String, GraphPattern> classInfo = contextObjCoverage.get(i);
            if (!classInfo.containsKey(className)) {
                classInfo.put(className, SerializationUtils.clone(baseClassInfo.get(className)));
            }
            GraphPattern graphPattern = classInfo.get(className);
            Set<String> brokenInvs = new HashSet<>();
            LogInfo logInfo = new LogInfo(dumpId);
            changed |= graphPattern.update(contextObj, baseClassInfo, logInfo, equalitySet,
                    isSerialized, brokenInvs, objId);
            if (!brokenInvs.isEmpty() && enableInvariantCombination) {
                invariantCombination.record(objId, brokenInvs);
            }
            if (equalitySet != null)
                equalitySet.dumpSameObjectGraph(dumpId, objId);
        }
        return changed;
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
        // Separate format coverage across tests
        visitedObjects.clear();
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

        mergeTopGraphPattern(otherObjCoverage, formatCoverageStatus);
        mergeContextGraphPattern(otherObjCoverage, formatCoverageStatus);
        mergeSpecialInvariant(otherObjCoverage, formatCoverageStatus);

        if (formatCoverageStatus.isChanged()) {
            Runtime.log(
                    String.format("[hklog] --- Merged new coverage from testId: %d ---", testId));
        }
        return formatCoverageStatus;
    }

    private void mergeTopGraphPattern(ObjectGraphCoverage otherObjCoverage,
            FormatCoverageStatus formatCoverageStatus) {
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
    }

    private void mergeContextGraphPattern(ObjectGraphCoverage otherObjCoverage,
            FormatCoverageStatus formatCoverageStatus) {
        for (int dumpId : otherObjCoverage.dumpId2ContextObjCoverage.keySet()) {
            Map<Integer, Map<String, GraphPattern>> otherContextObjCoverage = otherObjCoverage.dumpId2ContextObjCoverage
                    .get(dumpId);
            if (otherContextObjCoverage == null)
                continue;

            if (!dumpId2ContextObjCoverage.containsKey(dumpId)) {
                dumpId2ContextObjCoverage.put(dumpId, new HashMap<>());
                for (int argId : otherContextObjCoverage.keySet()) {
                    Map<String, GraphPattern> otherClassInfo = otherContextObjCoverage.get(argId);
                    if (otherClassInfo == null)
                        continue;
                    for (String className : otherClassInfo.keySet()) {
                        GraphPattern otherGraphPattern = otherClassInfo.get(className);
                        if (otherGraphPattern == null)
                            continue;
                        dumpId2ContextObjCoverage.get(dumpId).put(argId, new HashMap<>());
                        dumpId2ContextObjCoverage.get(dumpId).get(argId).put(className,
                                SerializationUtils.clone(otherGraphPattern));
                    }
                }
                formatCoverageStatus.newFormat = true;
                continue;
            }

            Map<Integer, Map<String, GraphPattern>> contextObjCoverage = dumpId2ContextObjCoverage
                    .get(dumpId);
            for (int argId : otherContextObjCoverage.keySet()) {
                Map<String, GraphPattern> otherClassInfo = otherContextObjCoverage.get(argId);
                if (otherClassInfo == null)
                    continue;
                if (!contextObjCoverage.containsKey(argId)) {
                    contextObjCoverage.put(argId, new HashMap<>());
                }
                Map<String, GraphPattern> classInfo = contextObjCoverage.get(argId);
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
        }
    }

    private void mergeSpecialInvariant(ObjectGraphCoverage otherObjCoverage,
            FormatCoverageStatus formatCoverageStatus) {
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
        // Boundary
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
    }

    public static IsSerialize constructIsSerialize(Path modifiedFieldsPath,
            Path modifiedEnumsPath) {
        Map<String, Set<String>> modifiedFields = Utils
                .loadModifiedFields(modifiedFieldsPath.toString());
        Set<String> modifiedEnums = Utils.loadSetFromFile(modifiedEnumsPath.toString());
        return new IsSerialize(modifiedFields, modifiedEnums);
    }
}
