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
    public static Map<String, ClassInfo> baseClassInfo;

    public void initBaseClassInfo() {
        // preset a list of objects to watch
        /*
         * The sub objects can be tracked at 2 levels: 1. Separate the sub object from
         * the parent object. 2. Track the sub object as part of the parent object.
         * Separate them would give us a better accuracy.
         */
        // Test Purpose
        initExample();
    }

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

    public ObjectCoverage() {
        // Input1: A list of classnames, fieldnames and type to watch
        // Input2: List<String> topObjects
        initBaseClassInfo();
        Set<String> topObjects = initExampleTopObjects();
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
        return classInfo.update(obj);
    }

}
