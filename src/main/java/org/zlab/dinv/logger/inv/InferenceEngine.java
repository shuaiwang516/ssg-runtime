package org.zlab.dinv.logger.inv;

import org.zlab.dinv.logger.Node;
import org.zlab.dinv.logger.SSG;
import org.zlab.dinv.logger.inv.unary.UpperBound;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class InferenceEngine {
    // List of prototype invariants
    public static final ArrayList<Invariant> proto_invs = new ArrayList<>();
    static {
        // TODO: add all proto invariants to proto_invs
        proto_invs.add(new UpperBound());
    }

    public Map<VarInfo, SSGPointSlice1> unaryInvariantMap = new HashMap<>();

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
        for (SSGPointSlice1 ssgPointSlice1 : unaryInvariantMap.values()) {
            for (Invariant inv : ssgPointSlice1.collection_size_invs) {
                System.out.println("collection size: " + inv.formatString());
            }
        }
    }

    public void process(SSG ssg) {
        handleUnaryInvariant(ssg);
    }

    public void handleUnaryInvariant(SSG ssg) {
        handleCollectionSizeInvariant(ssg);
        handlePrimitiveInvariant(ssg);
    }

    public void handleCollectionSizeInvariant(SSG ssg) {
        List<Node> collectionOrArrayNodes = findCollectionOrArray(ssg);

        for (Node node : collectionOrArrayNodes) {
            List<VarInfo> varInfos = VarInfo.fromNode(node);
            for (VarInfo varInfo : varInfos) {
                if (!unaryInvariantMap.containsKey(varInfo)) {
                    SSGPointSlice1 ssgPointSlice1 = new SSGPointSlice1(varInfo);
                    ssgPointSlice1.instantiate_invariants();
                    ssgPointSlice1.instantiate_collection_size_invariants();
                    unaryInvariantMap.put(varInfo, ssgPointSlice1);
                }
                SSGPointSlice1 ssgPointSlice1 = unaryInvariantMap.get(varInfo);
                Integer collectionSize = node.children.size();
                ssgPointSlice1.addCollectionSize(collectionSize, 1);
            }
        }
    }

    public void handlePrimitiveInvariant(SSG ssg) {
        // TODO
    }

    public void handleBinaryInvariant(SSG ssg) {
        // TODO
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
            }
        }
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
