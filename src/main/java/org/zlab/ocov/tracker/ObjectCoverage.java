package org.zlab.ocov.tracker;

import org.zlab.ocov.Utils;
import org.zlab.ocov.tracker.type.CollectionType;
import org.zlab.ocov.tracker.type.IntegerType;
import org.zlab.ocov.tracker.type.ObjectType;
import org.apache.commons.lang3.SerializationUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ObjectCoverage {
    // Only contain the top level objects: class name -> class info
    public Map<String, ClassInfo> objCoverage = new HashMap<>();
    public Set<String> topObjects;

    // contains all target class info, clone one from this if we need
    // a new ClassInfo
    public Map<String, ClassInfo> baseClassInfo;

    public void initExample() {
        baseClassInfo = readClassInfo(Paths.get("input/baseClassInfo.json"));
        topObjects = readTopObjects(Paths.get("input/topObjects.json"));
    }

    public Set<String> initExampleTopObjects() {
        Set<String> topObjects = new HashSet<>();
        topObjects.add("org.zlab.ocov.dumper.TestObjectGraphDumper$TargetClassA");
        topObjects.add("org.zlab.ocov.dumper.TestObjectGraphDumper$TargetClassD");
        return topObjects;
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
                if (fieldType.equals("int")) {
                    classInfoItem.fields.put(fieldName, new IntegerType());
                } else if (fieldType.equals("java.util.List")) {
                    classInfoItem.fields.put(fieldName, new CollectionType());
                } else {
                    classInfoItem.fields.put(fieldName, new ObjectType());
                }
            }
            classInfo.put(className, classInfoItem);
        }
        return classInfo;
    }

    public static Set<String> readTopObjects(Path file) {
        return Utils.loadSetFromFile(file.toString());
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
                throw new RuntimeException("ClassInfo not found for " + className);
            }
        }
    }

    public boolean update(Object obj) {
        // get object class name
        String className = obj.getClass().getName();
        // get class info
        ClassInfo classInfo = objCoverage.get(className);
        if (classInfo == null) {
            return false;
        }
        Set<String> visitedClasses = new HashSet<>();
        if (!Utils.isPrimitiveType(className)) {
            visitedClasses.add(className);
        }
        return classInfo.update(obj, visitedClasses, baseClassInfo);
    }

}
