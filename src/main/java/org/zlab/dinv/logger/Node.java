package org.zlab.dinv.logger;

import java.io.Serializable;
import java.util.*;

public class Node implements Serializable {
    private static final long serialVersionUID = -7603362767491437099L;

    public LogEntry.VariableType type; // This will hold either the primitive type name or "String"
    public String className;

    public final int identifyHash;
    public String value; // hashcode for object, value for primitive

    public List<Node> parents = new LinkedList<>();
    public List<Node> children = new LinkedList<>();
    public Map<Integer, List<Edge>> childrenIdHash2Name = new HashMap<>();

    public Node(LogEntry.VariableInfo variableInfo) {
        this.type = variableInfo.type;
        this.className = variableInfo.className;
        if (variableInfo.identifyHash == 0) {
            // Static class
            if (variableInfo.className != null
                    && variableInfo.type == LogEntry.VariableType.CLASS) {
                identifyHash = variableInfo.className.hashCode();
            } else {
                // Primitives
                identifyHash = Objects.hash(variableInfo.type, variableInfo.className,
                        variableInfo.value);
            }
        } else {
            // primitive type will still be 0
            identifyHash = variableInfo.identifyHash;
        }
        this.value = variableInfo.value;
    }

    public void addParent(Node node) {
        if (node == null) {
            return;
        }
        for (Node parent : parents) {
            if (parent.equals(node)) {
                return;
            }
        }
        parents.add(node);
    }

    public void addChild(Node node, String name, int timestamp) {
        // TODO: Handle primitives, identifyHash == 0
        if (node == null) {
            return;
        }
        boolean exist = false;
        for (Node child : children) {
            if (child.identifyHash == node.identifyHash) {
                exist = true;
                break;
            }
        }
        if (!exist) {
            children.add(node);
        }
        if (!childrenIdHash2Name.containsKey(node.identifyHash)) {
            childrenIdHash2Name.put(node.identifyHash, new LinkedList<>());
        }
        childrenIdHash2Name.get(node.identifyHash).add(new Edge(name, timestamp));
    }

    public static void printAllChildren(Node node, int indentLevel) {
        if (node == null) {
            return;
        }

        // Process the current node (e.g., print its information)
        printWithIndent(node, indentLevel);

        // Recursively call this method for each child
        for (Node child : node.children) {
            printAllChildren(child, indentLevel + 1);
        }
    }

    private static void printWithIndent(Node node, int indentLevel) {
        for (int i = 0; i < indentLevel; i++) {
            System.out.print("  "); // Two spaces for each level of indentation
        }
        System.out.println(node); // Assuming VariableInfo has a meaningful toString()
                                  // implementation
    }

    // Overload the method to start without indentation
    public static void printAllChildren(Node node) {
        printAllChildren(node, 0);
    }

    public boolean isCollectionOrArray() {
        return className.toLowerCase().contains("list")
                || className.toLowerCase().contains("array");
    }

    // Method to count edges in the graph
    public static int countEdges(Node node, Set<Integer> visitedNodes) {
        if (node == null || visitedNodes.contains(node.identifyHash)) {
            return 0;
        }

        visitedNodes.add(node.identifyHash);

        // count size of node.childrenIdHash2Name
        int edgeCount = 0;
        for (List<Edge> edges : node.childrenIdHash2Name.values()) {
            edgeCount += edges.size();
        }
        for (Node child : node.children) {
            edgeCount += countEdges(child, visitedNodes); // Recursively count edges in child nodes
        }
        return edgeCount;
    }

    // Helper method to start counting from the root
    public static int countEdgesFromRoot(Node root) {
        return countEdges(root, new HashSet<>());
    }

    @Override
    public String toString() {
        return "Node{" + "type=" + type + ", className='" + className + '\'' + ", identifyHash="
                + identifyHash + ", value='" + value + '\'' + '}';
    }

}
