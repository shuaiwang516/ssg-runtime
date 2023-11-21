package org.zlab.dinv.logger.inv;

import org.zlab.dinv.logger.Node;
import org.zlab.dinv.logger.SSG;
import org.zlab.dinv.logger.SSGPointSlice;
import org.zlab.dinv.logger.inv.unary.UpperBound;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class InferenceEngine {
    // List of prototype invariants
    public static ArrayList<Invariant> proto_invs = new ArrayList<>();
    static {
        // TODO: add all proto invariants to proto_invs
        proto_invs.add(new UpperBound());
    }

    public List<SSGPointSlice> unaryInvariants = new LinkedList<>();
    public Map<VarInfo, SSGPointSlice> unaryInvariantMap = new HashMap<>();

    /**
     * Input: a folder, which contains a list of ssg.ser file Output: a list of
     * invariants 1. Read all ssg.ser files into memory (SSG) 2. Process each ssg
     * Iterate the SSG if some SSG point doesn't exist create: a new SSG point slice
     * Init unary invariants Init binary invariants else: validate/improve the
     * likely invariants of the SSG point slice Validate unary invariants Validate
     * binary invariants 3. After processing all ssgs, output the generated
     * invariants to a file (Later we need an invariant monitor for it) (1) Embed
     * invariants and use monitor (2) Keep instrumenting the runtime system and
     * monitor the invariants
     */
    public void run() {
        // Read and process each ssg: folder: example_ssg_folder
        Path ssgFolder = Paths.get("example_ssg_folder");
        assert ssgFolder.toFile().isDirectory();
        for (File ssgFile : ssgFolder.toFile().listFiles()) {
            SSG ssg = SSG.deserializeSSG(ssgFile.toPath());
            process(ssg);
        }
        // Output invariants: unaryInvariants
    }

    public void process(SSG ssg) {
        handleUnaryInvariant(ssg);
    }

    public void handleUnaryInvariant(SSG ssg) {
        // Primitive, String, Enum, Collection, Array
        // Collection size (As the very first example throughout the design)

        // (1) Find all collection node
        List<Node> collectionOrArrayNodes = findCollectionOrArray(ssg);
        for (Node node : collectionOrArrayNodes) {
            if (!node.variableInfo.name.contains("."))
                return;
            // There might be multiple parents
            List<VarInfo> varInfos = VarInfo.fromNode(node);

            for (VarInfo varInfo : varInfos) {
                if (!unaryInvariantMap.containsKey(varInfo)) {
                    SSGPointSlice1 ssgPointSlice1 = new SSGPointSlice1(varInfo);
                    ssgPointSlice1.instantiate_invariants();
                    unaryInvariantMap.put(varInfo, ssgPointSlice1);
                }
                SSGPointSlice ssgPointSlice = unaryInvariantMap.get(varInfo);
            }
        }
        // (2) Init SSGPointSlices for them
        // (3) For each invariant, update using the given value
    }

    public void handleBinaryInvariant(SSG ssg) {
    }

    public static List<Node> findCollectionOrArray(SSG ssg) {
        System.out.println("Root Nodes size: " + ssg.rootNodeMap.size());

        List<Node> collectionOrArrayNodes = new LinkedList<>();

        for (Node node : ssg.rootNodeMap.values()) {
            List<Node> targetNodes = findCollectionNode(node);

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

        // Know which field it belongs to
        // List<String> fieldRelations = new LinkedList<>();
        // for (Node node : collectionOrArrayNodes) {
        // // System.out.println("Node: " + node.variableInfo);
        // for (Node parent : node.parents) {
        // fieldRelations.add(parent.variableInfo.className + " : " +
        // node.variableInfo.name
        // + " : " + "children size = " + node.children.size());
        // }
        // }
        //
        // for (String s : fieldRelations) {
        // System.out.println(s);
        // }
        return collectionOrArrayNodes;
    }

    public static List<Node> findCollectionNode(Node node) {
        List<Node> result = new LinkedList<>();
        if (node == null) {
            return result;
        }
        if (node.isCollectionOrArray()) {
            result.add(node);
        }
        for (Node child : node.children) {
            result.addAll(findCollectionNode(child));
        }
        return result;
    }

    // Test Usage
    public static void main(String[] args) {
        InferenceEngine engine = new InferenceEngine();
        engine.run();
    }

}
