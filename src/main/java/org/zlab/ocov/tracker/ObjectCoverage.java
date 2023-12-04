package org.zlab.ocov.tracker;

import org.zlab.ocov.tracker.type.IntegerType;
import org.zlab.ocov.tracker.type.ObjectType;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class ObjectCoverage {
    // class name -> class info
    public Map<String, ClassInfo> objCoverage = new HashMap<>();

    public ObjectCoverage() {
        // we have a list of objects to watch
        // Classname = org.zlab.ocov.dumper.TestObjectGraphDumper.TargetClassA
        ClassInfo classInfo = new ClassInfo();
        classInfo.fields.put("a", new IntegerType());
        classInfo.fields.put("b", new IntegerType());
        classInfo.fields.put("bObj",
                new ObjectType("org.zlab.ocov.dumper.TestObjectGraphDumper$TargetClassA.TargetClassB"));
        objCoverage.put("org.zlab.ocov.dumper.TestObjectGraphDumper$TargetClassA", classInfo);
    }

    public boolean update(Object obj) {
        boolean newFormat = false;
        try {
            // get object class name
            String className = obj.getClass().getName();
            // get class info
            ClassInfo classInfo = objCoverage.get(className);
            if (classInfo == null) {
                return false;
            }
            Field[] fields = obj.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(obj);
                if (classInfo.update(field.getName(), value)) {
                    if (!newFormat)
                        newFormat = true;
                }
                System.out.println(field.getName() + ": " + value);
            }
            return newFormat;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }
}
