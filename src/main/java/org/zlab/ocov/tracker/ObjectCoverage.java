package org.zlab.ocov.tracker;

import org.zlab.ocov.Utils;
import org.apache.commons.lang3.SerializationUtils;
import org.zlab.ocov.tracker.type.TypeInfo;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ObjectCoverage implements Serializable {
    private static final long serialVersionUID = 20231215L;

    // Only contain the top level objects: class name -> class info
    public Map<String, ClassInfo> objCoverage = new HashMap<>();

    // contains all target class info, clone one from this if we need
    // a new ClassInfo
    public Map<String, ClassInfo> baseClassInfo;
    public Set<String> topObjects;

    public EqualitySet equalitySet;

    public ObjectCoverage() {
        // for json
    }

    public ObjectCoverage(Path baseClassInfoPath, Path topObjectsPath) {
        this(baseClassInfoPath, topObjectsPath, null);
    }

    public ObjectCoverage(Path baseClassInfoPath, Path topObjectsPath, Path comparableClassesPath) {
        baseClassInfo = readClassInfo(baseClassInfoPath);
        topObjects = readTopObjects(topObjectsPath);
        if (comparableClassesPath != null && comparableClassesPath.toFile().exists()) {
            equalitySet = constructEqualitySet(comparableClassesPath);
        }

        // Get classinfo from base
        for (String className : topObjects) {
            ClassInfo classInfo = baseClassInfo.get(className);
            if (classInfo != null) {
                // Copy the class info to a new object and put it into the map
                ClassInfo copyClassInfo = SerializationUtils.clone(classInfo);
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
        // get object class name
        String className = obj.getClass().getName();
        // get class info
        ClassInfo classInfo = objCoverage.get(className);
        if (classInfo == null) {
            // Runtime.log("[hklog] classInfo is null for " + className);
            return false;
        }
        // Runtime.log("[hklog] classInfo = " + className);
        return classInfo.update(obj, baseClassInfo, dumpId, equalitySet);
    }

    public boolean merge(ObjectCoverage otherObjCoverage) {
        return merge(otherObjCoverage, -1);
    }

    public boolean merge(ObjectCoverage otherObjCoverage, int testId) {
        // The coverage's class info should be similar
        // Let's include all new here
        boolean newCoverage = false;
        // Normal object coverage merge
        for (String className : otherObjCoverage.objCoverage.keySet()) {
            ClassInfo otherClassInfo = otherObjCoverage.objCoverage.get(className);
            if (otherClassInfo == null) {
                // Skip this
                continue;
            }
            ClassInfo classInfo = objCoverage.get(className);
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
        // TODO: Equality set merge
        if (equalitySet == null) {
            if (otherObjCoverage.equalitySet != null) {
                equalitySet = otherObjCoverage.equalitySet;
                newCoverage = true;
            }
        } else {
            if (equalitySet.merge(otherObjCoverage.equalitySet)) {
                newCoverage = true;
            }
        }
        if (newCoverage) {
            Runtime.log(
                    String.format("[hklog] --- Merged new coverage from testId: %d ---", testId));
        }
        return newCoverage;
    }

    public static Map<String, ClassInfo> readClassInfo(Path file) {
        Map<String, Map<String, String>> classInfoOri = Utils.loadMapFromFile(file.toString());
        // transform it into Map<String, ClassInfo>
        Map<String, ClassInfo> classInfo = new HashMap<>();
        assert classInfoOri != null;
        for (String className : classInfoOri.keySet()) {
            ClassInfo classInfoItem = new ClassInfo(className);
            for (String fieldName : classInfoOri.get(className).keySet()) {
                String fieldType = classInfoOri.get(className).get(fieldName);
                // Map from fieldType to TypeInfo
                TypeInfo typeInfo = TypeInfo.createTypeInfo(fieldType, className + "." + fieldName);
                if (typeInfo == null) {
                    // skip it
                    continue;
                }
                classInfoItem.fields.put(fieldName, typeInfo);
            }
            classInfo.put(className, classInfoItem);
        }
        return classInfo;
    }

    public static Set<String> readTopObjects(Path file) {
        return Utils.loadSetFromFile(file.toString());
    }

    public static EqualitySet constructEqualitySet(Path file) {
        Set<String> comparableClasses = Utils.loadSetFromFile(file.toString());
        return new EqualitySet(comparableClasses);
    }

    public void initExample() {
        baseClassInfo = readClassInfo(Paths.get("input/baseClassInfo.json"));
        topObjects = readTopObjects(Paths.get("input/topObjects.json"));
    }

}
