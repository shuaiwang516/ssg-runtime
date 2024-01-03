package org.zlab.ocov.tracker;

import org.zlab.ocov.Utils;
import org.zlab.ocov.tracker.type.CollectionType;
import org.zlab.ocov.tracker.type.IntegerType;
import org.zlab.ocov.tracker.type.ObjectType;
import org.apache.commons.lang3.SerializationUtils;
import org.zlab.ocov.tracker.type.TypeInfo;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
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

    public ObjectCoverage() {
        // for json
    }

    public ObjectCoverage(Path baseClassInfoPath, Path topObjectsPath) {
        baseClassInfo = readClassInfo(baseClassInfoPath);
        topObjects = readTopObjects(topObjectsPath);

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
        // get object class name
        String className = obj.getClass().getName();
        // get class info
        ClassInfo classInfo = objCoverage.get(className);
        if (classInfo == null) {
            // Runtime.log("[hklog] classInfo is null for " + className);
            return false;
        }
        // Runtime.log("[hklog] classInfo = " + className);
        return classInfo.update(obj, baseClassInfo);
    }

    public boolean merge(ObjectCoverage otherObjCoverage) {
        // The coverage's class info should be similar
        // Let's include all new here
        boolean newCoverage = false;
        for (String className : otherObjCoverage.objCoverage.keySet()) {
            ClassInfo otherClassInfo = otherObjCoverage.objCoverage.get(className);
            if (otherClassInfo == null) {
                // Skip this
                continue;
            }
            ClassInfo classInfo = objCoverage.get(className);
            if (classInfo == null) {
                // Add it
                objCoverage.put(className, otherClassInfo);
                newCoverage = true;
            } else {
                // merge it
                if (classInfo.merge(otherClassInfo))
                    newCoverage = true;
            }
        }
        return newCoverage;
    }

    public static Map<String, ClassInfo> readClassInfo(Path file) {
        Map<String, Map<String, String>> classInfoOri = Utils.loadMapFromFile(file.toString());
        // transform it into Map<String, ClassInfo>
        Map<String, ClassInfo> classInfo = new HashMap<>();
        assert classInfoOri != null;
        for (String className : classInfoOri.keySet()) {
            ClassInfo classInfoItem = new ClassInfo();
            for (String fieldName : classInfoOri.get(className).keySet()) {
                String fieldType = classInfoOri.get(className).get(fieldName);
                // Map from fieldType to TypeInfo
                TypeInfo typeInfo = TypeInfo.createTypeInfo(fieldType);
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

    public void initExample() {
        baseClassInfo = readClassInfo(Paths.get("input/baseClassInfo.json"));
        topObjects = readTopObjects(Paths.get("input/topObjects.json"));
    }

}
