package org.zlab.dinv.logger.inv;

import org.jgrapht.graph.DirectedMultigraph;
import org.zlab.dinv.logger.Edge;
import org.zlab.dinv.logger.LogEntry;
import org.zlab.dinv.logger.Node;
import org.zlab.dinv.logger.derive.Derivation;
import org.zlab.dinv.logger.ssg.Vertex;

import java.io.Serializable;
import java.util.*;

public class VarInfo implements Serializable {
    static final long serialVersionUID = 20231120L;

    public String name;
    public LogEntry.VariableType type;
    public String className; // Dup with type?

    public String parentClassName; // Immediate parent
    public String rootClassName; // Root node
    public Derivation derivation;

    public VarInfo(String name, LogEntry.VariableType type, String className,
            String parentClassName, String rootClassName) {
        this.name = name;
        this.type = type;
        this.className = className;

        this.parentClassName = parentClassName;
        this.rootClassName = rootClassName;
    }

    public boolean isCollection() {
        String lowerCaseClassName = className.toLowerCase();
        return lowerCaseClassName.contains("collection") || lowerCaseClassName.contains("list")
                || lowerCaseClassName.contains("set") || lowerCaseClassName.contains("array");
    }

    @Override
    public String toString() {
        return "VarInfo{" + "name='" + name + '\'' + ", type='" + type + '\'' + ", rootClassName='"
                + rootClassName + '\'' + '}';
    }

    @Override
    public boolean equals(Object obj) {
        // add parent class name
        if (obj instanceof VarInfo) {
            VarInfo varInfo = (VarInfo) obj;
            return this.name.equals(varInfo.name) && this.type.equals(varInfo.type)
                    && this.className.equals(varInfo.className)
                    && this.rootClassName.equals(varInfo.rootClassName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, className, rootClassName);
    }

    public static List<VarInfo> fromNode(Node node) {
        List<VarInfo> vars = new LinkedList<>();

        LogEntry.VariableType type = node.type;
        String className = node.className;

        Set<String> visitedParents = new HashSet<>();
        // FIXME: if this is the root node, how to handle it?
        for (Node parent : node.parents) {
            // get current node name, might be different

            for (Edge edge : parent.childrenIdHash2Name.get(node.identifyHash)) {
                String name = edge.name;
                if (name == null) {
                    System.out.println("Null Node: " + node);
                    assert false;
                }
                if (visitedParents.contains(parent.className)) {
                    continue;
                }
                vars.add(new VarInfo(name, type, className, parent.className, parent.className));
                visitedParents.add(parent.className);
            }
        }
        return vars;
    }

    public static List<VarInfo> fromVertex(Vertex vertex,
            DirectedMultigraph<Vertex, org.zlab.dinv.logger.ssg.Edge> ssg) {
        List<VarInfo> vars = new LinkedList<>();

        LogEntry.VariableType type = vertex.type;
        String className = vertex.className;

        // FIXME: If this is the root node, how to handle it?
        for (org.zlab.dinv.logger.ssg.Edge edge : ssg.incomingEdgesOf(vertex)) {
            Vertex parent = ssg.getEdgeSource(edge);
            String name = edge.name;
            if (name == null) {
                System.out.printf("null name edge:\n parent = %s\n, node = %s\n", parent, vertex);
                assert false;
            }
            vars.add(new VarInfo(name, type, className, parent.className, parent.className));
        }
        return vars;
    }

}
