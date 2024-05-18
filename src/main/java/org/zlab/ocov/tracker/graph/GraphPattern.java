package org.zlab.ocov.tracker.graph;

import org.apache.commons.lang3.SerializationUtils;
import org.jgrapht.Graph;
import org.jgrapht.graph.DirectedMultigraph;
import org.zlab.ocov.Utils;
import org.zlab.ocov.tracker.EqualitySet;
import org.zlab.ocov.tracker.FormatCoverageStatus;
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
    private static final boolean useFixedSampleSize = true;
    private static final int maxArrayLength = 20;
    private static final int arraySampleSize = 20;
    private static final double arraySampleRate = 0.01;

    public static class Vertex implements Serializable {
        private static final long serialVersionUID = 20231215L;

        boolean isObjectType;

        final String type;
        String itinerary;
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

        public static void updateItinerary(GraphPattern graphPattern, String itineraryPrefix) {
            for (Vertex v : graphPattern.graph.vertexSet()) {
                v.itinerary = itineraryPrefix + "->" + v.itinerary;
            }
        }

        public void reset() {
            for (LabelConstraint labelConstraint : labelConstraints) {
                labelConstraint.reset();
            }
            for (StructureConstraint structureConstraint : structureConstraints) {
                structureConstraint.reset();
            }
        }

        public boolean update(Object obj, GraphPattern graphPattern,
                Map<String, GraphPattern> graphPatternMap, LogInfo logInfo, EqualitySet equalitySet,
                IsSerialize isSerialized, Set<String> brokenInvs, int objId, Set<Integer> visited) {
            // if (Runtime.debug)
            // Runtime.log("[debug] update vertex: dumpId = " + logInfo.dumpId + ", iti = "
            // + itinerary
            // + ", current time = " + System.currentTimeMillis() + ", objId = " + objId
            // + ", obj class = " + (obj == null ? "null" : obj.getClass().getName()));
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

            if (obj == null)
                return labelConstraintsChange || structureConstraintsChange;

            String objectType = obj.getClass().getName();
            if (visited.contains(System.identityHashCode(obj))) {
                // Though we do not further track, we still need to process this object
                // since the this could lead to a different iti for equality set
                computeSpecialInvariant(equalitySet, isSerialized, obj, obj.getClass().getName(),
                        itinerary, objId, visited, logInfo);
                return labelConstraintsChange || structureConstraintsChange;
            }

            if (isObjectType) {
                // Marker vetrex for polymorphism
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
                    // Skip it if it's recursive type (linked list...)
                    if (graphPatternMap.containsKey(objectType)
                            && !itinerary.contains(objectType)) {
                        // Include the subgraph's edges and vertices
                        GraphPattern subGraphPattern = SerializationUtils
                                .clone(graphPatternMap.get(objectType));
                        updateItinerary(subGraphPattern, itinerary);
                        for (Vertex v1 : subGraphPattern.graph.vertexSet())
                            graphPattern.graph.addVertex(v1);
                        for (Edge edge : subGraphPattern.graph.edgeSet())
                            graphPattern.graph.addEdge(subGraphPattern.graph.getEdgeSource(edge),
                                    subGraphPattern.graph.getEdgeTarget(edge), edge);

                        // Connect two graphs
                        GraphPattern.Edge newEdge = new GraphPattern.Edge(objectType);
                        graphPattern.graph.addEdge(this, subGraphPattern.root, newEdge);
                        subGraphPattern.root.update(obj, graphPattern, graphPatternMap, logInfo,
                                equalitySet, isSerialized, brokenInvs, objId, visited);
                        subGraphPatternChange = true;
                    } else {
                        // We won't further track, but still need to process this object
                        // since it's recorded in serialized objects
                        computeSpecialInvariant(equalitySet, isSerialized, obj, objectType,
                                itinerary, objId, visited, logInfo);
                    }
                }
            } else {
                computeSpecialInvariant(equalitySet, isSerialized, obj, objectType, itinerary,
                        objId, visited, logInfo);

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
                            int sampleSize = useFixedSampleSize
                                    ? arraySampleSize
                                    : (int) (length * arraySampleRate);
                            sampleIdxs = Utils.sampleIdxFromSize(length, sampleSize);
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
                            int sampleSize = useFixedSampleSize
                                    ? arraySampleSize
                                    : (int) (length * arraySampleRate);
                            sampleIdxs = Utils.sampleIdxFromSize(length, sampleSize);
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
                    GraphPattern.Vertex collectionFirstItemVertex = null;
                    GraphPattern.Vertex collectionLastItemVertex = null;
                    Set<GraphPattern.Edge> outgoingEdges = graphPattern.graph.outgoingEdgesOf(this);
                    for (GraphPattern.Edge patternEdge : outgoingEdges) {
                        if (patternEdge.name.equals("collection_item")) {
                            collectionItemVertex = graphPattern.graph.getEdgeTarget(patternEdge);
                        }
                        if (patternEdge.name.equals("collection_firstItem")) {
                            collectionFirstItemVertex = graphPattern.graph
                                    .getEdgeTarget(patternEdge);
                        }
                        if (patternEdge.name.equals("collection_lastItem")) {
                            collectionLastItemVertex = graphPattern.graph
                                    .getEdgeTarget(patternEdge);
                        }
                        if (collectionItemVertex != null && collectionFirstItemVertex != null
                                && collectionLastItemVertex != null) {
                            break;
                        }
                    }
                    boolean firstItemUpdated = false;
                    boolean lastItemUpdated = false;
                    if (collectionFirstItemVertex != null) {
                        Object firstItem = getFirstItemFromCollectionWithOrder(obj);
                        if (collectionFirstItemVertex.update(firstItem, graphPattern,
                                graphPatternMap, logInfo, equalitySet, isSerialized, brokenInvs,
                                objId, visited))
                            subGraphPatternChange = true;
                        firstItemUpdated = true;
                    }
                    if (collectionLastItemVertex != null) {
                        Object lastItem = getLastItemFromCollectionWithOrder(obj);
                        if (collectionLastItemVertex.update(lastItem, graphPattern, graphPatternMap,
                                logInfo, equalitySet, isSerialized, brokenInvs, objId, visited))
                            subGraphPatternChange = true;
                        lastItemUpdated = true;
                    }
                    if (collectionItemVertex != null) {
                        int length = ((Collection) obj).size();
                        List<Integer> sampleIdxs;
                        if (length > maxArrayLength) {
                            int sampleSize = useFixedSampleSize
                                    ? arraySampleSize
                                    : (int) (length * arraySampleRate);
                            sampleIdxs = Utils.sampleIdxFromSize(length, sampleSize);
                        } else {
                            sampleIdxs = new ArrayList<>();
                            for (int i = 0; i < length; i++)
                                sampleIdxs.add(i);
                        }
                        Object[] array = ((Collection) obj).toArray();
                        for (int i : sampleIdxs) {
                            if ((firstItemUpdated && i == 0)
                                    || (lastItemUpdated && i == length - 1)) {
                                // Skip the first and the last item, handled separately
                                continue;
                            }
                            Object object = array[i];
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
                            int sampleSize = useFixedSampleSize
                                    ? arraySampleSize
                                    : (int) (length * arraySampleRate);
                            sampleIdxs = Utils.sampleIdxFromSize(length, sampleSize);
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
                                // skip static or final fields
                                if (java.lang.reflect.Modifier.isStatic(field.getModifiers()))
                                    continue;
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
                            currentClass = currentClass.getSuperclass(); // Move to the superclass
                        }
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            return labelConstraintsChange || structureConstraintsChange || subGraphPatternChange;
        }

        public FormatCoverageStatus merge(Vertex otherVertex, GraphPattern otherGraphPattern,
                GraphPattern graphPattern) {
            FormatCoverageStatus formatCoverageStatus = new FormatCoverageStatus();
            // merge label constraints
            assert labelConstraints.size() == otherVertex.labelConstraints.size();
            for (int i = 0; i < labelConstraints.size(); i++) {
                formatCoverageStatus.incorporate(labelConstraints.get(i)
                        .merge(otherVertex.labelConstraints.get(i), itinerary));
            }
            // merge structure constraints
            assert structureConstraints.size() == otherVertex.structureConstraints.size();
            for (int i = 0; i < structureConstraints.size(); i++) {
                formatCoverageStatus.incorporate(structureConstraints.get(i)
                        .merge(otherVertex.structureConstraints.get(i), itinerary));
            }
            for (GraphPattern.Edge edge : otherGraphPattern.graph.outgoingEdgesOf(otherVertex)) {
                // check whether the edge is in the graphPattern
                boolean found = false;
                for (GraphPattern.Edge patternEdge : graphPattern.graph.outgoingEdgesOf(this)) {
                    if (patternEdge.name.equals(edge.name)) {
                        GraphPattern.Vertex target = graphPattern.graph.getEdgeTarget(patternEdge);
                        formatCoverageStatus.incorporate(
                                target.merge(otherGraphPattern.graph.getEdgeTarget(edge),
                                        otherGraphPattern, graphPattern));
                        // there should only be one edge with the same name
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    GraphPattern.Vertex newVertex = SerializationUtils
                            .clone(otherGraphPattern.graph.getEdgeTarget(edge));
                    newVertex.reset();
                    graphPattern.graph.addVertex(newVertex);
                    graphPattern.graph.addEdge(this, newVertex, edge);
                    formatCoverageStatus.incorporate(
                            newVertex.merge(otherGraphPattern.graph.getEdgeTarget(edge),
                                    otherGraphPattern, graphPattern));
                }
            }
            return formatCoverageStatus;
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

    public boolean update(Object obj, Map<String, GraphPattern> graphPatternMap, LogInfo logInfo,
            EqualitySet equalitySet, IsSerialize isSerialized, Set<String> brokenInvs, int objId) {
        return root.update(obj, this, graphPatternMap, logInfo, equalitySet, isSerialized,
                brokenInvs, objId, new HashSet<>());
    }

    public FormatCoverageStatus merge(GraphPattern other) {
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
            // TODO: add preserved string set...
            labelInvs.add(new PreservedStringOnce(Runtime.preservedStrings));
        } else if (isCollection(typeName)) {
            structureConstraints.add(new OutDegreeConstraint(getCollectionSizeFormatInvariants(),
                    getCollectionSizeBoundaryInvariants(), "collection_item"));
            if (enableAccumulatedSizeCheck)
                structureConstraints
                        .add(new AccumulatedSizeConstraint(getCollectionSizeFormatInvariants(),
                                getCollectionSizeBoundaryInvariants(), "collection_item"));
        } else if (isMap(typeName)) {
            // keys
            structureConstraints.add(new OutDegreeConstraint(getCollectionSizeFormatInvariants(),
                    getCollectionSizeBoundaryInvariants(), "map_keyItem"));
            if (enableAccumulatedSizeCheck)
                structureConstraints
                        .add(new AccumulatedSizeConstraint(getCollectionSizeFormatInvariants(),
                                getCollectionSizeBoundaryInvariants(), "map_keyItem"));
            // values
            structureConstraints.add(new OutDegreeConstraint(getCollectionSizeFormatInvariants(),
                    getCollectionSizeBoundaryInvariants(), "map_valueItem"));
            if (enableAccumulatedSizeCheck)
                structureConstraints
                        .add(new AccumulatedSizeConstraint(getCollectionSizeFormatInvariants(),
                                getCollectionSizeBoundaryInvariants(), "map_valueItem"));
        } else if (isArray(typeName)) {
            // array_item
            structureConstraints.add(new OutDegreeConstraint(getCollectionSizeFormatInvariants(),
                    getCollectionSizeBoundaryInvariants(), "array_item"));
            if (enableAccumulatedSizeCheck)
                structureConstraints
                        .add(new AccumulatedSizeConstraint(getCollectionSizeFormatInvariants(),
                                getCollectionSizeBoundaryInvariants(), "array_item"));
        } else {
            // object type
            labelInvs.add(new EnumConstant());
            isObjectType = true;
        }
        valueConstraints.add(new ValueConstraint(labelInvs, new LinkedList<>()));
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

    public static List<UnaryInvariant> getCollectionSizeFormatInvariants() {
        List<UnaryInvariant> invariants = new LinkedList<>();
        invariants.add(new ZeroOnce());
        invariants.add(new OneOnce());

        Set<Number> targetValues = new HashSet<>();
        targetValues.add(0);
        targetValues.add(1);
        invariants.add(new RestOnce(targetValues));

        return invariants;
    }

    public static List<UnaryInvariant> getCollectionSizeBoundaryInvariants() {
        List<UnaryInvariant> invariants = new LinkedList<>();
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

    public static boolean isCollectionWithOrder(String typeName) {
        // also make sure set is included
        return typeName.equals("java.util.List") || typeName.equals("java.util.ArrayList")
                || typeName.equals("java.util.LinkedList") || typeName.equals("java.util.Vector")
                || typeName.equals("java.util.Stack") || typeName.equals("java.util.Queue")
                || typeName.equals("java.util.PriorityQueue") || typeName.equals("java.util.Set")
                || typeName.equals("java.util.SortedSet")
                || typeName.equals("java.util.NavigableSet");
    }

    public static Object getFirstItemFromCollectionWithOrder(Object obj) {
        if (obj instanceof List) {
            List list = (List) obj;
            if (list.size() > 0) {
                return list.get(0);
            }
        } else if (obj instanceof Queue) {
            Queue queue = (Queue) obj;
            if (queue.size() > 0) {
                return queue.peek();
            }
        } else if (obj instanceof SortedSet) {
            SortedSet sortedSet = (SortedSet) obj;
            if (sortedSet.size() > 0) {
                return sortedSet.first();
            }
        }
        return null;
    }

    public static Object getLastItemFromCollectionWithOrder(Object obj) {
        if (obj instanceof List) {
            List list = (List) obj;
            if (list.size() > 0) {
                return list.get(list.size() - 1);
            }
        } else if (obj instanceof Queue) {
            Queue queue = (Queue) obj;
            if (queue.size() > 0) {
                return queue.peek();
            }
        } else if (obj instanceof SortedSet) {
            SortedSet sortedSet = (SortedSet) obj;
            if (sortedSet.size() > 0) {
                return sortedSet.last();
            }
        }
        return null;
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

                    // Special handle the first/last item if there's order
                    if (isCollectionWithOrder(fieldType)) {
                        Vertex firstItemVertex = createVertex("ObjectPlaceHolder",
                                itinerary + ".collection_firstItem");
                        graphPattern.graph.addVertex(firstItemVertex);
                        graphPattern.graph.addEdge(vertex, firstItemVertex,
                                new Edge("collection_firstItem"));

                        Vertex lastItemVertex = createVertex("ObjectPlaceHolder",
                                itinerary + ".collection_lastItem");
                        graphPattern.graph.addVertex(lastItemVertex);
                        graphPattern.graph.addEdge(vertex, lastItemVertex,
                                new Edge("collection_lastItem"));
                    }
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

    private static void computeSpecialInvariant(EqualitySet equalitySet, IsSerialize isSerialized,
            Object obj, String objectType, String itinerary, int objId, Set<Integer> visited,
            LogInfo logInfo) {
        // The current object vertex won't be iterated again, process it
        if (equalitySet != null) {
            equalitySet.update(obj, objectType, itinerary, objId);
        }
        if (isSerialized != null) {
            if (obj.getClass().isEnum())
                isSerialized.updateVisitedEnums(objectType, obj.toString());
        }
        visited.add(System.identityHashCode(obj));
    }

}
