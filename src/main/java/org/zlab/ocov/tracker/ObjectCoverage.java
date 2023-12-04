package org.zlab.ocov.tracker;

import org.zlab.ocov.tracker.type.IntegerType;
import org.zlab.ocov.tracker.type.ObjectType;
import org.apache.commons.lang3.SerializationUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ObjectCoverage {
    // Only contain the top level objects: class name -> class info
    public Map<String, ClassInfo> objCoverage = new HashMap<>();

    // contains all target class info, clone one from this if we need
    // a new ClassInfo
    public static Map<String, ClassInfo> baseClassInfo = new HashMap<>();

    static {
        initBaseClassInfo();
    }

    public static void initBaseClassInfo() {
        // preset a list of objects to watch
        /*
         * The sub objects can be tracked at 2 levels:
         * 1. Separate the sub object from the parent object.
         * 2. Track the sub object as part of the parent object.
         * Separate them would give us a better accuracy.
         */
        // Test Purpose
        initExampleClassInfo();
    }

    public static void initExampleClassInfo() {
        ClassInfo classInfoA = new ClassInfo();
        classInfoA.fields.put("a", new IntegerType());
        classInfoA.fields.put("b", new IntegerType());
        classInfoA.fields.put("bObj",
                new ObjectType("org.zlab.ocov.dumper.TestObjectGraphDumper$TargetClassA.TargetClassB"));

        ClassInfo classInfoB = new ClassInfo();
        classInfoB.fields.put("value", new IntegerType());

        ClassInfo classInfoC = new ClassInfo();
        classInfoC.fields.put("c", new IntegerType());

        baseClassInfo.put("org.zlab.ocov.dumper.TestObjectGraphDumper$TargetClassA", classInfoA);
        baseClassInfo.put("org.zlab.ocov.dumper.TestObjectGraphDumper$TargetClassA.TargetClassB", classInfoB);
        baseClassInfo.put("org.zlab.ocov.dumper.TestObjectGraphDumper$TargetClassC", classInfoC);
    }

    public ObjectCoverage() {
        // we have a list of objects to watch
        // Classname = org.zlab.ocov.dumper.TestObjectGraphDumper.TargetClassA
        Set<String> topObjects = new HashSet<>();
        topObjects.add("org.zlab.ocov.dumper.TestObjectGraphDumper$TargetClassA");
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
        boolean isNew = false;
        // get object class name
        String className = obj.getClass().getName();
        // get class info
        ClassInfo classInfo = objCoverage.get(className);
        if (classInfo == null) {
            return false;
        }
        isNew = classInfo.update(obj);
        return isNew;
    }

}
