package org.zlab.ocov.tracker.graph;

import org.jgrapht.graph.DirectedMultigraph;

import java.io.Serializable;
import java.util.List;

public class GraphPattern implements Serializable {
    private static final long serialVersionUID = 20231215L;

    // FIXME: make it final?
    private Vertex root;
    private DirectedMultigraph<GraphPattern.Vertex, GraphPattern.Edge> graphPattern = new DirectedMultigraph<>(
            GraphPattern.Edge.class);

    /**
     * Similar to the TypeInfo in previous implementation, we could do a
     * transformation.
     */
    public static class Vertex implements Serializable {
        private static final long serialVersionUID = 20231215L;

        private String type;
        private List<LabelConstraint> labelConstraints;
        private List<StructureConstraint> structureConstraints;

        public Vertex() {
        }

        public boolean update(ObjectGraph.Vertex vertex) {
            return false;
        }
    }

    public static class Edge implements Serializable {
        private static final long serialVersionUID = 20231215L;

        public String name;

        public Edge(String name) {
            this.name = name;
        }
    }

    public GraphPattern() {
        /**
         * Construct the basic graph pattern according to the base type
         */
    }

    public boolean update(ObjectGraph objectGraph) {
        /**
         * Traverse the object graph and update the graph pattern - new node, edge could
         * be inserted.
         */
        update(objectGraph, objectGraph.getRoot(), root);
        return false;
    }

    public boolean update(ObjectGraph objectGraph, ObjectGraph.Vertex objVertex,
            Vertex patternVertex) {
        /**
         * Traverse the object graph and update the graph pattern - new node, edge could
         * be inserted.
         */
        return false;
    }

    public boolean merge(GraphPattern other) {
        // TODO
        return false;
    }

}
