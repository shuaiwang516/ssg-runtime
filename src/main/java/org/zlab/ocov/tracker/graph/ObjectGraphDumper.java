package org.zlab.ocov.tracker.graph;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.zlab.ocov.tracker.Runtime.log;

public class ObjectGraphDumper implements Serializable {
    private static final long serialVersionUID = 20231215L;

    public final boolean computeSize = true;

    private final Map<String, Map<String, String>> classInfo;
    private final Set<String> comparableClasses;

    public ObjectGraphDumper(Map<String, Map<String, String>> classInfo) {
        this.classInfo = classInfo;
        this.comparableClasses = new HashSet<>();
    }

    public ObjectGraphDumper(Map<String, Map<String, String>> classInfo,
            Set<String> comparableClasses) {
        this.classInfo = classInfo;
        this.comparableClasses = comparableClasses;
    }

    /**
     * Given an object, traverse its reference graph and dump into ObjectGraph
     */
    public ObjectGraph dump(Object obj) {
        if (obj == null || !classInfo.containsKey(obj.getClass().getName()))
            return null;

        ObjectGraph.Vertex vertex;
        if (computeSize) {
            vertex = new ObjectGraph.Vertex(obj.getClass().getName(), getValue(obj),
                    System.identityHashCode(obj), invokeSizeMethodIfExists(obj));
        } else {
            vertex = new ObjectGraph.Vertex(obj.getClass().getName(), getValue(obj),
                    System.identityHashCode(obj));
        }
        ObjectGraph objectGraph = new ObjectGraph(vertex);
        String className = obj.getClass().getName();
        processObject(objectGraph, obj, className, vertex);
        return objectGraph;
    }

    public void processObject(ObjectGraph objectGraph, Object obj, String className,
            ObjectGraph.Vertex vertex) {
        // Traverse the object graph and create the object graph
        if (obj == null) {
            return;
        }
        try {
            // debug
            // if (obj.getClass().getName().contains("IndexEntry")) {
            // log("[hklog] processing IndexEntry object: " + obj);
            // }
            // process all fields
            Class<?> currentClass = obj.getClass();
            while (currentClass != Object.class) {
                Field[] fields = currentClass.getDeclaredFields();
                for (Field field : fields) {
                    if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())
                            || !java.lang.reflect.Modifier.isFinal(field.getModifiers())) {
                        field.setAccessible(true);
                        Object curObj = field.get(obj);
                        String fieldName = field.getName();
                        if (isSerializedField(className, fieldName)) {
                            addVertex(curObj, objectGraph, vertex, fieldName);
                        }
                    }
                }
                currentClass = currentClass.getSuperclass(); // Move to the superclass
            }
            // Handle array/collection/map
            if (obj instanceof Collection) {
                for (Object curObj : (java.util.Collection) obj) {
                    addVertex(curObj, objectGraph, vertex, "collection_item");
                }
            } else if (obj instanceof Map) {
                for (Object curObj : ((java.util.Map) obj).keySet()) {
                    addVertex(curObj, objectGraph, vertex, "map_keyItem");
                }
                for (Object curObj : ((java.util.Map) obj).values()) {
                    addVertex(curObj, objectGraph, vertex, "map_valueItem");
                }
            } else if (obj.getClass().isArray()) {
                int length = Array.getLength(obj);
                for (int i = 0; i < length; i++) {
                    Object item = Array.get(obj, i);
                    addVertex(item, objectGraph, vertex, "array_item");
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void addVertex(Object object, ObjectGraph objectGraph, ObjectGraph.Vertex vertex,
            String edgeName) {
        ObjectGraph.Vertex curVertex;
        String curClassName = null;
        if (object == null) {
            curVertex = new ObjectGraph.Vertex("null", null, 0);
        } else {
            curClassName = object.getClass().getName();
            if (computeSize) {
                curVertex = new ObjectGraph.Vertex(curClassName, getValue(object),
                        System.identityHashCode(object), invokeSizeMethodIfExists(object));
            } else {
                curVertex = new ObjectGraph.Vertex(curClassName, getValue(object),
                        System.identityHashCode(object));
            }
        }
        objectGraph.graph.addVertex(curVertex);
        objectGraph.graph.addEdge(vertex, curVertex, new ObjectGraph.Edge(edgeName));
        processObject(objectGraph, object, curClassName, curVertex);
    }

    /**
     * If it's a primitive, we clone one, otherwise, we keep the hashcode. However,
     * for some fields, we don't need to record the hashcode.
     */
    public Object getValue(Object obj) {
        // check whether it's a primitive type
        Class<?> className = obj.getClass();
        String classNameStr = className.getName();
        if (isPrimitive(classNameStr) || className.isEnum()) {
            // Directly return this object
            return obj;
        } else {
            if (comparableClasses.contains(classNameStr)) {
                return obj.hashCode();
            }
            // we don't care other values
        }
        return System.identityHashCode(obj);
    }

    public boolean isSerializedField(String className, String fieldName) {
        return classInfo.containsKey(className) && classInfo.get(className).containsKey(fieldName);
    }

    public static boolean isPrimitive(String className) {
        return className.equals("java.lang.Integer") || className.equals("java.lang.Long")
                || className.equals("java.lang.Float") || className.equals("java.lang.Double")
                || className.equals("java.lang.Boolean") || className.equals("java.lang.Character")
                || className.equals("java.lang.Byte") || className.equals("java.lang.Short")
                || className.equals("java.lang.String");
    }

    public static Integer invokeSizeMethodIfExists(Object obj) {
        try {
            Class<?> clazz = obj.getClass();
            while (clazz != null) {
                for (Method method : clazz.getDeclaredMethods()) {
                    if ((method.getName().equals("size") || method.getName().equals("Size"))
                            && method.getParameterTypes().length == 0
                            && method.getReturnType() == int.class) {
                        method.setAccessible(true);
                        return (Integer) method.invoke(obj);
                    }
                }
                clazz = clazz.getSuperclass();
            }
        } catch (Exception e) {
            // e.printStackTrace();
        }
        return null;
    }

}
