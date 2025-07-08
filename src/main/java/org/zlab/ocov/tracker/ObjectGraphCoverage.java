package org.zlab.ocov.tracker;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.datasketches.theta.Sketch;
import org.apache.datasketches.theta.Sketches;
import org.apache.datasketches.theta.UpdateSketch;
import org.zlab.ocov.Utils;
import org.zlab.ocov.tracker.graph.*;
import org.zlab.ocov.tracker.inv.InvariantBrokenFrequency;
import org.zlab.ocov.tracker.inv.unary.*;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.apache.datasketches.theta.JaccardSimilarity.jaccard;

public class ObjectGraphCoverage implements Serializable {
    private static final long serialVersionUID = 20231215L;

    // ----------------------- General Config -------------------------
    // If object with the same addr occur twice, avoid processing it
    public static final boolean avoidRecordObjectWithSameAddress = false;
    public static final boolean useContextFromArgs = true;
    public static final boolean collectContextGraphPattern = true;

    public static final boolean limitMaxPatternNum = true;
    public static final int maxPatternNum = 8;

    // Use another implementation (more efficient...)
    private static final double similarityThreshold = 0.5;
    private final transient Map<String, Sketch> sketches = new HashMap<>();

    // DumpId -> classname -> graph pattern (Only top objects)
    transient Map<Integer, Map<String, Integer>> dumpId2Context2GroupId = new HashMap<>();
    transient Map<Integer, Integer> dumpId2CurrentGroupId = new HashMap<>();
    public Map<Integer, Map<Integer, Map<String, GraphPattern>>> accumDumpId2ObjCoverageWithContext = new HashMap<>();

    // Runtime: collector side
    public Map<Integer, Map<String, Map<String, GraphPattern>>> dumpId2ObjCoverageWithContext = new HashMap<>();
    // Not collect inv for context args currently
    public transient Map<Integer, Map<String, Map<String, GraphPattern>>> dumpId2ContextObjCoverageWithContext = new HashMap<>();

    public EqualitySet equalitySet;
    public IsSerialize isSerialized;
    public Boundary boundary;

    public static final boolean enableInvariantCombination = true;
    public static final boolean enableInvariantCombinationWithFrequency = true;

    public InvariantCombination invariantCombination;
    public int topNLessFrequentBrokenInvariant = 5;
    public InvariantBrokenFrequency invariantBrokenFrequency = new InvariantBrokenFrequency();

    // ----------------------- Serialization Window ---------------------
    public static final boolean useSerializationWindow = false;
    public static long serializationWindowTsLimitMillis = 1_000; // ms
    public static int serializationWindowCapacity = 10; // 10 objects
    public static long serializationWindowPeriodMillis = 10_000; // ms
    public static boolean periodicUpdate = false;

    // SerializationWindow serializationWindow = new SerializationWindow(
    // serializationWindowTsLimitMillis, serializationWindowCapacity,
    // serializationWindowPeriodMillis, periodicUpdate);

    // ----------------------- Runtime --------------------------------
    public transient Set<Integer> visitedObjects = new HashSet<>();

    // -------------------- Creation context --------------------------
    private transient final Map<Integer, Integer> objAddress2TopObjAddress = new HashMap<>();
    private transient final Map<Integer, String> topObj2CreationStacktrace = new HashMap<>();
    private transient final Map<Integer, Integer> dumpId2monitorCount = new HashMap<>();

    // Sample if the object if the dump point occur too often
    private static final boolean enableSampleMonitorThreshold = true;
    private static final int monitorSampleThreshold = 100;
    private static final double monitorSampleRate = 0.001;

    // ----------------------- Graph Pattern --------------------------
    private transient final Map<Integer, Integer> dumpId2UpdateCount = new HashMap<>();
    private static final boolean enableSampleUpdateThreshold = true;
    private static final int updateSampleThreshold = 100;
    private static final double updateSampleRate = 0.001;

    private transient Map<String, Map<String, String>> classInfoOri;
    private transient Map<String, GraphPattern> baseClassInfo;
    private transient Set<String> topObjects;

    // ------------------- Modification guided testing ----------------
    private transient Set<Integer> specialDumpIds;

    private transient Map<String, Map<String, String>> matchableClassInfo;
    // IsSerialized likely invariants
    private transient Set<String> changedClasses;
    private transient Set<String> visitedChangedClasses = new HashSet<>();

    public ObjectGraphCoverage() {
        // for json
    }

    public ObjectGraphCoverage(Path baseClassInfoPath, Path topObjectsPath) {
        this(baseClassInfoPath, topObjectsPath, null, null, null, null, null, null);
    }

    public ObjectGraphCoverage(Path baseClassInfoPath, Path topObjectsPath,
            Path comparableClassesPath) {
        this(baseClassInfoPath, topObjectsPath, comparableClassesPath, null, null, null, null,
                null);
    }

    public ObjectGraphCoverage(Path baseClassInfoPath, Path topObjectsPath,
            Path comparableClassesPath, Path modifiedFieldsPath, Path modifiedEnumsPath) {
        this(baseClassInfoPath, topObjectsPath, comparableClassesPath, modifiedFieldsPath,
                modifiedEnumsPath, null, null, null);
    }

    public ObjectGraphCoverage(Path baseClassInfoPath, Path topObjectsPath,
            Path comparableClassesPath, Path modifiedFieldsPath, Path modifiedEnumsPath,
            Path modifiedTypeHierarchyPath, Path branch2CollectionPath) {
        this(baseClassInfoPath, topObjectsPath, comparableClassesPath, modifiedFieldsPath,
                modifiedEnumsPath, modifiedTypeHierarchyPath, branch2CollectionPath, null);
    }

    public ObjectGraphCoverage(Path baseClassInfoPath, Path topObjectsPath,
            Path comparableClassesPath, Path modifiedFieldsPath, Path modifiedEnumsPath,
            Path modifiedTypeHierarchyPath, Path branch2CollectionPath, Path specialDumpIdsPath) {
        classInfoOri = Utils.loadMapFromFile(baseClassInfoPath);
        topObjects = Utils.loadSetFromFile(topObjectsPath);
        baseClassInfo = GraphPattern.createGraphPatterns(classInfoOri);
        Set<String> comparableClasses = null;
        if (comparableClassesPath != null && comparableClassesPath.toFile().exists()) {
            comparableClasses = Utils.loadSetFromFile(comparableClassesPath);
        }
        if (comparableClasses != null) {
            equalitySet = new EqualitySet(comparableClasses);
        }
        if (modifiedFieldsPath != null && modifiedFieldsPath.toFile().exists()
                && modifiedEnumsPath != null && modifiedEnumsPath.toFile().exists()
                && modifiedTypeHierarchyPath != null
                && modifiedTypeHierarchyPath.toFile().exists()) {
            isSerialized = constructIsSerialize(modifiedFieldsPath, modifiedEnumsPath,
                    modifiedTypeHierarchyPath);
        }
        boundary = new Boundary();
        if (enableInvariantCombination)
            invariantCombination = new InvariantCombination();
        if (specialDumpIdsPath != null && specialDumpIdsPath.toFile().exists()) {
            specialDumpIds = Utils.loadIntSetFromFile(specialDumpIdsPath);
        }
    }

    public GraphPattern getGraphPattern(String className, int dumpId, String context) {
        updateSketches(context);

        if (!dumpId2ObjCoverageWithContext.containsKey(dumpId)) {
            dumpId2ObjCoverageWithContext.put(dumpId, new HashMap<>());
        }
        Map<String, Map<String, GraphPattern>> contextObjCoverage = dumpId2ObjCoverageWithContext
                .get(dumpId);

        if (limitMaxPatternNum && !contextObjCoverage.containsKey(context)
                && contextObjCoverage.keySet().size() >= maxPatternNum) {
            // Find the closest context
            double maxSimilarity = 0;
            String closestContext = null;

            for (String oriContext : contextObjCoverage.keySet()) {
                double[] jaccardResults = jaccard(sketches.get(oriContext), sketches.get(context));
                double similarity = jaccardResults[1];
                if (similarity >= maxSimilarity) {
                    maxSimilarity = similarity;
                    closestContext = oriContext;
                }
            }
            assert closestContext != null;
            context = closestContext;
        }
        Map<String, GraphPattern> objCoverage = contextObjCoverage.computeIfAbsent(context,
                k -> new HashMap<>());

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
        monitorCreationContext(obj, null, -1);
    }

    public void monitorCreationContext(Object obj, int dumpId) {
        monitorCreationContext(obj, null, dumpId);
    }

    public void monitorCreationContext(Object obj, Object contextObj, int dumpId) {
        if (obj == null)
            return;
        // long time1 = System.currentTimeMillis();
        String className = obj.getClass().getName();
        if (!topObjects.contains(className) || !baseClassInfo.containsKey(className)) {
            return;
        }

        if (!dumpId2monitorCount.containsKey(dumpId)) {
            dumpId2monitorCount.put(dumpId, 0);
        }
        if (enableSampleMonitorThreshold && dumpId2monitorCount.get(dumpId) > monitorSampleThreshold
                && Math.random() > monitorSampleRate) {
            return;
        }
        dumpId2monitorCount.put(dumpId, dumpId2monitorCount.get(dumpId) + 1);

        int topAddr = System.identityHashCode(obj);
        ObjectGraphTraverser objectGraphTraverser = new ObjectGraphTraverser(classInfoOri);
        objectGraphTraverser.traverse(obj);
        Set<Integer> visited = objectGraphTraverser.getVisited();

        // update obj2TopObj
        for (int addr : visited) {
            objAddress2TopObjAddress.put(addr, topAddr);
        }
        // long time2 = System.currentTimeMillis();

        String stacktrace;
        if (contextObj != null && !getCreationContext(contextObj).isEmpty()) {
            stacktrace = getCreationContext(contextObj);
        } else {
            stacktrace = Utils.getStackTrace();
        }

        // update topObj2CreationStacktrace
        topObj2CreationStacktrace.put(topAddr, stacktrace);
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

        // Sample if the object is processed too often
        if (!dumpId2UpdateCount.containsKey(dumpId)) {
            dumpId2UpdateCount.put(dumpId, 0);
        }
        if (enableSampleUpdateThreshold && dumpId2UpdateCount.get(dumpId) > updateSampleThreshold
                && Math.random() > updateSampleRate) {
            return false;
        }
        dumpId2UpdateCount.put(dumpId, dumpId2UpdateCount.get(dumpId) + 1);

        if (Runtime.debug)
            Runtime.log("[ObjectGraphCoverage.update] dumpId: " + dumpId + ", obj type = : "
                    + obj.getClass().getName() + ", hashcode = " + System.identityHashCode(obj));

        boolean changed = false;
        if (collectContextGraphPattern) {
            if (useSerializationWindow) {
                // General
                // serializationWindow.update();
                // for (SerializationWindow.RecordedObject recObj :
                // serializationWindow.getQueue()) {
                // Object contextObj = recObj.object;
                // if (contextObj == null)
                // continue;
                // String contextClassName = contextObj.getClass().getName();
                // if (!baseClassInfo.containsKey(contextClassName))
                // continue;
                // updateContextObjectGraphPattern(dumpId, contextObj, contextClassName,
                // System.identityHashCode(contextObj));
                // }
            } else {
                // Heuristic
                for (Object contextObj : contextArgs) {
                    if (contextObj == null)
                        continue;
                    String contextClassName = contextObj.getClass().getName();
                    if (!baseClassInfo.containsKey(contextClassName))
                        continue;
                    updateContextObjectGraphPattern(dumpId, contextObj, contextClassName,
                            System.identityHashCode(contextObj));
                }
            }
        }
        String context = getCreationContextFromArgs(contextArgs);
        if (updateTopObjectGraphPattern(dumpId, context, obj, className, objId))
            changed = true;

        // include it, do an update
        // serializationWindow.add(obj);

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

    private void updateSketches(String context) {
        if (sketches.containsKey(context))
            return;
        Set<String> tokens = Utils.tokenize(context);
        UpdateSketch sketch = Sketches.updateSketchBuilder().build();
        for (String token : tokens) {
            sketch.update(token.getBytes(StandardCharsets.UTF_8));
        }
        sketches.put(context, sketch.compact());
    }

    private int getGroupId(int dumpId, String context) {
        if (!dumpId2Context2GroupId.containsKey(dumpId)) {
            dumpId2Context2GroupId.put(dumpId, new HashMap<>());
        }
        Map<String, Integer> context2GroupId = dumpId2Context2GroupId.get(dumpId);
        if (context2GroupId.containsKey(context)) {
            return context2GroupId.get(context);
        }

        // Update sketches
        updateSketches(context);

        double maxSimilarity = 0;
        // int minDistance = Integer.MAX_VALUE;
        int minGroupId = -1;
        for (String oriContext : context2GroupId.keySet()) {
            double[] jaccardResults = jaccard(sketches.get(oriContext), sketches.get(context));
            double similarity = jaccardResults[1];
            if (similarity > maxSimilarity) {
                maxSimilarity = similarity;
                minGroupId = context2GroupId.get(oriContext);
            }
        }

        // Found one that's very similar or reach the max maintained pattern number
        if (maxSimilarity >= similarityThreshold || context2GroupId.size() >= maxPatternNum) {
            context2GroupId.put(context, minGroupId);
            return minGroupId;
        }

        // Create a new group
        if (!dumpId2CurrentGroupId.containsKey(dumpId)) {
            dumpId2CurrentGroupId.put(dumpId, 0);
        }
        int groupId = dumpId2CurrentGroupId.get(dumpId);
        dumpId2CurrentGroupId.put(dumpId, groupId + 1);
        context2GroupId.put(context, groupId);
        return groupId;
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

        boolean changed = classInfo.update(obj, baseClassInfo, classInfoOri, logInfo, equalitySet,
                isSerialized, brokenInvs, objId);

        // This equality also includes the context objects
        if (equalitySet != null)
            equalitySet.dumpSameObjectGraph(dumpId, brokenInvs);

        if (!brokenInvs.isEmpty() && enableInvariantCombination) {
            invariantCombination.record(dumpId, brokenInvs);
        }

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

        boolean changed = classInfo.update(obj, baseClassInfo, classInfoOri, logInfo, equalitySet,
                isSerialized, brokenInvs, objId);

        if (!brokenInvs.isEmpty() && enableInvariantCombination) {
            invariantCombination.record(dumpId, brokenInvs);
        }
    }

    public void setMatchableClassInfo(Map<String, Map<String, String>> matchableClassInfo) {
        this.matchableClassInfo = matchableClassInfo;
    }

    public void setChangedClasses(Set<String> changedClasses) {
        this.changedClasses = changedClasses;
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
    }

    // Not in use as we create a new ObjectGraphCoverage for each test
    public void clear() {
        // Separate format coverage across tests
        visitedObjects.clear();
        objAddress2TopObjAddress.clear();
        topObj2CreationStacktrace.clear();
        dumpId2monitorCount.clear();
        dumpId2UpdateCount.clear();
        dumpId2CurrentGroupId.clear();
        dumpId2Context2GroupId.clear();

        // serializationWindow.clear();

        if (equalitySet != null)
            equalitySet.clear();
        if (isSerialized != null)
            isSerialized.clear();
        if (enableInvariantCombination)
            invariantCombination.clear();
        boundary.clear();
    }

    public FormatCoverageStatus merge(ObjectGraphCoverage otherObjCoverage) {
        return merge(otherObjCoverage, -1, false, false, false);
    }

    public FormatCoverageStatus merge(ObjectGraphCoverage otherObjCoverage, int testId) {
        return merge(otherObjCoverage, testId, false, false, false);
    }

    public FormatCoverageStatus merge(ObjectGraphCoverage otherObjCoverage, int testId,
            boolean groupByContext, boolean updateInvariantBrokenFrequency) {
        return merge(otherObjCoverage, "", testId, groupByContext, updateInvariantBrokenFrequency,
                false);
    }

    public FormatCoverageStatus merge(ObjectGraphCoverage otherObjCoverage, int testId,
            boolean groupByContext, boolean updateInvariantBrokenFrequency,
            boolean checkSpecialDumpIds) {
        return merge(otherObjCoverage, "", testId, groupByContext, updateInvariantBrokenFrequency,
                checkSpecialDumpIds);
    }

    public FormatCoverageStatus merge(ObjectGraphCoverage otherObjCoverage, String identifier,
            int testId, boolean groupByContext, boolean updateInvariantBrokenFrequency,
            boolean checkSpecialDumpIds) {
        FormatCoverageStatus formatCoverageStatus = new FormatCoverageStatus();
        if (otherObjCoverage == null)
            return formatCoverageStatus;

        if (groupByContext) {
            mergeTopGraphPatternWithGrouping(otherObjCoverage, formatCoverageStatus,
                    checkSpecialDumpIds);
        } else {
            mergeTopGraphPatternWithoutGrouping(otherObjCoverage, formatCoverageStatus,
                    checkSpecialDumpIds);
        }

        mergeSpecialInvariant(otherObjCoverage, formatCoverageStatus,
                updateInvariantBrokenFrequency, checkSpecialDumpIds);

        if (formatCoverageStatus.isChanged()) {
            if (identifier.isEmpty())
                Runtime.log(
                        String.format("[hklog] --- Merged new coverage testId: %d ---", testId));
            else
                Runtime.log(String.format("[hklog] --- Merged new coverage from: %s testId: %d ---",
                        identifier, testId));
        }
        return formatCoverageStatus;
    }

    private void mergeTopGraphPatternWithGrouping(ObjectGraphCoverage otherObjCoverage,
            FormatCoverageStatus formatCoverageStatus, boolean checkSpecialDumpIds) {
        mergeAccumCoverage(accumDumpId2ObjCoverageWithContext,
                otherObjCoverage.dumpId2ObjCoverageWithContext, formatCoverageStatus,
                checkSpecialDumpIds);
    }

    private void mergeTopGraphPatternWithoutGrouping(ObjectGraphCoverage otherObjCoverage,
            FormatCoverageStatus formatCoverageStatus, boolean checkSpecialDumpIds) {
        mergeCoverage(dumpId2ObjCoverageWithContext, otherObjCoverage.dumpId2ObjCoverageWithContext,
                formatCoverageStatus, checkSpecialDumpIds, specialDumpIds);
    }

    private void mergeContextGraphPattern(ObjectGraphCoverage otherObjCoverage,
            FormatCoverageStatus formatCoverageStatus, boolean checkSpecialDumpIds) {
        mergeCoverage(dumpId2ContextObjCoverageWithContext,
                otherObjCoverage.dumpId2ContextObjCoverageWithContext, formatCoverageStatus,
                checkSpecialDumpIds, specialDumpIds);
    }

    private static void mergeCoverage(
            Map<Integer, Map<String, Map<String, GraphPattern>>> dumpId2ObjCoverage1,
            Map<Integer, Map<String, Map<String, GraphPattern>>> dumpId2ObjCoverage2,
            FormatCoverageStatus formatCoverageStatus, boolean checkSpecialDumpIds,
            Set<Integer> specialDumpIds) {
        for (int dumpId : dumpId2ObjCoverage2.keySet()) {
            Map<String, Map<String, GraphPattern>> otherObjCoverageWithContext = dumpId2ObjCoverage2
                    .get(dumpId);
            if (otherObjCoverageWithContext == null)
                continue;
            Map<String, Map<String, GraphPattern>> objCoverageWithContext = dumpId2ObjCoverage1
                    .computeIfAbsent(dumpId, k -> new HashMap<>());

            FormatCoverageStatus currentFormatCoverageStatus = new FormatCoverageStatus();
            mergeGraphPattern(objCoverageWithContext, otherObjCoverageWithContext,
                    currentFormatCoverageStatus, dumpId);
            formatCoverageStatus.incorporate(currentFormatCoverageStatus);
        }
    }

    private void mergeAccumCoverage(
            Map<Integer, Map<Integer, Map<String, GraphPattern>>> dumpId2ObjCoverage1,
            Map<Integer, Map<String, Map<String, GraphPattern>>> dumpId2ObjCoverage2,
            FormatCoverageStatus formatCoverageStatus, boolean checkSpecialDumpIds) {
        for (int dumpId : dumpId2ObjCoverage2.keySet()) {
            long time1 = System.currentTimeMillis();

            Map<String, Map<String, GraphPattern>> otherObjCoverageWithContext = dumpId2ObjCoverage2
                    .get(dumpId);
            if (otherObjCoverageWithContext == null)
                continue;
            Map<Integer, Map<String, GraphPattern>> objCoverageWithContext = dumpId2ObjCoverage1
                    .computeIfAbsent(dumpId, k -> new HashMap<>());

            FormatCoverageStatus currentFormatCoverageStatus = new FormatCoverageStatus();
            mergeAccumGraphPattern(objCoverageWithContext, otherObjCoverageWithContext,
                    currentFormatCoverageStatus, dumpId, matchableClassInfo, changedClasses,
                    visitedChangedClasses);

            formatCoverageStatus.incorporate(currentFormatCoverageStatus);

            long time2 = System.currentTimeMillis();
            // Debug, need to disable
            if (Runtime.debug)
                Runtime.log(String.format("[hklog] dump Id = %d, mergeAccumCoverage time = %.2fs",
                        dumpId, (time2 - time1) / 1000.));
        }
    }

    private static void mergeGraphPattern(
            Map<String, Map<String, GraphPattern>> objCoverageWithContext,
            Map<String, Map<String, GraphPattern>> otherObjCoverageWithContext,
            FormatCoverageStatus formatCoverageStatus, int dumpId) {
        if (otherObjCoverageWithContext == null)
            return;
        for (String context : otherObjCoverageWithContext.keySet()) {
            Map<String, GraphPattern> otherClassInfo = otherObjCoverageWithContext.get(context);
            if (otherClassInfo == null)
                continue;
            if (!objCoverageWithContext.containsKey(context)) {
                objCoverageWithContext.put(context, new HashMap<>());
            }
            Map<String, GraphPattern> classInfo = objCoverageWithContext.get(context);
            for (String className : otherClassInfo.keySet()) {
                GraphPattern otherGraphPattern = otherClassInfo.get(className);
                if (otherGraphPattern == null)
                    continue;
                GraphPattern graphPattern = classInfo.get(className);
                if (graphPattern == null) {
                    classInfo.put(className, SerializationUtils.clone(otherGraphPattern));
                    formatCoverageStatus.setNewFormat(
                            "Add new graphPattern for " + className + ", context hashcode = "
                                    + context.hashCode() + ", dumpId = " + dumpId);
                } else {
                    LogInfo logInfo = new LogInfo(dumpId, context.hashCode());
                    FormatCoverageStatus otherFormatCoverageStatus = graphPattern
                            .merge(otherGraphPattern, logInfo);
                    formatCoverageStatus.incorporate(otherFormatCoverageStatus);
                }
            }
        }
    }

    private void mergeAccumGraphPattern(
            Map<Integer, Map<String, GraphPattern>> objCoverageWithContext,
            Map<String, Map<String, GraphPattern>> otherObjCoverageWithContext,
            FormatCoverageStatus formatCoverageStatus, int dumpId,
            Map<String, Map<String, String>> matchableClassInfo, Set<String> changedClasses,
            Set<String> visitedChangedClasses) {
        if (otherObjCoverageWithContext == null)
            return;
        for (String context : otherObjCoverageWithContext.keySet()) {
            Map<String, GraphPattern> otherClassInfo = otherObjCoverageWithContext.get(context);
            if (otherClassInfo == null)
                continue;

            // compute group ID
            int groupId = getGroupId(dumpId, context);

            if (!objCoverageWithContext.containsKey(groupId)) {
                objCoverageWithContext.put(groupId, new HashMap<>());
            }
            Map<String, GraphPattern> classInfo = objCoverageWithContext.get(groupId);
            for (String className : otherClassInfo.keySet()) {
                GraphPattern otherGraphPattern = otherClassInfo.get(className);
                if (otherGraphPattern == null)
                    continue;
                if (!classInfo.containsKey(className)) {
                    assert baseClassInfo.containsKey(className);
                    classInfo.put(className,
                            SerializationUtils.clone(baseClassInfo.get(className)));
                    formatCoverageStatus.setNewFormat(
                            "Add new graphPattern for " + className + ", context hashcode = "
                                    + context.hashCode() + ", dumpId = " + dumpId);
                    if (matchableClassInfo != null) {
                        if (matchableClassInfo.containsKey(className))
                            formatCoverageStatus.setMatchableNewFormat("");
                        else
                            formatCoverageStatus.setNonMatchableNewFormat("");
                    }
                    if (changedClasses != null && changedClasses.contains(className)
                            && !visitedChangedClasses.contains(className)) {
                        formatCoverageStatus.setIsSerialize("");
                        visitedChangedClasses.add(className);
                    }
                }
                GraphPattern graphPattern = classInfo.get(className);
                LogInfo logInfo = new LogInfo(dumpId, context.hashCode(), matchableClassInfo,
                        changedClasses, visitedChangedClasses);
                FormatCoverageStatus otherFormatCoverageStatus = graphPattern
                        .merge(otherGraphPattern, logInfo);
                formatCoverageStatus.incorporate(otherFormatCoverageStatus);
            }
        }
    }

    private void mergeSpecialInvariant(ObjectGraphCoverage otherObjCoverage,
            FormatCoverageStatus formatCoverageStatus, boolean updateInvariantBrokenFrequency,
            boolean checkSpecialDumpIds) {
        if (equalitySet == null) {
            if (otherObjCoverage.equalitySet != null) {
                equalitySet = SerializationUtils.clone(otherObjCoverage.equalitySet);
                formatCoverageStatus.setNewFormat("Add new equalitySet, previous is null");
            }
        } else {
            equalitySet.merge(otherObjCoverage.equalitySet, formatCoverageStatus,
                    new Utils.DeltaInfo(matchableClassInfo, changedClasses));
        }
        if (isSerialized == null) {
            if (otherObjCoverage.isSerialized != null) {
                isSerialized = SerializationUtils.clone(otherObjCoverage.isSerialized);
                formatCoverageStatus.setNewFormat("Add new isSerialized, previous is null");
            }
        } else {
            if (isSerialized.merge(otherObjCoverage.isSerialized)) {
                formatCoverageStatus.setNewFormat("New isSerialized");
            }
        }
        if (boundary == null) {
            if (otherObjCoverage.boundary != null) {
                boundary = SerializationUtils.clone(otherObjCoverage.boundary);
                formatCoverageStatus.setNewFormat("Add new boundary, previous is null");
            }
        } else {
            if (boundary.merge(otherObjCoverage.boundary)) {
                formatCoverageStatus.setBoundaryChange("New boundary");
            }
        }

        if (enableInvariantCombination) {
            if (enableInvariantCombinationWithFrequency && updateInvariantBrokenFrequency) {
                // Update frequency
                invariantBrokenFrequency.update(otherObjCoverage.invariantCombination);
                invariantCombination.merge(otherObjCoverage.invariantCombination,
                        invariantBrokenFrequency
                                .getMostInfrequentInvariants(topNLessFrequentBrokenInvariant),
                        formatCoverageStatus, checkSpecialDumpIds, specialDumpIds,
                        new Utils.DeltaInfo(matchableClassInfo, changedClasses));
            } else {
                // Deprecated
                if (invariantCombination.merge(otherObjCoverage.invariantCombination))
                    formatCoverageStatus.setNewFormat("invariantCombination");
            }
        }
    }

    // Test usage only
    public Map<Integer, String> getObjectCreationStacktrace() {
        return topObj2CreationStacktrace;
    }

    public void copyBasicInfo(ObjectGraphCoverage otherObjCoverage) {
        classInfoOri = otherObjCoverage.classInfoOri;
        topObjects = otherObjCoverage.topObjects;
        baseClassInfo = otherObjCoverage.baseClassInfo;
    }

    public static IsSerialize constructIsSerialize(Path modifiedFieldsPath, Path modifiedEnumsPath,
            Path modifiedHierarchyPath) {
        Map<String, Set<String>> modifiedFields = Utils.loadModifiedFields(modifiedFieldsPath);
        Set<String> modifiedEnums = Utils.loadSetFromFile(modifiedEnumsPath);
        Set<String> typeWithModifiedHierarchy = Utils.loadSetFromFile(modifiedHierarchyPath);
        return new IsSerialize(modifiedFields, modifiedEnums, typeWithModifiedHierarchy);
    }

    public double measureCoverageOfModifiedReferences(
            Map<String, Set<String>> modifiedSerializedReferences, boolean save) {
        long start = System.nanoTime();

        int allModifiedReferenceSize = Utils.count(modifiedSerializedReferences);

        Map<String, Set<String>> occurredReferences = extractOccurredReferences();

        // Compute the occurred modified references
        Map<String, Set<String>> occurredModifiedReferences = Utils.intersect(occurredReferences,
                modifiedSerializedReferences);
        int occurredModifiedReferenceSize = Utils.count(occurredModifiedReferences);
        if (save)
            Utils.saveModifiedFields(occurredModifiedReferences,
                    Paths.get("occurredModifiedReferences.json"));

        // Compute remaining references
        Map<String, Set<String>> NotOccurredModifiedReferences = Utils
                .onlyExistInLeft(modifiedSerializedReferences, occurredModifiedReferences);
        if (save)
            Utils.saveModifiedFields(NotOccurredModifiedReferences,
                    Paths.get("unOccurredModifiedReferences.json"));

        // Coverage (Percentage)
        double coveredPercentage = occurredModifiedReferenceSize * 1.0 / allModifiedReferenceSize;
        // print this double with only 2 decimal places after the dot

        String formattedCoveredPercentage = String.format("%.2f", coveredPercentage);

        long end = System.nanoTime();

        long duration = end - start;

        System.out.format("|%30s|%30s|%30s|%30s|\n",
                "All mod ref size : " + allModifiedReferenceSize,
                "Occurred Mod ref size : " + occurredModifiedReferenceSize,
                "Covered Percentage : " + formattedCoveredPercentage,
                "compute time : " + duration / 1000000 + "ms");
        return coveredPercentage;
    }

    public Map<String, Set<String>> extractOccurredReferences() {
        Map<String, Set<String>> allOccurredReferences = new HashMap<>();
        // Map<String, Set<String>>
        // iterate all graph patterns
        for (Map<Integer, Map<String, GraphPattern>> objCoverageWithContext : accumDumpId2ObjCoverageWithContext
                .values()) {
            for (Map<String, GraphPattern> classInfo : objCoverageWithContext.values()) {
                for (GraphPattern graphPattern : classInfo.values()) {
                    Utils.merge(allOccurredReferences, graphPattern.extractOccurredReference());
                }
            }
        }
        return allOccurredReferences;
    }

    // clear serialization window
    public void clearSerializationWindow() {
        // serializationWindow.clear();
    }
}
