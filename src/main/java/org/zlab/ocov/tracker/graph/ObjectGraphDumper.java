package org.zlab.ocov.tracker.graph;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ObjectGraphDumper implements Serializable {
    private static final long serialVersionUID = 20231215L;

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
     * Given an object, dump the object graph into ObjectGraph
     */
    public ObjectGraph dump(Object obj) {
        if (obj == null)
            return null;

        ObjectGraph.Vertex vertex = new ObjectGraph.Vertex(obj.getClass().getName(), getValue(obj),
                System.identityHashCode(obj));
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
            // process all fields
            Class<?> currentClass = obj.getClass();
            while (currentClass != Object.class) { // Traverse up the class hierarchy
                Field[] fields = currentClass.getDeclaredFields();
                for (Field field : fields) {
                    if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())
                            || !java.lang.reflect.Modifier.isFinal(field.getModifiers())) {
                        field.setAccessible(true);
                        Object curObj = field.get(obj);
                        String fieldName = field.getName();
                        if (isSerializedField(className, fieldName)) {
                            ObjectGraph.Vertex curVertex;
                            String curClassName;
                            if (curObj == null) {
                                curVertex = new ObjectGraph.Vertex("null", null, 0);
                                curClassName = "null";
                            } else {
                                curClassName = curObj.getClass().getName();
                                curVertex = new ObjectGraph.Vertex(curClassName, getValue(curObj),
                                        System.identityHashCode(curObj));
                            }
                            objectGraph.graph.addVertex(curVertex);
                            objectGraph.graph.addEdge(vertex, curVertex,
                                    new ObjectGraph.Edge(fieldName));
                            processObject(objectGraph, curObj, curClassName, curVertex);
                        }
                    }
                }
                currentClass = currentClass.getSuperclass(); // Move to the superclass
            }
            // Handle array/collection/map
            if (obj instanceof Collection) {
                for (Object item : (java.util.Collection) obj) {
                    if (item == null) {
                        continue;
                    }
                    String curClassName = item.getClass().getName();
                    ObjectGraph.Vertex curVertex = new ObjectGraph.Vertex(curClassName,
                            getValue(item), System.identityHashCode(item));
                    objectGraph.graph.addVertex(curVertex);
                    objectGraph.graph.addEdge(vertex, curVertex,
                            new ObjectGraph.Edge("collection_item"));
                    processObject(objectGraph, item, curClassName, curVertex);
                }
            } else if (obj instanceof Map) {
                for (Object item : ((java.util.Map) obj).keySet()) {
                    if (item == null) {
                        continue;
                    }
                    String curClassName = item.getClass().getName();
                    ObjectGraph.Vertex curVertex = new ObjectGraph.Vertex(curClassName,
                            getValue(item), System.identityHashCode(item));
                    objectGraph.graph.addVertex(curVertex);
                    objectGraph.graph.addEdge(vertex, curVertex,
                            new ObjectGraph.Edge("map_keyItem"));
                    processObject(objectGraph, item, curClassName, curVertex);
                }
                for (Object item : ((java.util.Map) obj).values()) {
                    if (item == null) {
                        continue;
                    }
                    String curClassName = item.getClass().getName();
                    ObjectGraph.Vertex curVertex = new ObjectGraph.Vertex(curClassName,
                            getValue(item), System.identityHashCode(item));
                    objectGraph.graph.addVertex(curVertex);
                    objectGraph.graph.addEdge(vertex, curVertex,
                            new ObjectGraph.Edge("map_valueItem"));
                    processObject(objectGraph, item, curClassName, curVertex);
                }
            } else if (obj.getClass().isArray()) {
                for (Object item : (Object[]) obj) {
                    if (item == null) {
                        continue;
                    }
                    String curClassName = item.getClass().getName();
                    ObjectGraph.Vertex curVertex = new ObjectGraph.Vertex(curClassName,
                            getValue(item), System.identityHashCode(item));
                    objectGraph.graph.addVertex(curVertex);
                    objectGraph.graph.addEdge(vertex, curVertex,
                            new ObjectGraph.Edge("array_item"));
                    processObject(objectGraph, item, curClassName, curVertex);
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * If it's a primitive, we clone one, otherwise, we keep the hashcode. However,
     * for some fields, we don't need to record the hashcode.
     */
    public Object getValue(Object obj) {
        // check whether it's a primitive type
        Class<?> className = obj.getClass();
        if (isPrimitive(className.getName()) || className.isEnum()) {
            // Directly return this object
            return obj;
        } else {
            if (comparableClasses.contains(className.toString())) {
                return obj.hashCode();
            }
            // we don't care other values
        }
        return null;
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

}
