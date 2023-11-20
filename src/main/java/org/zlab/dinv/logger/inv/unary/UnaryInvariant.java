package org.zlab.dinv.logger.inv.unary;

import org.zlab.dinv.logger.Node;
import org.zlab.dinv.logger.SSG;
import org.zlab.dinv.logger.inv.Invariant;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class UnaryInvariant extends Invariant {

    static final long serialVersionUID = 20020122L;

    public Map<String, List<UnaryTemplate>> unaryInvariants = new HashMap<>();

    public void initTemplates(String key) {
        // init a list of templates and add it to the map
    }

    @Override
    public void process(Node node) {
        // Only support primitive type/collection/array

        // field must contains a dot
        if (!node.variableInfo.name.contains("."))
            return;

        // split dot, the [1] will be field name
        String fieldInfo = node.variableInfo.name.split("\\.")[1];

        // There might be multiple parents
        for (Node parent : node.parents) {
            // Get class name
            String className = parent.variableInfo.className;
            String key = className + "." + fieldInfo;
            if (!unaryInvariants.containsKey(key)) {
                unaryInvariants.put(key, new LinkedList<>());
                // add a list of templates
                initTemplates(key);
            }

            // Update the list of templates
            if (Node.isCollectionOrArray(node)) {
                // Collection Size

            } else {
                // Primitive type
            }
        }

        // Two cases: (1) Node is a collection (2) Node is a primitive type

        /**
         * If it's a new field, create one and init a list of templates
         */

        /**
         * If it's an existing field, update the list of templates, invariants might be
         * updated or removed.
         */
    }

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
        Map<String, UnaryTemplate> fieldInvariants = new HashMap<>();
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
