package org.zlab.ocov.tracker.graph;

import org.jgrapht.Graph;
import org.jgrapht.graph.DirectedMultigraph;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ObjectGraph implements Serializable {
    private static final long serialVersionUID = 20231215L;

    public final Vertex root;
    public final DirectedMultigraph<Vertex, Edge> graph;

    public ObjectGraph(Vertex root) {
        this.root = root;
        graph = new DirectedMultigraph<>(Edge.class);
        graph.addVertex(root);
    }

    public Vertex getRoot() {
        return root;
    }

    public static class Edge implements Serializable {
        public static final long serialVersionUID = 20231215L;
        public final String name;

        public Edge(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return "Edge{" + "name='" + name + "'" + '}';
        }

    }

    public static class Vertex implements Serializable {
        private static final long serialVersionUID = 20231215L;

        // If it's a primitive type, we can clone the value
        // If it's a class, we keep the hashcode, since clone value could lead to high
        // memory usage
        public String type;
        public Object value;
        public int identifyHash;

        public Integer size = null;
        public Integer fakeHashCodeForComparator = null;

        public Vertex(String type, Object value, int identifyHash) {
            this(type, value, identifyHash, null);
        }

        public Vertex(String type, Object value, int identifyHash, Integer size) {
            this(type, value, identifyHash, size, null);
        }

        public Vertex(String type, Object value, int identifyHash, Integer size,
                Integer fakeHashCodeForComparator) {
            this.type = type;
            this.value = value;
            this.identifyHash = identifyHash;
            this.size = size;
            this.fakeHashCodeForComparator = fakeHashCodeForComparator;
        }

        @Override
        public String toString() {
            return "Vertex{" + "type=" + type + ", value='" + value + '\'' + ", identifyHash="
                    + identifyHash + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Vertex))
                return false;
            Vertex that = (Vertex) o;
            return identifyHash == that.identifyHash;
        }

        @Override
        public int hashCode() {
            return Objects.hash(identifyHash);
        }

    }

    public void print() {
        // get root node of objectGraph
        Set<Vertex> rootNodes = new HashSet<>();
        for (Vertex vertex : graph.vertexSet()) {
            if (graph.inDegreeOf(vertex) == 0) {
                rootNodes.add(vertex);
            }
        }

        for (Vertex rootNode : rootNodes) {
            printGraph(rootNode, graph);
        }

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
