package org.zlab.dinv.logger.inv;

import org.zlab.dinv.logger.SSG;
import org.zlab.dinv.logger.SSGPointSlice;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

public class InferenceEngine {

    public List<SSGPointSlice> unaryInvariants = new LinkedList<>();

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
        // Update (1) unaryInvariants (2) binaryInvariants

        // If some ssg point doesn't exist, create a new ssg point slice
        // Init unary invariants
        // Init binary invariants
        // Else: validate/improve the likely invariants of the ssg point slice
        // Validate unary invariants
        // Validate binary invariants
    }

    public static void main(String[] args) {
        InferenceEngine engine = new InferenceEngine();
        engine.run();
    }

}
