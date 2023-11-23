package org.zlab.dinv.logger.ssg;

import org.jgrapht.Graph;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.graph.concurrent.AsSynchronizedGraph;
import org.zlab.dinv.logger.*;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

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

    public static List<Vertex> getRootNodes(DirectedMultigraph<Vertex, Edge> graph) {
        List<Vertex> rootNodes = new LinkedList<>();
        for (Vertex o : graph.vertexSet()) {
            if (graph.inDegreeOf(o) == 0) {
                System.out.println("Root Node: " + o);
                rootNodes.add(o);
            }
        }
        return rootNodes;
    }

    public static void createSSG(List<LogEntry> logEntries, Path ssgStorePath) {
        DirectedMultigraph<Vertex, Edge> graph = new DirectedMultigraph<>(Edge.class);

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

            // Compute name
            String fieldName = field.name;
            if (fieldName.contains(".")) {
                fieldName = fieldName.split("\\.")[1];
            }

            graph.addEdge(pVertex, fVertex, new Edge(fieldName, i));
        }
        // serializeSSG(graph, ssgStorePath);
    }

    public static void createSSGParallel(List<LogEntry> logEntries, Path ssgStorePath) {
        Graph<Vertex, Edge> baseGraph = new DirectedMultigraph<>(Edge.class);
        Graph<Vertex, Edge> synchronizedGraph = new AsSynchronizedGraph<>(baseGraph);

        ForkJoinPool customThreadPool = new ForkJoinPool(); // Adjust the number of threads if
                                                            // necessary
        try {
            customThreadPool.submit(() -> logEntries.parallelStream().forEach(logEntry -> {
                processLogEntry(logEntries, logEntry, synchronizedGraph);
            })).get();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            customThreadPool.shutdown();
        }
        // serializeSSG(synchronizedGraph, ssgStorePath);
    }

    private static void processLogEntry(List<LogEntry> logEntries, LogEntry logEntry,
            Graph<Vertex, Edge> graph) {
        // Similar processing as in your original method
        LogEntry.VariableInfo parent = logEntry.parent;
        LogEntry.VariableInfo field = logEntry.field;
        if (parent == null || field == null) {
            return;
        }

        Vertex pVertex = new Vertex(parent);
        Vertex fVertex = new Vertex(field);

        // Synchronized addVertex
        graph.addVertex(pVertex);
        graph.addVertex(fVertex);

        // Compute name and add edge
        String fieldName = field.name;
        if (fieldName.contains(".")) {
            fieldName = fieldName.split("\\.")[1];
        }

        // Synchronized addEdge
        graph.addEdge(pVertex, fVertex, new Edge(fieldName, logEntries.indexOf(logEntry)));
    }

    public static void createSSG(Path logEntryPath, Path ssgStorePath) {
        List<LogEntry> logEntries = LogReader.read(logEntryPath);
        createSSG(logEntries, ssgStorePath);
        // createSSGParallel(logEntries, ssgStorePath);
    }

    public static void testSSG(Path ssgPath) {
        DirectedMultigraph<Vertex, Edge> graph = (DirectedMultigraph<Vertex, Edge>) deserializeSSG(
                ssgPath);
        // Find all root nodes
        // List<Vertex> rootNodes = getRootNodes(graph);

        int totalNumberOfEdges = graph.edgeSet().size();
        System.out.println("Total number of edges: " + totalNumberOfEdges);

        int totalNumberOfVertices = graph.vertexSet().size();
        System.out.println("Total number of vertices: " + totalNumberOfVertices);
    }

    // Test usage
    public static void main(String[] args) {
        Path ssgPath = Paths.get("example_ssgraph_folder/ssg_ori.ser");
        // Path logEntryPath = Paths
        // .get("/Users/hanke/Desktop/Project/cassandra/cassandra1/logs/serialize.log");
        Path logEntryPath = Paths.get("serialize.log");
        createSSG(logEntryPath, ssgPath);
        // testSSG(ssgPath);
    }

}
