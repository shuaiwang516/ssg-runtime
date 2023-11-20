package org.zlab.dinv.logger;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class Node implements Serializable {
    private static final long serialVersionUID = -7603362767491437099L;

    public LogEntry.VariableInfo variableInfo;

    public List<Node> parents = new LinkedList<>();
    public List<Node> children = new LinkedList<>();

    public Node() {
    }

    public Node(LogEntry.VariableInfo variableInfo) {
        this.variableInfo = variableInfo;
    }

    public void addParent(Node node) {
        if (node == null) {
            return;
        }
        for (Node parent : parents) {
            if (parent.variableInfo.equals(node.variableInfo)) {
                return;
            }
        }
        parents.add(node);
    }

    public void addChild(Node node) {
        if (node == null) {
            return;
        }
        for (Node child : children) {
            if (child.variableInfo == null) {
                System.out.println("child is null");
            }
            if (child.variableInfo.equals(node.variableInfo)) {
                return;
            }
        }
        children.add(node);
    }

    public static void printAllChildren(Node node, int indentLevel) {
        if (node == null) {
            return;
        }

        // Process the current node (e.g., print its information)
        printWithIndent(node.variableInfo, indentLevel);

        // Recursively call this method for each child
        for (Node child : node.children) {
            printAllChildren(child, indentLevel + 1);
        }
    }

    private static void printWithIndent(LogEntry.VariableInfo info, int indentLevel) {
        for (int i = 0; i < indentLevel; i++) {
            System.out.print("  "); // Two spaces for each level of indentation
        }
        System.out.println(info); // Assuming VariableInfo has a meaningful toString()
                                  // implementation
    }

    // Overload the method to start without indentation
    public static void printAllChildren(Node node) {
        printAllChildren(node, 0);
    }

    public static List<Node> findNodeSatisfyClassname(Node startNode) {
        List<Node> result = new LinkedList<>();
        if (startNode == null) {
            return result;
        }
        if (startNode.variableInfo.className.toLowerCase().contains("list")
                || startNode.variableInfo.className.toLowerCase().contains("array")) {
            result.add(startNode);
        }
        for (Node child : startNode.children) {
            result.addAll(findNodeSatisfyClassname(child));
        }
        return result;
    }

}
