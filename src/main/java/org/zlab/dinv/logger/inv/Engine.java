package org.zlab.dinv.logger.inv;

import org.jgrapht.graph.DirectedMultigraph;
import org.zlab.dinv.logger.inv.unary.UpperBound;
import org.zlab.dinv.logger.ssg.Edge;
import org.zlab.dinv.logger.ssg.SSGraph;
import org.zlab.dinv.logger.ssg.Vertex;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Engine {

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
    public void run(Path ssgFolder) {
        assert ssgFolder.toFile().isDirectory();
        for (File ssgFile : ssgFolder.toFile().listFiles()) {
            System.out.println("Loading " + ssgFile.getName());
            DirectedMultigraph<Vertex, Edge> ssg = SSGraph.deserializeSSG(ssgFile.toPath());
            System.out.println("Processing " + ssgFile.getName());
            process(ssg);
        }
        // Output invariants: unaryInvariants
        for (SSGPointSlice1 ssgPointSlice1 : unaryInvariantMap.values()) {
            for (Invariant inv : ssgPointSlice1.collection_size_invs) {
                System.out.println("collection size: " + inv.formatString());
            }
        }
    }

    public void process(DirectedMultigraph<Vertex, Edge> ssg) {
        handleUnaryInvariant(ssg);
    }

    public void handleUnaryInvariant(DirectedMultigraph<Vertex, Edge> ssg) {
        handleCollectionSizeInvariant(ssg);
        handlePrimitiveInvariant(ssg);
    }

    public void handleCollectionSizeInvariant(DirectedMultigraph<Vertex, Edge> ssg) {
        List<Vertex> collectionOrArrayNodes = findCollectionOrArray(ssg);

        for (Vertex vertex : collectionOrArrayNodes) {
            List<VarInfo> varInfos = VarInfo.fromVertex(vertex, ssg);
            for (VarInfo varInfo : varInfos) {
                if (!unaryInvariantMap.containsKey(varInfo)) {
                    SSGPointSlice1 ssgPointSlice1 = new SSGPointSlice1(varInfo);
                    ssgPointSlice1.instantiate_invariants();
                    ssgPointSlice1.instantiate_collection_size_invariants();
                    unaryInvariantMap.put(varInfo, ssgPointSlice1);
                }
                SSGPointSlice1 ssgPointSlice1 = unaryInvariantMap.get(varInfo);
                Integer collectionSize = ssg.outDegreeOf(vertex);
                ssgPointSlice1.addCollectionSize(collectionSize, 1);
            }
        }
    }

    public void handlePrimitiveInvariant(DirectedMultigraph<Vertex, Edge> ssg) {
        // TODO
    }

    public void handleBinaryInvariant(DirectedMultigraph<Vertex, Edge> ssg) {
        // TODO
    }

    public static List<Vertex> findCollectionOrArray(DirectedMultigraph<Vertex, Edge> ssg) {
        List<Vertex> collectionOrArrayNodes = new LinkedList<>();
        for (Vertex o : ssg.vertexSet()) {
            if (o.isCollectionOrArray()) {
                if (ssg.outDegreeOf(o) == 0) {
                    continue;
                }
                collectionOrArrayNodes.add(o);
            }
        }
        return collectionOrArrayNodes;
    }

    // Test Usage
    public static void main(String[] args) {
        Engine engine = new Engine();
        Path ssgFolder = Paths.get("example_ssgraph_folder");
        engine.run(ssgFolder);
    }

}
