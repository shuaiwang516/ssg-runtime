package org.zlab.dinv.logger.inv;

public class InferenceEngine {

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

    }

    public static void main(String[] args) {
        InferenceEngine engine = new InferenceEngine();
        engine.run();
    }

}
