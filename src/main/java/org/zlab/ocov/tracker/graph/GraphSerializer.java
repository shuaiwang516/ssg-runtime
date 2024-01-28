package org.zlab.ocov.tracker.graph;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.jgrapht.graph.DirectedMultigraph;
import java.lang.reflect.Type;

public class GraphSerializer
        implements
            JsonSerializer<DirectedMultigraph<GraphPattern.Vertex, GraphPattern.Edge>> {
    @Override
    public JsonElement serialize(DirectedMultigraph<GraphPattern.Vertex, GraphPattern.Edge> graph,
            Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonGraph = new JsonObject();
        JsonArray jsonVertices = new JsonArray();
        JsonArray jsonEdges = new JsonArray();

        // Serialize vertices
        for (GraphPattern.Vertex vertex : graph.vertexSet()) {
            JsonObject jsonVertex = context.serialize(vertex, GraphPattern.Vertex.class)
                    .getAsJsonObject();
            jsonVertices.add(jsonVertex);
        }

        // Serialize edges
        for (GraphPattern.Edge edge : graph.edgeSet()) {
            JsonObject jsonEdge = new JsonObject();
            jsonEdge.addProperty("name", edge.name);

            GraphPattern.Vertex sourceVertex = graph.getEdgeSource(edge);
            GraphPattern.Vertex targetVertex = graph.getEdgeTarget(edge);
            jsonEdge.add("source", context.serialize(sourceVertex, GraphPattern.Vertex.class));
            jsonEdge.add("target", context.serialize(targetVertex, GraphPattern.Vertex.class));

            jsonEdges.add(jsonEdge);
        }

        jsonGraph.add("vertices", jsonVertices);
        jsonGraph.add("edges", jsonEdges);
        return jsonGraph;
    }
}
