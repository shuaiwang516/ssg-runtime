package org.zlab.dinv.logger.inference;

import org.zlab.dinv.logger.LogEntry;
import org.zlab.dinv.logger.Node;
import org.zlab.dinv.logger.SSG;

import java.nio.file.Paths;
import java.util.List;

public class Unary {

    /**
     * Input: ssg Output: List<UnaryTemplate>
     */

    // Test Usage
    public static void main(String[] args) {
        SSG ssg = SSG.deserializeSSG(Paths.get("ssg.ser"));
        // Traverse ssg from root nodes, identify collections/Array, find its parent,
        // check the number of children

        // Identify all collections/array
        findCollectionOrArray(ssg);
    }

    public static void findCollectionOrArray(SSG ssg) {
        System.out.println("Root Nodes size: " + ssg.rootNodeMap.size());

        for (Node node : ssg.rootNodeMap.values()) {
            List<Node> nodes = Node.findNodeSatisfyClassname(node);

            for (Node n : nodes) {
                // print parent, current node and children
                if (n.children.isEmpty() || n.parents.isEmpty())
                    continue;
                for (Node p : n.parents) {
                    System.out.println("Parent: " + p.variableInfo);
                }
                System.out.println("Current Node: " + n.variableInfo);
                for (Node c : n.children) {
                    System.out.println("Children: " + c.variableInfo);
                }
                System.out.println();
            }

        }
    }

}
