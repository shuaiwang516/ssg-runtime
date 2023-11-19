package org.zlab.dinv.logger;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class Node implements Serializable {
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

}
