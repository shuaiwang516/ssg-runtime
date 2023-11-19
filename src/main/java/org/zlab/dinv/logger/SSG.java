package org.zlab.dinv.logger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SSG implements Serializable {
    // process the List<LogEntry> to generate the SSG

    // Root Nodes
    public Map<LogEntry.VariableInfo, Node> rootNodeMap = new HashMap<>();
    public Map<LogEntry.VariableInfo, Node> nodeMap = new HashMap<>();

    public SSG(List<LogEntry> logEntries) {
        // construct the SSG, each logEntry is an edge consisting of parent and field
        for (LogEntry logEntry : logEntries) {
            LogEntry.VariableInfo parent = logEntry.parent;
            LogEntry.VariableInfo field = logEntry.field;

            if (parent == null || field == null) {
                System.out.println("parent or field is null");
                System.out.println("p = " + parent);
                System.out.println("c = " + field);
                continue;
            }

            // construct the variableInfoNodeMap
            if (!nodeMap.containsKey(parent)) {
                nodeMap.put(parent, new Node(parent));
            }
            if (!nodeMap.containsKey(field)) {
                nodeMap.put(field, new Node(field));
            }
            nodeMap.get(parent).addChild(nodeMap.get(field));
            nodeMap.get(field).addParent(nodeMap.get(parent));

            // Update the rootNodeMap
            if (!nodeMap.get(parent).parents.isEmpty()) {
                rootNodeMap.put(parent, nodeMap.get(parent));
            }
            if (rootNodeMap.containsKey(field)) {
                rootNodeMap.remove(field);
            }
        }
    }

    public static void serializeSSG(SSG graph, String filename) {
        try (FileOutputStream fileOut = new FileOutputStream(filename);
                ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
            out.writeObject(graph);
            System.out.println("Serialized data is saved in " + filename);
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Reconstruct the SSG from the log file

        // Path filePath = Paths
        // .get("/Users/hanke/Desktop/Project/cassandra/cassandra1/logs/serialize.log");
        Path filePath = Paths.get("serialize.log");

        List<LogEntry> logEntries = LogReader.read(
                Paths.get("/Users/hanke/Desktop/Project/cassandra/cassandra1/logs/serialize.log"));

        SSG ssg = new SSG(logEntries);
        // Store it to a file
        SSG.serializeSSG(ssg, "ssg.ser");
    }

}
