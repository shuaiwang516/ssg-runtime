package org.zlab.dinv.logger.inv.unary;

import org.zlab.dinv.logger.Node;
import org.zlab.dinv.logger.SSG;
import org.zlab.dinv.logger.inv.Invariant;

import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

public abstract class UnaryInvariant extends Invariant {

    static final long serialVersionUID = 20020122L;

    public abstract void add(Object val, int count);
    public abstract boolean check(Object val, int count);

    public static void findCollectionOrArray(SSG ssg) {
        System.out.println("Root Nodes size: " + ssg.rootNodeMap.size());

        List<Node> collectionOrArrayNodes = new LinkedList<>();

        for (Node node : ssg.rootNodeMap.values()) {
            List<Node> targetNodes = Node.findNodeSatisfyClassname(node);

            for (Node n : targetNodes) {
                // print parent, current node and children
                if (n.children.isEmpty() || n.parents.isEmpty())
                    continue;
                collectionOrArrayNodes.add(n);
                // for (Node p : n.parents) {
                // System.out.println("Parent: " + p.variableInfo);
                // }
                // System.out.println("Current Node: " + n.variableInfo);
                // for (Node c : n.children) {
                // System.out.println("Children: " + c.variableInfo);
                // }
                // System.out.println();
            }
        }

        // we need to know which field it belongs to
        List<String> fieldRelations = new LinkedList<>();
        for (Node node : collectionOrArrayNodes) {
            // System.out.println("Node: " + node.variableInfo);
            for (Node parent : node.parents) {
                fieldRelations.add(parent.variableInfo.className + " : " + node.variableInfo.name
                        + " : " + "children size = " + node.children.size());
            }
        }

        for (String s : fieldRelations) {
            System.out.println(s);
        }

        // Infer invariants：
        // Separate with different fields (ClassName.fieldName)

        // For each field with collection type, we maintain a list of invariants
        // Generated Example Invariants
        /**
         * Map<String, Invariant>
         *
         *
         * For field with collection/array type, the number of item being serialized
         * serializeItemSize(a) <= x, a is the field, x is the number.
         */
    }

    // Test Usage
    public static void main(String[] args) {
        SSG ssg = SSG.deserializeSSG(Paths.get("ssg.ser"));
        // Traverse ssg from root nodes, identify collections/Array, find its parent,
        // check the number of children

        // Identify all collections/array
        findCollectionOrArray(ssg);
    }

}
