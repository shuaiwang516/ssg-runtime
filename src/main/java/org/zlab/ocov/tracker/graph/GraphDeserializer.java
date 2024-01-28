package org.zlab.ocov.tracker.graph;

import com.google.gson.*;
import org.jgrapht.graph.DirectedMultigraph;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class GraphDeserializer
        implements
            JsonDeserializer<DirectedMultigraph<GraphPattern.Vertex, GraphPattern.Edge>> {
    @Override
    public DirectedMultigraph<GraphPattern.Vertex, GraphPattern.Edge> deserialize(JsonElement json,
            Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        JsonArray jsonVertices = jsonObject.getAsJsonArray("vertices");
        JsonArray jsonEdges = jsonObject.getAsJsonArray("edges");

        DirectedMultigraph<GraphPattern.Vertex, GraphPattern.Edge> graph = new DirectedMultigraph<>(
                GraphPattern.Edge.class);
        Map<String, GraphPattern.Vertex> vertexMap = new HashMap<>();

        // Deserialize vertices
        for (JsonElement elem : jsonVertices) {
            GraphPattern.Vertex vertex = context.deserialize(elem, GraphPattern.Vertex.class);
            graph.addVertex(vertex);
            System.out.println("vertex: " + vertex.toString());
            vertexMap.put(vertex.toString(), vertex); // assuming toString uniquely identifies the
                                                      // vertex
        }

        // Deserialize edges
        for (JsonElement elem : jsonEdges) {
            JsonObject edgeObj = elem.getAsJsonObject();
            String edgeName = edgeObj.get("name").getAsString();

            // Deserialize the source and target vertices
            JsonElement sourceElem = edgeObj.get("source");
            GraphPattern.Vertex source = context.deserialize(sourceElem, GraphPattern.Vertex.class);
            JsonElement targetElem = edgeObj.get("target");
            GraphPattern.Vertex target = context.deserialize(targetElem, GraphPattern.Vertex.class);

            if (source != null && target != null) {
                GraphPattern.Edge edge = new GraphPattern.Edge(edgeName);
                graph.addEdge(source, target, edge);
            } else {
                throw new JsonParseException(
                        "Invalid edge in graph: missing source or target vertex.");
            }
        }
        return graph;
    }
}