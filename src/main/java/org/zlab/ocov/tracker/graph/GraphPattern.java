package org.zlab.ocov.tracker.graph;

import org.apache.commons.lang3.SerializationUtils;
import org.jgrapht.Graph;
import org.jgrapht.graph.DirectedMultigraph;
import org.zlab.ocov.Utils;
import org.zlab.ocov.tracker.EqualitySet;
import org.zlab.ocov.tracker.IsSerialize;
import org.zlab.ocov.tracker.Runtime;
import org.zlab.ocov.tracker.graph.label.LabelConstraint;
import org.zlab.ocov.tracker.graph.label.ValueConstraint;
import org.zlab.ocov.tracker.graph.structure.AccumulatedSizeConstraint;
import org.zlab.ocov.tracker.graph.structure.OutDegreeConstraint;
import org.zlab.ocov.tracker.graph.structure.StructureConstraint;
import org.zlab.ocov.tracker.inv.unary.*;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;

public class GraphPattern implements Serializable {
    private static final long serialVersionUID = 20231215L;

    // Likely Invariant Options
    public static boolean enableSequenceBoundaryCheck = true;
    public static boolean enableAccumulatedSizeCheck = false;

    protected Vertex root;
    protected DirectedMultigraph<GraphPattern.Vertex, GraphPattern.Edge> graph;

    /**
     * If an array length is larger than this value, we sample values from the array
     */
    private static final int maxArrayLength = 100;
    private static final int arrayMaxSampleSize = 20;

    public static class Vertex implements Serializable {
        private static final long serialVersionUID = 20231215L;

        boolean isObjectType;

        final String type;
        final String itinerary;
        List<LabelConstraint> labelConstraints;
        List<StructureConstraint> structureConstraints;

        public Vertex(String type, String itinerary, boolean isObjectType,
                List<LabelConstraint> labelConstraints,
                List<StructureConstraint> structureConstraints) {
            this.type = type;
            this.itinerary = itinerary;
            this.isObjectType = isObjectType;
            this.labelConstraints = labelConstraints;
            this.structureConstraints = structureConstraints;
        }

        public static Vertex cloneWithNewItineraryPrefix(Vertex v, String itineraryPrefix) {
            return new Vertex(v.type, itineraryPrefix + "->" + v.itinerary, v.isObjectType,
                    new LinkedList<>(v.labelConstraints), new LinkedList<>(v.structureConstraints));
        }

        public boolean update(ObjectGraph.Vertex vertex, ObjectGraph objectGraph,
                GraphPattern graphPattern, Map<String, GraphPattern> graphPatternMap,
                LogInfo logInfo, EqualitySet equalitySet, IsSerialize isSerialized,
                Set<String> brokenInvs) {
            // Update label constraints
            boolean labelConstraintsChange = false;
            for (LabelConstraint labelConstraint : labelConstraints) {
                labelConstraint.checkPure(vertex, logInfo, itinerary, brokenInvs);
                if (labelConstraint.update(vertex, logInfo))
                    labelConstraintsChange = true;
            }
            // Update structure constraints
            boolean structureConstraintsChange = false;
            for (StructureConstraint structureConstraint : structureConstraints) {
                structureConstraint.checkPure(vertex, objectGraph, logInfo, itinerary, brokenInvs);
                if (structureConstraint.update(vertex, objectGraph, logInfo))
                    structureConstraintsChange = true;
            }
            boolean subGraphPatternChange = false;
            if (isObjectType) {
                boolean found = false;
                Set<GraphPattern.Edge> outgoingEdges = graphPattern.graph.outgoingEdgesOf(this);
                for (GraphPattern.Edge edge : outgoingEdges) {
                    GraphPattern.Vertex target = graphPattern.graph.getEdgeTarget(edge);
                    if (target.type.equals(vertex.type)) {
                        found = true;
                        subGraphPatternChange = target.update(vertex, objectGraph, graphPattern,
                                graphPatternMap, logInfo, equalitySet, isSerialized, brokenInvs);
                    }
                }
                if (!found) {
                    // If not found, create a new one
                    if (graphPatternMap.containsKey(vertex.type)) {
                        // Include the subgraph's edges and vertices
                        GraphPattern subGraphPattern = SerializationUtils
                                .clone(graphPatternMap.get(vertex.type));

                        for (Vertex v1 : subGraphPattern.graph.vertexSet())
                            graphPattern.graph
                                    .addVertex(cloneWithNewItineraryPrefix(v1, itinerary));
                        for (Edge edge : subGraphPattern.graph.edgeSet())
                            graphPattern.graph.addEdge(
                                    cloneWithNewItineraryPrefix(
                                            subGraphPattern.graph.getEdgeSource(edge), itinerary),
                                    cloneWithNewItineraryPrefix(
                                            subGraphPattern.graph.getEdgeTarget(edge), itinerary),
                                    edge);

                        // Connect two graphs
                        Vertex subGraphPatternRoot = cloneWithNewItineraryPrefix(
                                subGraphPattern.root, itinerary);

                        GraphPattern.Edge newEdge = new GraphPattern.Edge(vertex.type);
                        graphPattern.graph.addEdge(this, subGraphPatternRoot, newEdge);
                        subGraphPatternRoot.update(vertex, objectGraph, graphPattern,
                                graphPatternMap, logInfo, equalitySet, isSerialized, brokenInvs);
                        subGraphPatternChange = true;
                    }
                }
            } else {
                // The current object vertex won't be iterated again, process it
                if (equalitySet != null)
                    equalitySet.update(vertex, type, itinerary, objectGraph.root.identifyHash);
                if (isSerialized != null) {
                    if (vertex.value != null && vertex.value.getClass().isEnum())
                        isSerialized.updateVisitedEnums(vertex.value.getClass().getName(),
                                vertex.value.toString());
                }

                for (ObjectGraph.Edge edge : objectGraph.graph.outgoingEdgesOf(vertex)) {
                    // check whether the edge is in the graphPattern
                    Set<GraphPattern.Edge> outgoingEdges = graphPattern.graph.outgoingEdgesOf(this);
                    for (GraphPattern.Edge patternEdge : outgoingEdges) {
                        if (patternEdge.name.equals(edge.name)) {
                            GraphPattern.Vertex target = graphPattern.graph
                                    .getEdgeTarget(patternEdge);
                            if (target.update(objectGraph.graph.getEdgeTarget(edge), objectGraph,
                                    graphPattern, graphPatternMap, logInfo, equalitySet,
                                    isSerialized, brokenInvs)) {
                                subGraphPatternChange = true;
                            }
                            // there should only be one edge with the same name
                            break;
                        }
                    }
                }
            }
            return labelConstraintsChange || structureConstraintsChange || subGraphPatternChange;
        }

        public boolean update(Object obj, GraphPattern graphPattern,
                Map<String, GraphPattern> graphPatternMap, LogInfo logInfo, EqualitySet equalitySet,
                IsSerialize isSerialized, Set<String> brokenInvs, int objId, Set<Integer> visited) {
            Runtime.log("[debug] update vertex: dumpId = " + logInfo.dumpId + ", iti = " + itinerary
                    + ", current time = " + System.currentTimeMillis() + ", objId = " + objId);
            // Update label constraints
            boolean labelConstraintsChange = false;
            for (LabelConstraint labelConstraint : labelConstraints) {
                labelConstraint.checkPure(obj, logInfo, itinerary, brokenInvs);
                if (labelConstraint.update(obj, logInfo))
                    labelConstraintsChange = true;
            }
            // Update structure constraints
            boolean structureConstraintsChange = false;
            for (StructureConstraint structureConstraint : structureConstraints) {
                structureConstraint.checkPure(obj, logInfo, itinerary, brokenInvs);
                if (structureConstraint.update(obj, logInfo))
                    structureConstraintsChange = true;
            }
            boolean subGraphPatternChange = false;

            if (obj == null || visited.contains(System.identityHashCode(obj)))
                return labelConstraintsChange || structureConstraintsChange;

            String objectType = obj.getClass().getName();
            if (isObjectType) {
                boolean found = false;
                Set<GraphPattern.Edge> outgoingEdges = graphPattern.graph.outgoingEdgesOf(this);
                for (GraphPattern.Edge edge : outgoingEdges) {
                    GraphPattern.Vertex target = graphPattern.graph.getEdgeTarget(edge);
                    if (target.type.equals(objectType)) {
                        found = true;
                        subGraphPatternChange = target.update(obj, graphPattern, graphPatternMap,
                                logInfo, equalitySet, isSerialized, brokenInvs, objId, visited);
                    }
                }
                if (!found) {
                    // If not found, create a new one
                    if (graphPatternMap.containsKey(objectType)) {
                        // Include the subgraph's edges and vertices
                        GraphPattern subGraphPattern = SerializationUtils
                                .clone(graphPatternMap.get(objectType));

                        for (Vertex v1 : subGraphPattern.graph.vertexSet())
                            graphPattern.graph
                                    .addVertex(cloneWithNewItineraryPrefix(v1, itinerary));
                        for (Edge edge : subGraphPattern.graph.edgeSet())
                            graphPattern.graph.addEdge(
                                    cloneWithNewItineraryPrefix(
                                            subGraphPattern.graph.getEdgeSource(edge), itinerary),
                                    cloneWithNewItineraryPrefix(
                                            subGraphPattern.graph.getEdgeTarget(edge), itinerary),
                                    edge);

                        // Connect two graphs
                        Vertex subGraphPatternRoot = cloneWithNewItineraryPrefix(
                                subGraphPattern.root, itinerary);

                        GraphPattern.Edge newEdge = new GraphPattern.Edge(objectType);
                        graphPattern.graph.addEdge(this, subGraphPatternRoot, newEdge);
                        subGraphPatternRoot.update(obj, graphPattern, graphPatternMap, logInfo,
                                equalitySet, isSerialized, brokenInvs, objId, visited);
                        subGraphPatternChange = true;
                    }
                }
            } else {
                // The current object vertex won't be iterated again, process it
                if (equalitySet != null)
                    equalitySet.update(obj, objectType, itinerary, objId);
                if (isSerialized != null) {
                    if (obj.getClass().isEnum())
                        isSerialized.updateVisitedEnums(objectType, obj.toString());
                }
                visited.add(System.identityHashCode(obj));
                // Special process Map/Collection/Array
                if (obj instanceof Map) {
                    GraphPattern.Vertex mapKeyItemVertex = null;
                    GraphPattern.Vertex mapValueItemVertex = null;
                    Set<GraphPattern.Edge> outgoingEdges = graphPattern.graph.outgoingEdgesOf(this);
                    for (GraphPattern.Edge patternEdge : outgoingEdges) {
                        if (patternEdge.name.equals("map_keyItem")) {
                            mapKeyItemVertex = graphPattern.graph.getEdgeTarget(patternEdge);
                        }
                        if (patternEdge.name.equals("map_valueItem")) {
                            mapValueItemVertex = graphPattern.graph.getEdgeTarget(patternEdge);
                        }
                        if (mapKeyItemVertex != null && mapValueItemVertex != null) {
                            break;
                        }
                    }
                    if (mapKeyItemVertex != null) {
                        int length = ((java.util.Map) obj).keySet().size();
                        List<Integer> sampleIdxs;
                        if (length > maxArrayLength) {
                            sampleIdxs = Utils.sampleIdxFromSize(length, arrayMaxSampleSize);
                        } else {
                            sampleIdxs = new ArrayList<>();
                            for (int i = 0; i < length; i++)
                                sampleIdxs.add(i);
                        }
                        for (int i : sampleIdxs) {
                            Object object = ((java.util.Map) obj).keySet().toArray()[i];
                            if (object == null) {
                                continue;
                            }
                            if (mapKeyItemVertex.update(object, graphPattern, graphPatternMap,
                                    logInfo, equalitySet, isSerialized, brokenInvs, objId, visited))
                                subGraphPatternChange = true;
                        }
                    }
                    if (mapValueItemVertex != null) {
                        int length = ((java.util.Map) obj).values().size();
                        List<Integer> sampleIdxs;
                        if (length > maxArrayLength) {
                            sampleIdxs = Utils.sampleIdxFromSize(length, arrayMaxSampleSize);
                        } else {
                            sampleIdxs = new ArrayList<>();
                            for (int i = 0; i < length; i++)
                                sampleIdxs.add(i);
                        }
                        for (int i : sampleIdxs) {
                            Object object = ((java.util.Map) obj).values().toArray()[i];
                            if (object == null) {
                                continue;
                            }
                            if (mapValueItemVertex.update(object, graphPattern, graphPatternMap,
                                    logInfo, equalitySet, isSerialized, brokenInvs, objId, visited))
                                subGraphPatternChange = true;
                        }
                    }
                } else if (obj instanceof Collection) {
                    GraphPattern.Vertex collectionItemVertex = null;
                    Set<GraphPattern.Edge> outgoingEdges = graphPattern.graph.outgoingEdgesOf(this);
                    for (GraphPattern.Edge patternEdge : outgoingEdges) {
                        if (patternEdge.name.equals("collection_item")) {
                            collectionItemVertex = graphPattern.graph.getEdgeTarget(patternEdge);
                            break;
                        }
                    }
                    if (collectionItemVertex != null) {
                        int length = ((Collection) obj).size();
                        List<Integer> sampleIdxs;
                        if (length > maxArrayLength) {
                            sampleIdxs = Utils.sampleIdxFromSize(length, arrayMaxSampleSize);
                        } else {
                            sampleIdxs = new ArrayList<>();
                            for (int i = 0; i < length; i++)
                                sampleIdxs.add(i);
                        }
                        for (int i : sampleIdxs) {
                            Object object = ((Collection) obj).toArray()[i];
                            if (object == null) {
                                continue;
                            }
                            if (collectionItemVertex.update(object, graphPattern, graphPatternMap,
                                    logInfo, equalitySet, isSerialized, brokenInvs, objId, visited))
                                subGraphPatternChange = true;
                        }
                    }
                } else if (obj.getClass().isArray()) {
                    GraphPattern.Vertex arrayItemVertex = null;
                    Set<GraphPattern.Edge> outgoingEdges = graphPattern.graph.outgoingEdgesOf(this);
                    for (GraphPattern.Edge patternEdge : outgoingEdges) {
                        if (patternEdge.name.equals("array_item")) {
                            arrayItemVertex = graphPattern.graph.getEdgeTarget(patternEdge);
                            break;
                        }
                    }
                    if (arrayItemVertex != null) {
                        int length = Array.getLength(obj);
                        List<Integer> sampleIdxs;
                        if (length > maxArrayLength) {
                            // Sample a few values from this array
                            sampleIdxs = Utils.sampleIdxFromSize(length, arrayMaxSampleSize);
                        } else {
                            sampleIdxs = new ArrayList<>();
                            for (int i = 0; i < length; i++) {
                                sampleIdxs.add(i);
                            }
                        }
                        for (int i : sampleIdxs) {
                            Object object = Array.get(obj, i);
                            if (object == null) {
                                continue;
                            }
                            if (arrayItemVertex.update(object, graphPattern, graphPatternMap,
                                    logInfo, equalitySet, isSerialized, brokenInvs, objId, visited))
                                subGraphPatternChange = true;
                        }
                    }
                } else {
                    // Iterate all fields of the object
                    try {
                        Class<?> currentClass = obj.getClass();
                        while (currentClass != Object.class) { // Traverse up the class hierarchy
                            Field[] fields = currentClass.getDeclaredFields();
                            for (Field field : fields) {
                                if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())
                                        || !java.lang.reflect.Modifier
                                                .isFinal(field.getModifiers())) {
                                    field.setAccessible(true);
                                    // Field Information
                                    Object value = field.get(obj);
                                    String fieldName = field.getName();
                                    if (value == obj || fieldName.equals("this$0")) {
                                        continue;
                                    }
                                    Set<GraphPattern.Edge> outgoingEdges = new HashSet<>(
                                            graphPattern.graph.outgoingEdgesOf(this));
                                    for (GraphPattern.Edge patternEdge : outgoingEdges) {
                                        if (patternEdge.name.equals(fieldName)) {
                                            GraphPattern.Vertex target = graphPattern.graph
                                                    .getEdgeTarget(patternEdge);
                                            if (target.update(value, graphPattern, graphPatternMap,
                                                    logInfo, equalitySet, isSerialized, brokenInvs,
                                                    objId, visited)) {
                                                subGraphPatternChange = true;
                                            }
                                            // there should only be one edge with the same name
                                            break;
                                        }
                                    }
                                }
                            }
                            currentClass = currentClass.getSuperclass(); // Move to the superclass
                        }
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            return labelConstraintsChange || structureConstraintsChange || subGraphPatternChange;
        }

        public boolean merge(Vertex otherVertex, GraphPattern otherGraphPattern,
                GraphPattern graphPattern) {
            boolean changed = false;
            // merge label constraints
            assert labelConstraints.size() == otherVertex.labelConstraints.size();
            for (int i = 0; i < labelConstraints.size(); i++) {
                if (labelConstraints.get(i).merge(otherVertex.labelConstraints.get(i), itinerary))
                    changed = true;
            }
            // merge structure constraints
            assert structureConstraints.size() == otherVertex.structureConstraints.size();
            for (int i = 0; i < structureConstraints.size(); i++) {
                if (structureConstraints.get(i).merge(otherVertex.structureConstraints.get(i),
                        itinerary)) {
                    changed = true;
                }
            }
            for (GraphPattern.Edge edge : otherGraphPattern.graph.outgoingEdgesOf(otherVertex)) {
                // check whether the edge is in the graphPattern
                boolean found = false;
                for (GraphPattern.Edge patternEdge : graphPattern.graph.outgoingEdgesOf(this)) {
                    if (patternEdge.name.equals(edge.name)) {
                        GraphPattern.Vertex target = graphPattern.graph.getEdgeTarget(patternEdge);
                        if (target.merge(otherGraphPattern.graph.getEdgeTarget(edge),
                                otherGraphPattern, graphPattern)) {
                            changed = true;
                        }
                        // there should only be one edge with the same name
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    GraphPattern.Vertex newVertex = SerializationUtils
                            .clone(otherGraphPattern.graph.getEdgeTarget(edge));
                    graphPattern.graph.addVertex(newVertex);
                    graphPattern.graph.addEdge(this, newVertex, edge);
                    newVertex.merge(otherGraphPattern.graph.getEdgeTarget(edge), otherGraphPattern,
                            graphPattern);
                    changed = true;
                }
            }
            return changed;
        }

        @Override
        public String toString() {
            return "Vertex{" + "type='" + type + "'" + ", itinerary='" + itinerary + "'"
                    + ", labelConstraints=" + labelConstraints + ", structureConstraints="
                    + structureConstraints + "}";
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Vertex)) {
                return false;
            }
            Vertex other = (Vertex) obj;
            return type.equals(other.type) && itinerary.equals(other.itinerary);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, itinerary);
        }
    }

    public static class Edge implements Serializable {
        private static final long serialVersionUID = 20231215L;

        public String name;

        public Edge(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "Edge{" + "name='" + name + "'" + '}';
        }
    }

    public GraphPattern() {
    }

    public GraphPattern(String type) {
        root = createBaseVertex(type);
        graph = new DirectedMultigraph<>(GraphPattern.Edge.class);
        graph.addVertex(root);
    }

    public boolean update(ObjectGraph objectGraph, Map<String, GraphPattern> graphPatternMap,
            LogInfo logInfo, EqualitySet equalitySet, IsSerialize isSerialized,
            Set<String> brokenInvs) {
        return root.update(objectGraph.root, objectGraph, this, graphPatternMap, logInfo,
                equalitySet, isSerialized, brokenInvs);
    }

    public boolean update(Object obj, Map<String, GraphPattern> graphPatternMap, LogInfo logInfo,
            EqualitySet equalitySet, IsSerialize isSerialized, Set<String> brokenInvs, int objId) {
        // TODO: avoid dumping the object graph (save one time overhead!)
        return root.update(obj, this, graphPatternMap, logInfo, equalitySet, isSerialized,
                brokenInvs, objId, new HashSet<>());
    }

    public boolean merge(GraphPattern other) {
        return root.merge(other.root, other, this);
    }

    public void print() {
        printGraph(root, graph);
    }

    public static Vertex createBaseVertex(String typeName) {
        List<LabelConstraint> valueConstraints = new LinkedList<>();
        List<StructureConstraint> structureConstraints = new LinkedList<>();
        return new Vertex(typeName, typeName, false, valueConstraints, structureConstraints);
    }

    public static Vertex createVertex(String typeName, String itinerary) {
        boolean isObjectType = false;

        List<LabelConstraint> valueConstraints = new LinkedList<>();
        List<StructureConstraint> structureConstraints = new LinkedList<>();

        List<UnaryInvariant> labelInvs = new LinkedList<>();
        labelInvs.add(new NullOnce());

        if (isScala(typeName)) {
            labelInvs.addAll(getScalaValueInvariants());
        } else if (typeName.equals("boolean") || typeName.equals("java.lang.Boolean")) {
            labelInvs.add(new TrueOnce());
            labelInvs.add(new FalseOnce());
        } else if (typeName.equals("char") || typeName.equals("java.lang.Character")) {
            // Empty
        } else if (typeName.equals("byte") || typeName.equals("java.lang.Byte")) {
            // Empty
        } else if (typeName.equals("java.lang.String")) {
            labelInvs.add(new EmptyStringOnce());
            labelInvs.add(new OneCharStringOnce());

            Set<Integer> targetValues = new HashSet<>();
            targetValues.add(0);
            targetValues.add(1);
            labelInvs.add(new RestStringSizeOnce(targetValues));
        } else if (isCollection(typeName)) {
            structureConstraints
                    .add(new OutDegreeConstraint(getCollectionSizeInvariants(), "collection_item"));
            if (enableAccumulatedSizeCheck)
                structureConstraints.add(new AccumulatedSizeConstraint(
                        getCollectionSizeInvariants(), "collection_item"));
        } else if (isMap(typeName)) {
            // keys
            structureConstraints
                    .add(new OutDegreeConstraint(getCollectionSizeInvariants(), "map_keyItem"));
            if (enableAccumulatedSizeCheck)
                structureConstraints.add(new AccumulatedSizeConstraint(
                        getCollectionSizeInvariants(), "map_keyItem"));
            // values
            structureConstraints
                    .add(new OutDegreeConstraint(getCollectionSizeInvariants(), "map_valueItem"));
            if (enableAccumulatedSizeCheck)
                structureConstraints.add(new AccumulatedSizeConstraint(
                        getCollectionSizeInvariants(), "map_valueItem"));
        } else if (isArray(typeName)) {
            // array_item
            structureConstraints
                    .add(new OutDegreeConstraint(getCollectionSizeInvariants(), "array_item"));
            if (enableAccumulatedSizeCheck)
                structureConstraints.add(
                        new AccumulatedSizeConstraint(getCollectionSizeInvariants(), "array_item"));
        } else {
            // object type
            labelInvs.add(new EnumConstant());
            isObjectType = true;
        }
        valueConstraints.add(new ValueConstraint(labelInvs));
        return new Vertex(typeName, itinerary, isObjectType, valueConstraints,
                structureConstraints);
    }

    public static List<UnaryInvariant> getScalaValueInvariants() {
        List<UnaryInvariant> invariants = new LinkedList<>();
        invariants.add(new NegativeOneOnce());
        invariants.add(new ZeroOnce());
        invariants.add(new OneOnce());

        Set<Number> targetValues = new HashSet<>();
        targetValues.add(-1);
        targetValues.add(0);
        targetValues.add(1);
        invariants.add(new RestOnce(targetValues));
        return invariants;
    }

    public static List<UnaryInvariant> getCollectionSizeInvariants() {
        List<UnaryInvariant> invariants = new LinkedList<>();
        invariants.add(new ZeroOnce());
        invariants.add(new OneOnce());

        Set<Number> targetValues = new HashSet<>();
        targetValues.add(0);
        targetValues.add(1);
        invariants.add(new RestOnce(targetValues));

        if (enableSequenceBoundaryCheck) {
            invariants.add(new IntegerLowerBound());
            invariants.add(new IntegerUpperBound());
        }
        return invariants;
    }

    public static boolean isCollection(String typeName) {
        if (typeName.equals("java.util.List") || typeName.equals("java.util.ArrayList")
                || typeName.equals("java.util.LinkedList") || typeName.equals("java.util.Vector")
                || typeName.equals("java.util.Stack") || typeName.equals("java.util.Queue")
                || typeName.equals("java.util.PriorityQueue")) {
            return true;
        } else if (typeName.equals("java.util.Set") || typeName.equals("java.util.HashSet")
                || typeName.equals("java.util.TreeSet")
                || typeName.equals("java.util.LinkedHashSet")
                || typeName.equals("java.util.EnumSet")
                || typeName.equals("java.util.concurrent.CopyOnWriteArraySet")) {
            return true;
        } else if (typeName.equals("java.util.SortedSet")
                || typeName.equals("java.util.NavigableSet")
                || typeName.equals("java.util.concurrent.ConcurrentSkipListSet")) {
            return true;
        }
        return false;
    }

    public static boolean isMap(String typeName) {
        return typeName.equals("java.util.Map") || typeName.equals("java.util.HashMap")
                || typeName.equals("java.util.TreeMap") || typeName.equals("java.util.Hashtable")
                || typeName.equals("java.util.LinkedHashMap")
                || typeName.equals("java.util.WeakHashMap")
                || typeName.equals("java.util.IdentityHashMap")
                || typeName.equals("java.util.EnumMap")
                || typeName.equals("java.util.ConcurrentHashMap")
                || typeName.equals("java.util.ConcurrentSkipListMap");
    }

    public static boolean isArray(String typeName) {
        return (typeName.contains("[]"));
    }

    public static boolean isScala(String typeName) {
        // char and byte are not included
        return typeName.equals("int") || typeName.equals("java.lang.Integer")
                || typeName.equals("long") || typeName.equals("java.lang.Long")
                || typeName.equals("double") || typeName.equals("java.lang.Double")
                || typeName.equals("float") || typeName.equals("java.lang.Float")
                || typeName.equals("short") || typeName.equals("java.lang.Short");
    }

    public static Map<String, GraphPattern> createGraphPatterns(
            Map<String, Map<String, String>> classInfoOri) {
        assert classInfoOri != null;
        Map<String, GraphPattern> graphPatterns = new HashMap<>();
        for (String className : classInfoOri.keySet()) {
            GraphPattern graphPattern = new GraphPattern(className);
            for (String fieldName : classInfoOri.get(className).keySet()) {
                String fieldType = classInfoOri.get(className).get(fieldName);
                // Create the vertex and edge
                Edge edge = new Edge(fieldName);

                String itinerary = className + "." + fieldName;

                Vertex vertex = createVertex(fieldType, itinerary);
                graphPattern.graph.addVertex(vertex);
                graphPattern.graph.addEdge(graphPattern.root, vertex, edge);
                if (Utils.isPrimitiveType(fieldType)) {
                    // Do nothing
                } else if (isCollection(fieldType)) {
                    Vertex collectionItemVertex = createVertex("ObjectPlaceHolder",
                            itinerary + ".collection_item");
                    graphPattern.graph.addVertex(collectionItemVertex);
                    graphPattern.graph.addEdge(vertex, collectionItemVertex,
                            new Edge("collection_item"));
                } else if (isMap(fieldType)) {
                    Vertex mapKeyItemVertex = createVertex("ObjectPlaceHolder",
                            itinerary + ".map_keyItem");
                    graphPattern.graph.addVertex(mapKeyItemVertex);
                    graphPattern.graph.addEdge(vertex, mapKeyItemVertex, new Edge("map_keyItem"));

                    Vertex mapValueItemVertex = createVertex("ObjectPlaceHolder",
                            itinerary + ".map_valueItem");
                    graphPattern.graph.addVertex(mapValueItemVertex);
                    graphPattern.graph.addEdge(vertex, mapValueItemVertex,
                            new Edge("map_valueItem"));
                } else if (isArray(fieldType)) {
                    Vertex arrayItemVertex = createVertex("ObjectPlaceHolder",
                            itinerary + ".array_item");
                    graphPattern.graph.addVertex(arrayItemVertex);
                    graphPattern.graph.addEdge(vertex, arrayItemVertex, new Edge("array_item"));
                }
            }
            graphPatterns.put(className, graphPattern);
        }
        return graphPatterns;
    }

    public static void printGraph(Vertex startVertex, Graph<Vertex, Edge> graph) {
        printGraphDFS(startVertex, graph, new HashSet<>(), 0);
    }

    private static void printGraphDFS(Vertex currentVertex, Graph<Vertex, Edge> graph,
            Set<Vertex> visited, int depth) {
        // if (visited.contains(currentVertex)) {
        // return;
        // }
        printWithIndent(currentVertex, depth);

        visited.add(currentVertex);

        // avoid print multi-edge between 2 nodes
        Set<String> visitedEdgeNames = new HashSet<>();
        Set<Vertex> visitedVertices = new HashSet<>();
        // If name is the same and the target vertex is the same, skip it
        for (Edge edge : graph.outgoingEdgesOf(currentVertex)) {
            Vertex target = graph.getEdgeTarget(edge);
            if (visitedEdgeNames.contains(edge.name) && visitedVertices.contains(target)) {
                continue;
            }
            printWithIndent(edge, depth + 1); // Print the edge with an indent
            printGraphDFS(target, graph, visited, depth + 2); // Increase the indent for the child
            // node
            visitedEdgeNames.add(edge.name);
            visitedVertices.add(target);
        }
    }

    public static void reversePrintGraph(Vertex startVertex, Graph<Vertex, Edge> graph) {
        reversePrintGraphDFS(startVertex, graph, new HashSet<>(), 0);
    }

    private static void reversePrintGraphDFS(Vertex currentVertex, Graph<Vertex, Edge> graph,
            Set<Vertex> visited, int depth) {
        // if (visited.contains(currentVertex)) {
        // return;
        // }
        printWithIndent(currentVertex, depth);

        visited.add(currentVertex);

        for (Edge edge : graph.incomingEdgesOf(currentVertex)) {
            Vertex target = graph.getEdgeSource(edge);
            printWithIndent(edge, depth + 1); // Print the edge with an indent
            reversePrintGraphDFS(target, graph, visited, depth + 2); // Increase the indent for the
            // child node
        }
    }

    private static void printWithIndent(Object obj, int indentLevel) {
        for (int i = 0; i < indentLevel; i++) {
            System.out.print("  "); // Two spaces for each level of indentation
        }
        System.out.println(obj);
    }

}
