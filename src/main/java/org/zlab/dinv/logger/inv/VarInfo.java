package org.zlab.dinv.logger.inv;

import org.zlab.dinv.logger.LogEntry;
import org.zlab.dinv.logger.Node;
import org.zlab.dinv.logger.derive.Derivation;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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
        return this.name.hashCode() + this.type.hashCode() + this.className.hashCode()
                + this.rootClassName.hashCode();
    }

    public static List<VarInfo> fromNode(Node node) {
        List<VarInfo> vars = new LinkedList<>();

        String name = node.variableInfo.name.split("\\.")[1];

        LogEntry.VariableType type = node.variableInfo.type;
        String className = node.variableInfo.className;

        Set<String> visitedParents = new HashSet<>();
        for (Node parent : node.parents) {
            if (visitedParents.contains(parent.variableInfo.className)) {
                continue;
            }
            vars.add(new VarInfo(name, type, className, parent.variableInfo.className,
                    parent.variableInfo.className));
            visitedParents.add(parent.variableInfo.className);
        }
        return vars;
    }

}
