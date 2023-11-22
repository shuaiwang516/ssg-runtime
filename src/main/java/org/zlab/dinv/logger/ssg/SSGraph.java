package org.zlab.dinv.logger.ssg;

import org.jgrapht.graph.DirectedMultigraph;
import org.zlab.dinv.logger.*;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

public class SSGraph {

    public static void serializeSSG(DirectedMultigraph<Vertex, Edge> graph, Path filePath) {
        try (FileOutputStream fileOut = new FileOutputStream(filePath.toFile());
                ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
            out.writeObject(graph);
            System.out.println("Serialized data is saved in " + filePath.toFile());
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    // deserialize the SSG
    public static DirectedMultigraph<Vertex, Edge> deserializeSSG(Path filePath) {
        DirectedMultigraph<Vertex, Edge> ssg = null;
        try (FileInputStream fileIn = new FileInputStream(filePath.toFile());
                ObjectInputStream in = new ObjectInputStream(fileIn)) {
            ssg = (DirectedMultigraph<Vertex, Edge>) in.readObject();
        } catch (IOException i) {
            i.printStackTrace();
        } catch (ClassNotFoundException c) {
            System.out.println("Graph class not found");
            c.printStackTrace();
        }
        return ssg;
    }

    public static void createSSG() {
        // Path filePath = Paths.get("/Users/hanke/Desktop/Project/serialize.log");
        Path filePath = Paths
                .get("/Users/hanke/Desktop/Project/cassandra/cassandra1/logs/serialize.log");

        DirectedMultigraph<Vertex, Edge> graph = new DirectedMultigraph<>(Edge.class);

        List<LogEntry> logEntries = LogReader.read(filePath);
        for (int i = 0; i < logEntries.size(); i++) {
            if (i % 10000 == 0) {
                System.out.println("Processing " + i + "th log entry");
            }
            LogEntry logEntry = logEntries.get(i);
            LogEntry.VariableInfo parent = logEntry.parent;
            LogEntry.VariableInfo field = logEntry.field;
            if (parent == null || field == null) {
                // System.out.println("parent or field is null");
                // System.out.println("p = " + parent);
                // System.out.println("c = " + field);
                continue;
            }

            Vertex pVertex = new Vertex(parent);
            Vertex fVertex = new Vertex(field);

            graph.addVertex(pVertex);
            graph.addVertex(fVertex);

            graph.addEdge(pVertex, fVertex, new Edge("edge", i));

        }
        serializeSSG(graph, Paths.get("example_ssgraph_folder/ssg_ori.ser"));
    }

    public static void testSSG() {
        DirectedMultigraph<Vertex, Edge> graph = (DirectedMultigraph<Vertex, Edge>) deserializeSSG(
                Paths.get("example_ssgraph_folder/ssg.ser"));
        // Find all root nodes
        List<Vertex> rootNodes = new LinkedList<>();
        for (Vertex o : graph.vertexSet()) {
            if (graph.inDegreeOf(o) == 0) {
                System.out.println("Root Node: " + o);
                rootNodes.add(o);
            }
        }
        System.out.println("Root Nodes size: " + rootNodes.size());
    }

    public static void main(String[] args) {
        createSSG();
        // testSSG();
    }

}
