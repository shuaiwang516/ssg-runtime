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
    public final static boolean useContextFromArgs = true;
    public final static boolean collectContextGraphPattern = true;

    // DumpId -> classname -> graph pattern (Only top objects)
    public Map<Integer, Map<String, Map<String, GraphPattern>>> dumpId2ObjCoverageWithContext = new HashMap<>();
    public Map<Integer, Map<String, Map<String, GraphPattern>>> dumpId2ContextObjCoverageWithContext = new HashMap<>();

    public Set<Integer> visitedObjects = new HashSet<>();

    // Creation context
    private final Map<Integer, Integer> objAddress2TopObjAddress = new HashMap<>();
    private final Map<Integer, String> topObj2CreationStacktrace = new HashMap<>();

    private Map<String, Map<String, String>> classInfoOri;
    public Map<String, GraphPattern> baseClassInfo;
    public Set<String> topObjects;

    public EqualitySet equalitySet;
    public IsSerialize isSerialized;
    public Boundary boundary;
    public InvariantCombination invariantCombination;

    ObjectGraphDumper objectGraphDumper;

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
        classInfoOri = Utils.loadMapFromFile(baseClassInfoPath.toString());
        topObjects = Utils.loadSetFromFile(topObjectsPath.toString());
        baseClassInfo = GraphPattern.createGraphPatterns(classInfoOri);
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

    public GraphPattern getGraphPattern(String className, int dumpId, String context) {
        if (!dumpId2ObjCoverageWithContext.containsKey(dumpId)) {
            dumpId2ObjCoverageWithContext.put(dumpId, new HashMap<>());
        }
        Map<String, Map<String, GraphPattern>> contextObjCoverage = dumpId2ObjCoverageWithContext
                .get(dumpId);
        if (!contextObjCoverage.containsKey(context)) {
            contextObjCoverage.put(context, new HashMap<>());
        }
        Map<String, GraphPattern> objCoverage = contextObjCoverage.get(context);
        if (!objCoverage.containsKey(className)) {
            objCoverage.put(className, SerializationUtils.clone(baseClassInfo.get(className)));
        }
        return objCoverage.get(className);
    }

    public GraphPattern getContextGraphPattern(String className, int dumpId, String context) {
        if (!dumpId2ContextObjCoverageWithContext.containsKey(dumpId)) {
            dumpId2ContextObjCoverageWithContext.put(dumpId, new HashMap<>());
        }
        Map<String, Map<String, GraphPattern>> contextObjCoverage = dumpId2ContextObjCoverageWithContext
                .get(dumpId);
        if (!contextObjCoverage.containsKey(context)) {
            contextObjCoverage.put(context, new HashMap<>());
        }
        Map<String, GraphPattern> objCoverage = contextObjCoverage.get(context);
        if (!objCoverage.containsKey(className)) {
            objCoverage.put(className, SerializationUtils.clone(baseClassInfo.get(className)));
        }
        return objCoverage.get(className);
    }

    public void monitorCreationContext(Object obj) {
        if (obj == null)
            return;
        // long time1 = System.currentTimeMillis();
        String className = obj.getClass().getName();
        if (!topObjects.contains(className) || !baseClassInfo.containsKey(className)) {
            return;
        }
        int topAddr = System.identityHashCode(obj);
        ObjectGraphTraverser objectGraphTraverser = new ObjectGraphTraverser(classInfoOri);
        objectGraphTraverser.traverse(obj);
        Set<Integer> visited = objectGraphTraverser.getVisited();

        // update obj2TopObj
        for (int addr : visited) {
            objAddress2TopObjAddress.put(addr, topAddr);
        }
        // long time2 = System.currentTimeMillis();

        // update topObj2CreationStacktrace
        topObj2CreationStacktrace.put(topAddr, Utils.getStackTrace());
        // long time3 = System.currentTimeMillis();
        // Runtime.log("Traverse time= " + (time2 - time1) / 1000. + "s, stacktrace time
        // = "
        // + (time3 - time2) / 1000. + "s");
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
        if (collectContextGraphPattern) {
            for (Object contextObj : contextArgs) {
                String contextClassName = contextObj.getClass().getName();
                if (!baseClassInfo.containsKey(contextClassName))
                    continue;
                updateContextObjectGraphPattern(dumpId, contextObj, contextClassName,
                        System.identityHashCode(contextObj));
            }
        }
        String context = getCreationContextFromArgs(contextArgs);
        if (updateTopObjectGraphPattern(dumpId, context, obj, className, objId))
            changed = true;

        // debugLog();
        return changed;
    }

    private String getCreationContextFromArgs(Object... contextArgs) {
        if (!useContextFromArgs)
            return "";
        StringBuilder sb = new StringBuilder();
        for (Object contextObj : contextArgs) {
            sb.append(getCreationContext(contextObj));
        }
        return sb.toString();
    }

    private String getCreationContext(Object obj) {
        if (obj == null)
            return "";

        String className = obj.getClass().getName();
        if (!baseClassInfo.containsKey(className))
            return "";
        int addr = System.identityHashCode(obj);
        if (objAddress2TopObjAddress.containsKey(addr)
                && topObj2CreationStacktrace.containsKey(objAddress2TopObjAddress.get(addr))) {
            return topObj2CreationStacktrace.get(objAddress2TopObjAddress.get(addr));
        }
        return "";
    }

    public boolean updateTopObjectGraphPattern(int dumpId, String context, Object obj,
            String className, int objId) {
        // Combine context stack trace with top object's stack trace
        context = context + getCreationContext(obj);

        GraphPattern classInfo = getGraphPattern(className, dumpId, context);
        if (classInfo == null)
            throw new RuntimeException("ClassInfo not found for " + className);

        Set<String> brokenInvs = new HashSet<>();
        LogInfo logInfo = new LogInfo(dumpId, context.hashCode());

        boolean changed = classInfo.update(obj, baseClassInfo, logInfo, equalitySet, isSerialized,
                brokenInvs, objId);

        if (!brokenInvs.isEmpty() && enableInvariantCombination) {
            invariantCombination.record(objId, brokenInvs);
        }

        // This equality also includes the context objects
        if (equalitySet != null)
            equalitySet.dumpSameObjectGraph(dumpId, objId);

        return changed;
    }

    public void updateContextObjectGraphPattern(int dumpId, Object obj, String className,
            int objId) {
        // Combine context stack trace with top object's stack trace
        String context = getCreationContext(obj);

        GraphPattern classInfo = getContextGraphPattern(className, dumpId, context);
        if (classInfo == null)
            throw new RuntimeException("ClassInfo not found for " + className);

        Set<String> brokenInvs = new HashSet<>();
        LogInfo logInfo = new LogInfo(dumpId, context.hashCode());

        boolean changed = classInfo.update(obj, baseClassInfo, logInfo, equalitySet, isSerialized,
                brokenInvs, objId);

        if (!brokenInvs.isEmpty() && enableInvariantCombination) {
            invariantCombination.record(objId, brokenInvs);
        }
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

    public boolean dump(Object obj, int dumpId, String context) {
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

        GraphPattern classInfo = getGraphPattern(className, dumpId, context);
        if (classInfo == null)
            return false;

        ObjectGraph objectGraph = objectGraphDumper.dump(obj, dumpId);

        // Testing
        // objectGraphs.clear();

        objectGraphs.add(objectGraph);
        return true;
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
        mergeCoverage(dumpId2ObjCoverageWithContext, otherObjCoverage.dumpId2ObjCoverageWithContext,
                formatCoverageStatus);
    }

    private void mergeContextGraphPattern(ObjectGraphCoverage otherObjCoverage,
            FormatCoverageStatus formatCoverageStatus) {
        mergeCoverage(dumpId2ContextObjCoverageWithContext,
                otherObjCoverage.dumpId2ContextObjCoverageWithContext, formatCoverageStatus);
    }

    private static void mergeCoverage(
            Map<Integer, Map<String, Map<String, GraphPattern>>> dumpId2ContextObjCoverageWithContext1,
            Map<Integer, Map<String, Map<String, GraphPattern>>> dumpId2ContextObjCoverageWithContext2,
            FormatCoverageStatus formatCoverageStatus) {
        for (int dumpId : dumpId2ContextObjCoverageWithContext2.keySet()) {
            Map<String, Map<String, GraphPattern>> otherObjCoverageWithContext = dumpId2ContextObjCoverageWithContext2
                    .get(dumpId);
            if (otherObjCoverageWithContext == null)
                continue;
            Map<String, Map<String, GraphPattern>> objCoverageWithContext = dumpId2ContextObjCoverageWithContext1
                    .computeIfAbsent(dumpId, k -> new HashMap<>());
            mergeGraphPattern(objCoverageWithContext, otherObjCoverageWithContext,
                    formatCoverageStatus);
        }
    }

    private static void mergeGraphPattern(
            Map<String, Map<String, GraphPattern>> objCoverageWithContext,
            Map<String, Map<String, GraphPattern>> otherObjCoverageWithContext,
            FormatCoverageStatus formatCoverageStatus) {
        if (otherObjCoverageWithContext == null)
            return;
        for (String context : otherObjCoverageWithContext.keySet()) {
            Map<String, GraphPattern> otherClassInfo = otherObjCoverageWithContext.get(context);
            if (otherClassInfo == null)
                continue;
            if (!objCoverageWithContext.containsKey(context)) {
                if (Runtime.debug) {
                    Runtime.log("[hklog] new context = " + context);
                    for (String oriContext : objCoverageWithContext.keySet()) {
                        Runtime.log("[hklog] ori context = " + oriContext);
                    }
                    Runtime.log("");
                }
                objCoverageWithContext.put(context, new HashMap<>());
            }
            Map<String, GraphPattern> classInfo = objCoverageWithContext.get(context);
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
                    FormatCoverageStatus otherFormatCoverageStatus = graphPattern
                            .merge(otherGraphPattern);
                    formatCoverageStatus.incorporate(otherFormatCoverageStatus);
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
        if (enableInvariantCombination
                && invariantCombination.merge(otherObjCoverage.invariantCombination)) {
            formatCoverageStatus.newFormat = true;
        }
    }

    // Test usage only
    public Map<Integer, String> getObjectCreationStacktrace() {
        return topObj2CreationStacktrace;
    }

    public static IsSerialize constructIsSerialize(Path modifiedFieldsPath,
            Path modifiedEnumsPath) {
        Map<String, Set<String>> modifiedFields = Utils
                .loadModifiedFields(modifiedFieldsPath.toString());
        Set<String> modifiedEnums = Utils.loadSetFromFile(modifiedEnumsPath.toString());
        return new IsSerialize(modifiedFields, modifiedEnums);
    }
}
