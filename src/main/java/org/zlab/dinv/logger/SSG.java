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
    public Map<Integer, Node> rootNodeMap = new HashMap<>();
    public Map<Integer, Node> nodeMap = new HashMap<>();

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

            Node pNode = new Node(parent);
            Node fNode = new Node(field);

            // construct the variableInfoNodeMap
            if (!nodeMap.containsKey(pNode.identifyHash)) {
                nodeMap.put(pNode.identifyHash, pNode);
            }
            if (!nodeMap.containsKey(fNode.identifyHash)) {
                nodeMap.put(fNode.identifyHash, fNode);
            }
            pNode = nodeMap.get(pNode.identifyHash);
            fNode = nodeMap.get(fNode.identifyHash);

            // split get [1]
            String fieldName = field.name;
            if (fieldName.contains(".")) {
                fieldName = fieldName.split("\\.")[1];
            }

            pNode.addChild(fNode, fieldName);
            fNode.addParent(pNode);

            // Update the rootNodeMap
            if (pNode.parents.isEmpty() && !rootNodeMap.containsKey(pNode.identifyHash)) {
                rootNodeMap.put(pNode.identifyHash, pNode);
            }
            rootNodeMap.remove(fNode.identifyHash);
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

    public static SSG createSSG(Path filePath) {
        List<LogEntry> logEntries = LogReader.read(filePath);
        SSG ssg = new SSG(logEntries);

        for (Node node : ssg.rootNodeMap.values()) {
            if (node.identifyHash == 0 && node.className == null) {
                System.out.println("Node: " + node);
            }
        }

        return ssg;
    }

    // Test usage
    public void traverse() {
        // Find how many child a RowIndexEntry has
        for (Node node : nodeMap.values()) {
            if (node.className.contains("IndexEntry")) {
                System.out.println("start node = " + node);
                System.out.println(node.children);
                Node.printAllChildren(node);
                System.out.println("====================================");
            }
        }
    }

    // Test usage
    public void traverse1() {
        // Find how many child a RowIndexEntry has
        for (Node node : nodeMap.values()) {
            if (node.identifyHash == 519944329) {
                System.out.println("start node = " + node);
                System.out.println(node.children);
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
        SSG ssg = SSG.createSSG(filePath);
        SSG.serializeSSG(ssg, "example_ssg_folder/ssg.ser");
    }
}
