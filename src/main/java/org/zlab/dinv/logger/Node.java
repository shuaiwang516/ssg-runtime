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
    public Map<Integer, String> childrenIdHash2Name = new HashMap<>();

    public Node(LogEntry.VariableInfo variableInfo) {
        this.type = variableInfo.type;
        this.className = variableInfo.className;
        if (variableInfo.identifyHash == 0 && variableInfo.className != null) {
            // Static class
            identifyHash = variableInfo.className.hashCode();
        } else {
            identifyHash = variableInfo.identifyHash;
        }
        this.value = variableInfo.value;
        if (className == null && identifyHash == 0) {
            System.out.println("className and identifyHash are both null" + variableInfo);
        }
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

    public void addChild(Node node, String name) {
        if (node == null) {
            return;
        }
        for (Node child : children) {
            if (child.identifyHash == node.identifyHash) {
                return;
            }
        }
        children.add(node);
        childrenIdHash2Name.put(node.identifyHash, name);
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

    @Override
    public String toString() {
        return "Node{" + "type=" + type + ", className='" + className + '\'' + ", identifyHash="
                + identifyHash + ", value='" + value + '\'' + '}';
    }

}
