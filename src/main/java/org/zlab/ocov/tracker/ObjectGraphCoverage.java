package org.zlab.ocov.tracker;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.datasketches.theta.Sketch;
import org.apache.datasketches.theta.Sketches;
import org.apache.datasketches.theta.UpdateSketch;
import org.zlab.ocov.Utils;
import org.zlab.ocov.tracker.graph.*;
import org.zlab.ocov.tracker.inv.unary.*;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

import static org.apache.datasketches.theta.JaccardSimilarity.jaccard;

public class ObjectGraphCoverage implements Serializable {
    private static final long serialVersionUID = 20231215L;

    // ----------------------- Config -----------------------
    public final static boolean enableInvariantCombination = false;
    // If object with the same addr occur twice, avoid processing it
    public final static boolean avoidRecordObjectWithSameAddress = false;
    public final static boolean useContextFromArgs = true;
    public final static boolean collectContextGraphPattern = true;

    public final static boolean limitMaxPatternNum = true;
    public final static int maxPatternNum = 8;

    private final transient boolean useLevenshteinDistance = false;

    private final transient LevenshteinDistance levenshteinDistance = new LevenshteinDistance();
    private final int editDistanceThreshold = 600;

    // Use another implementation (more efficient...)
    private final transient Map<String, Sketch> sketches = new HashMap<>();
    private final transient double similarityThreshold = 0.5;

    // DumpId -> classname -> graph pattern (Only top objects)
    transient Map<Integer, Map<String, Integer>> dumpId2Context2GroupId = new HashMap<>();
    transient Map<Integer, Integer> dumpId2CurrentGroupId = new HashMap<>();
    transient Map<Integer, Map<Integer, Map<String, GraphPattern>>> accumDumpId2ObjCoverageWithContext = new HashMap<>();

    // Runtime: collector side
    public Map<Integer, Map<String, Map<String, GraphPattern>>> dumpId2ObjCoverageWithContext = new HashMap<>();
    // Not collect inv for context args currently
    public transient Map<Integer, Map<String, Map<String, GraphPattern>>> dumpId2ContextObjCoverageWithContext = new HashMap<>();

    public EqualitySet equalitySet;
    public IsSerialize isSerialized;
    public Boundary boundary;
    public InvariantCombination invariantCombination;

    // ----------------------- Runtime -----------------------
    public transient Set<Integer> visitedObjects = new HashSet<>();

    // Creation context
    private transient final Map<Integer, Integer> objAddress2TopObjAddress = new HashMap<>();
    private transient final Map<Integer, String> topObj2CreationStacktrace = new HashMap<>();

    private transient Map<String, Map<String, String>> classInfoOri;
    public transient Map<String, GraphPattern> baseClassInfo;
    public transient Set<String> topObjects;

    // Only record, and infer at last
    private transient List<ObjectGraph> objectGraphs = new ArrayList<>();
    private transient ObjectGraphDumper objectGraphDumper;

    public ObjectGraphCoverage() {
        // for json
    }

    public ObjectGraphCoverage(Path baseClassInfoPath, Path topObjectsPath) {
        this(baseClassInfoPath, topObjectsPath, null, null, null, null, null);
    }

    public ObjectGraphCoverage(Path baseClassInfoPath, Path topObjectsPath,
            Path comparableClassesPath) {
        this(baseClassInfoPath, topObjectsPath, comparableClassesPath, null, null, null, null);
    }

    public ObjectGraphCoverage(Path baseClassInfoPath, Path topObjectsPath,
            Path comparableClassesPath, Path modifiedFieldsPath, Path modifiedEnumsPath) {
        this(baseClassInfoPath, topObjectsPath, comparableClassesPath, modifiedFieldsPath,
                modifiedEnumsPath, null, null);
    }

    public ObjectGraphCoverage(Path baseClassInfoPath, Path topObjectsPath,
            Path comparableClassesPath, Path modifiedFieldsPath, Path modifiedEnumsPath,
            Path modifiedTypeHierarchyPath, Path branch2CollectionPath) {
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
                && modifiedEnumsPath != null && modifiedEnumsPath.toFile().exists()
                && modifiedTypeHierarchyPath != null
                && modifiedTypeHierarchyPath.toFile().exists()) {
            isSerialized = constructIsSerialize(modifiedFieldsPath, modifiedEnumsPath,
                    modifiedTypeHierarchyPath);
        }
        boundary = new Boundary();
        if (enableInvariantCombination)
            invariantCombination = new InvariantCombination();
    }

    public GraphPattern getGraphPattern(String className, int dumpId, String context) {
        updateSketches(context);

        if (!dumpId2ObjCoverageWithContext.containsKey(dumpId)) {
            dumpId2ObjCoverageWithContext.put(dumpId, new HashMap<>());
        }
        Map<String, Map<String, GraphPattern>> contextObjCoverage = dumpId2ObjCoverageWithContext
                .get(dumpId);

        if (limitMaxPatternNum && !contextObjCoverage.containsKey(context)
                && contextObjCoverage.size() >= maxPatternNum) {
            // Find the closest context
            double maxSimilarity = 0;
            String closestContext = null;
            for (String oriContext : contextObjCoverage.keySet()) {
                double[] jaccardResults = jaccard(sketches.get(oriContext), sketches.get(context));
                double similarity = jaccardResults[1];
                if (similarity > maxSimilarity) {
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
        monitorCreationContext(obj, null);
    }

    public void monitorCreationContext(Object obj, Object contextObj) {
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

        boolean changed = false;
        if (collectContextGraphPattern) {
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

    // Deprecated!
    private int getGroupIdEditDistance(int dumpId, String context) {
        if (!dumpId2Context2GroupId.containsKey(dumpId)) {
            dumpId2Context2GroupId.put(dumpId, new HashMap<>());
        }
        Map<String, Integer> context2GroupId = dumpId2Context2GroupId.get(dumpId);
        if (context2GroupId.containsKey(context)) {
            return context2GroupId.get(context);
        }
        // Find the closest stack trace
        int minDistance = Integer.MAX_VALUE;
        int minGroupId = -1;
        for (String oriContext : context2GroupId.keySet()) {
            if (minDistance > levenshteinDistance.apply(context, oriContext)) {
                minDistance = levenshteinDistance.apply(context, oriContext);
                minGroupId = context2GroupId.get(oriContext);
            }
        }
        if (minDistance <= editDistanceThreshold) {
            context2GroupId.put(context, minGroupId);
            return minGroupId;
        }
        // Extract current group id
        if (!dumpId2CurrentGroupId.containsKey(dumpId)) {
            dumpId2CurrentGroupId.put(dumpId, 0);
        }
        int groupId = dumpId2CurrentGroupId.get(dumpId);
        dumpId2CurrentGroupId.put(dumpId, groupId + 1);
        context2GroupId.put(context, groupId);
        return groupId;
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
        return merge(otherObjCoverage, -1, false);
    }

    public FormatCoverageStatus merge(ObjectGraphCoverage otherObjCoverage, int testId) {
        return merge(otherObjCoverage, testId, false);
    }

    public FormatCoverageStatus merge(ObjectGraphCoverage otherObjCoverage, int testId,
            boolean groupByContext) {
        return merge(otherObjCoverage, "", testId, groupByContext);
    }

    public FormatCoverageStatus merge(ObjectGraphCoverage otherObjCoverage, String identifier,
            int testId, boolean groupByContext) {
        FormatCoverageStatus formatCoverageStatus = new FormatCoverageStatus();
        if (otherObjCoverage == null)
            return formatCoverageStatus;

        if (groupByContext) {
            mergeTopGraphPatternWithGrouping(otherObjCoverage, formatCoverageStatus);
        } else {
            mergeTopGraphPatternWithoutGrouping(otherObjCoverage, formatCoverageStatus);
        }

        mergeSpecialInvariant(otherObjCoverage, formatCoverageStatus);

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
            FormatCoverageStatus formatCoverageStatus) {
        mergeAccumCoverage(accumDumpId2ObjCoverageWithContext,
                otherObjCoverage.dumpId2ObjCoverageWithContext, formatCoverageStatus);
    }

    private void mergeTopGraphPatternWithoutGrouping(ObjectGraphCoverage otherObjCoverage,
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
            Map<Integer, Map<String, Map<String, GraphPattern>>> dumpId2ObjCoverage1,
            Map<Integer, Map<String, Map<String, GraphPattern>>> dumpId2ObjCoverage2,
            FormatCoverageStatus formatCoverageStatus) {
        for (int dumpId : dumpId2ObjCoverage2.keySet()) {
            Map<String, Map<String, GraphPattern>> otherObjCoverageWithContext = dumpId2ObjCoverage2
                    .get(dumpId);
            if (otherObjCoverageWithContext == null)
                continue;
            Map<String, Map<String, GraphPattern>> objCoverageWithContext = dumpId2ObjCoverage1
                    .computeIfAbsent(dumpId, k -> new HashMap<>());
            mergeGraphPattern(objCoverageWithContext, otherObjCoverageWithContext,
                    formatCoverageStatus, dumpId);
        }
    }

    private void mergeAccumCoverage(
            Map<Integer, Map<Integer, Map<String, GraphPattern>>> dumpId2ObjCoverage1,
            Map<Integer, Map<String, Map<String, GraphPattern>>> dumpId2ObjCoverage2,
            FormatCoverageStatus formatCoverageStatus) {
        for (int dumpId : dumpId2ObjCoverage2.keySet()) {
            long time1 = System.currentTimeMillis();

            Map<String, Map<String, GraphPattern>> otherObjCoverageWithContext = dumpId2ObjCoverage2
                    .get(dumpId);
            if (otherObjCoverageWithContext == null)
                continue;
            Map<Integer, Map<String, GraphPattern>> objCoverageWithContext = dumpId2ObjCoverage1
                    .computeIfAbsent(dumpId, k -> new HashMap<>());

            mergeAccumGraphPattern(objCoverageWithContext, otherObjCoverageWithContext,
                    formatCoverageStatus, dumpId);

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
                    Runtime.log("[hklog] Add new graphPattern for " + className
                            + ", context hashcode = " + context.hashCode() + ", dumpId = "
                            + dumpId);
                    classInfo.put(className, SerializationUtils.clone(otherGraphPattern));
                    formatCoverageStatus.newFormat = true;
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
            FormatCoverageStatus formatCoverageStatus, int dumpId) {
        if (otherObjCoverageWithContext == null)
            return;
        for (String context : otherObjCoverageWithContext.keySet()) {
            Map<String, GraphPattern> otherClassInfo = otherObjCoverageWithContext.get(context);
            if (otherClassInfo == null)
                continue;

            // compute group ID
            int groupId;
            if (useLevenshteinDistance) {
                groupId = getGroupIdEditDistance(dumpId, context);
            } else {
                groupId = getGroupId(dumpId, context);
            }
            if (!objCoverageWithContext.containsKey(groupId)) {
                objCoverageWithContext.put(groupId, new HashMap<>());
            }
            Map<String, GraphPattern> classInfo = objCoverageWithContext.get(groupId);
            for (String className : otherClassInfo.keySet()) {
                GraphPattern otherGraphPattern = otherClassInfo.get(className);
                if (otherGraphPattern == null)
                    continue;
                GraphPattern graphPattern = classInfo.get(className);
                if (graphPattern == null) {
                    Runtime.log("[hklog] Add new graphPattern for " + className
                            + ", context hashcode = " + context.hashCode() + ", dumpId = "
                            + dumpId);
                    long time1 = System.currentTimeMillis();
                    classInfo.put(className, SerializationUtils.clone(otherGraphPattern));
                    long time2 = System.currentTimeMillis();
                    if (Runtime.debug) {
                        double time = (time2 - time1) / 1000.;
                        Runtime.log(String.format(
                                "[hklog] Add new graphPattern for %s, context hashcode = %d, clone time = %.2fs",
                                className, context.hashCode(), time));
                        if (time > 1)
                            Runtime.log("[hklog] clone slow for " + className + ", time = " + time
                                    + ", node num = "
                                    + otherGraphPattern.getGraph().vertexSet().size()
                                    + ", edge num = "
                                    + otherGraphPattern.getGraph().edgeSet().size());
                    }
                    formatCoverageStatus.newFormat = true;
                } else {
                    LogInfo logInfo = new LogInfo(dumpId, context.hashCode());
                    FormatCoverageStatus otherFormatCoverageStatus = graphPattern
                            .merge(otherGraphPattern, logInfo);
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

    public static IsSerialize constructIsSerialize(Path modifiedFieldsPath, Path modifiedEnumsPath,
            Path modifiedHierarchyPath) {
        Map<String, Set<String>> modifiedFields = Utils
                .loadModifiedFields(modifiedFieldsPath.toString());
        Set<String> modifiedEnums = Utils.loadSetFromFile(modifiedEnumsPath.toString());
        Set<String> typeWithModifiedHierarchy = Utils
                .loadSetFromFile(modifiedHierarchyPath.toString());
        return new IsSerialize(modifiedFields, modifiedEnums, typeWithModifiedHierarchy);
    }
}
