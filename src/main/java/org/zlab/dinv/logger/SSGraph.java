package org.zlab.dinv.logger;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

public class SSGraph {

    public static void main(String[] args) {
        // Create a graph object
        Graph<String, DefaultEdge> graph = new SimpleGraph<>(DefaultEdge.class);

        // Add some vertices
        graph.addVertex("Vertex 1");
        graph.addVertex("Vertex 2");
        graph.addVertex("Vertex 3");

        // Add some edges
        graph.addEdge("Vertex 1", "Vertex 2");
        graph.addEdge("Vertex 2", "Vertex 3");
        graph.addEdge("Vertex 3", "Vertex 1");

        // Print the graph
        System.out.println("Graph: " + graph.toString());
    }

}
