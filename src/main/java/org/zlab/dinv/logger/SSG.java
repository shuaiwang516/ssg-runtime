package org.zlab.dinv.logger;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SSG implements Serializable {
    // process the List<LogEntry> to generate the SSG
    private static final long serialVersionUID = -3502268071773914071L;

    // Root Nodes
    public Map<LogEntry.VariableInfo, Node> rootNodeMap = new HashMap<>();
    public Map<LogEntry.VariableInfo, Node> nodeMap = new HashMap<>();

    public SSG(List<LogEntry> logEntries) {
        // construct the SSG, each logEntry is an edge consisting of parent and field
        for (LogEntry logEntry : logEntries) {
            LogEntry.VariableInfo parent = logEntry.parent;
            LogEntry.VariableInfo field = logEntry.field;

            if (parent == null || field == null) {
                // System.out.println("parent or field is null");
                // System.out.println("p = " + parent);
                // System.out.println("c = " + field);
                continue;
            }

            // construct the variableInfoNodeMap
            if (!nodeMap.containsKey(parent)) {
                nodeMap.put(parent, new Node(parent));
            } else {
                if (parent.name != null && nodeMap.get(parent).variableInfo.name == null)
                    nodeMap.get(parent).variableInfo.name = parent.name;
            }
            if (!nodeMap.containsKey(field)) {
                nodeMap.put(field, new Node(field));
            } else {
                if (field.name != null && nodeMap.get(field).variableInfo.name == null)
                    nodeMap.get(field).variableInfo.name = field.name;
            }
            nodeMap.get(parent).addChild(nodeMap.get(field));
            nodeMap.get(field).addParent(nodeMap.get(parent));

            // Update the rootNodeMap
            if (nodeMap.get(parent).parents.isEmpty() && !rootNodeMap.containsKey(parent)) {
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
    // deserialize the SSG
    public static SSG deserializeSSG(Path filePath) {
        SSG ssg = null;
        try (FileInputStream fileIn = new FileInputStream(filePath.toFile());
                ObjectInputStream in = new ObjectInputStream(fileIn)) {
            ssg = (SSG) in.readObject();
        } catch (IOException i) {
            i.printStackTrace();
        } catch (ClassNotFoundException c) {
            System.out.println("Graph class not found");
            c.printStackTrace();
        }
        return ssg;
    }

    public static void createSSG(Path filePath) {
        List<LogEntry> logEntries = LogReader.read(filePath);
        SSG ssg = new SSG(logEntries);
        SSG.serializeSSG(ssg, "ssg.ser");
    }

    // Test usage
    public void traverse() {
        // Find how many child a RowIndexEntry has
        for (Node node : nodeMap.values()) {
            if (node.variableInfo.className.contains("IndexEntry")) {
                System.out.println("start node = " + node.variableInfo);
                System.out.println(node.children.size());
                Node.printAllChildren(node);
                System.out.println("====================================");
            }
        }
    }

    // Test usage
    public void traverse1() {
        // Find how many child a RowIndexEntry has
        for (Node node : nodeMap.values()) {
            if (node.variableInfo.identifyHash == 519944329) {
                System.out.println("start node = " + node.variableInfo);
                System.out.println(node.children.size());
                Node.printAllChildren(node);
                System.out.println("====================================");
            }
        }
    }

    // Test usage
    public static void main(String[] args) {
        // Reconstruct the SSG from the log file

        // Path filePath = Paths.get("serialize.log");
        Path filePath = Paths
                .get("/Users/hanke/Desktop/Project/cassandra/cassandra1/logs/serialize.log");
        SSG.createSSG(filePath);

        SSG ssg = SSG.deserializeSSG(Paths.get("ssg.ser"));
        System.out.println("traverse the SSG =====================");
        ssg.traverse();
        // ssg.traverse1();
    }
}
