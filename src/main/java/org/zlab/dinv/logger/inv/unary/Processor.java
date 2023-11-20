package org.zlab.dinv.logger.inv.unary;

import org.zlab.dinv.logger.Node;

import java.util.LinkedList;

public class Processor {

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
            // if (!unaryInvariants.containsKey(key)) {
            // unaryInvariants.put(key, new LinkedList<>());
            // // add a list of templates
            // initTemplates(key);
            // }

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

}
