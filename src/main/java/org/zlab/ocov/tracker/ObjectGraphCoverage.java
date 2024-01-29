package org.zlab.ocov.tracker;

import org.apache.commons.lang3.SerializationUtils;
import org.zlab.ocov.Utils;
import org.zlab.ocov.tracker.graph.GraphPattern;
import org.zlab.ocov.tracker.graph.ObjectGraph;
import org.zlab.ocov.tracker.graph.ObjectGraphDumper;
import org.zlab.ocov.tracker.inv.unary.LogInfo;
import org.zlab.ocov.tracker.type.TypeInfo;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ObjectGraphCoverage implements Serializable {
    private static final long serialVersionUID = 20231215L;

    // Only contain the top level objects: class name -> class info
    public Map<String, GraphPattern> objCoverage = new HashMap<>();

    public Set<Integer> visitedObjects = new HashSet<>();

    public Map<String, GraphPattern> baseClassInfo;
    public Set<String> topObjects;

    public EqualitySet equalitySet;
    public IsSerialize isSerialized;

    // Graph Implementation
    ObjectGraphDumper objectGraphDumper;

    public ObjectGraphCoverage() {
        // for json
    }

    public ObjectGraphCoverage(Path baseClassInfoPath, Path topObjectsPath) {
        this(baseClassInfoPath, topObjectsPath, null, null, null);
    }

    public ObjectGraphCoverage(Path baseClassInfoPath, Path topObjectsPath,
            Path comparableClassesPath) {
        this(baseClassInfoPath, topObjectsPath, comparableClassesPath, null, null);
    }

    public ObjectGraphCoverage(Path baseClassInfoPath, Path topObjectsPath,
            Path comparableClassesPath, Path modifiedFieldsPath, Path modifiedEnumsPath) {
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

        // Get classInfo
        for (String className : topObjects) {
            GraphPattern classInfo = baseClassInfo.get(className);
            if (classInfo != null) {
                // Copy the class info to a new object and put it into the map
                GraphPattern copyClassInfo = SerializationUtils.clone(classInfo);
                objCoverage.put(className, copyClassInfo);
            } else {
                // throw new RuntimeException("ClassInfo not found for " + className);
                // skip it
                // Runtime.log("[hklog] ClassInfo not found for " + className);
            }
        }
    }

    public boolean update(Object obj) {
        return update(obj, -1);
    }

    public boolean update(Object obj, int dumpId) {
        if (obj == null)
            return false;
        String className = obj.getClass().getName();
        GraphPattern classInfo = objCoverage.get(className);
        if (classInfo == null)
            return false;

        ObjectGraph objectGraph = objectGraphDumper.dump(obj);
        LogInfo logInfo = new LogInfo(dumpId);

        boolean ret = classInfo.update(objectGraph, baseClassInfo, logInfo, equalitySet,
                isSerialized);

        Integer objId = System.identityHashCode(obj);
        if (visitedObjects.contains(objId))
            return false;
        visitedObjects.add(objId);

        if (equalitySet != null)
            equalitySet.dumpSameObjectGraph(dumpId);
        return ret;
    }

    public void clear() {
        // try to separate format coverage across tests
        visitedObjects.clear();
        equalitySet.clear();
    }

    public boolean merge(ObjectGraphCoverage otherObjCoverage) {
        return merge(otherObjCoverage, -1);
    }

    public boolean merge(ObjectGraphCoverage otherObjCoverage, int testId) {
        // The coverage's class info should be similar
        // Let's include all new here
        boolean newCoverage = false;
        // Normal object coverage merge
        for (String className : otherObjCoverage.objCoverage.keySet()) {
            GraphPattern otherClassInfo = otherObjCoverage.objCoverage.get(className);
            if (otherClassInfo == null) {
                // Skip this
                continue;
            }
            GraphPattern classInfo = objCoverage.get(className);
            if (classInfo == null) {
                // Add it
                Runtime.log("[hklog] Add new classInfo for " + className);
                objCoverage.put(className, otherClassInfo);
                newCoverage = true;
            } else {
                // merge it
                if (classInfo.merge(otherClassInfo)) {
                    newCoverage = true;
                }
            }
        }
        if (equalitySet == null) {
            // Avoid providing the information for upfuzz
            if (otherObjCoverage.equalitySet != null) {
                equalitySet = SerializationUtils.clone(otherObjCoverage.equalitySet);
                newCoverage = true;
            }
        } else {
            if (equalitySet.merge(otherObjCoverage.equalitySet)) {
                newCoverage = true;
            }
        }
        if (isSerialized == null) {
            if (otherObjCoverage.isSerialized != null) {
                isSerialized = SerializationUtils.clone(otherObjCoverage.isSerialized);
                newCoverage = true;
            }
        } else {
            if (isSerialized.merge(otherObjCoverage.isSerialized)) {
                newCoverage = true;
            }
        }
        if (newCoverage) {
            Runtime.log(
                    String.format("[hklog] --- Merged new coverage from testId: %d ---", testId));
        }
        return newCoverage;
    }

    public static IsSerialize constructIsSerialize(Path modifiedFieldsPath,
            Path modifiedEnumsPath) {
        Map<String, Set<String>> modifiedFields = Utils
                .loadModifiedFields(modifiedFieldsPath.toString());
        Set<String> modifiedEnums = Utils.loadSetFromFile(modifiedEnumsPath.toString());
        return new IsSerialize(modifiedFields, modifiedEnums);
    }

}
