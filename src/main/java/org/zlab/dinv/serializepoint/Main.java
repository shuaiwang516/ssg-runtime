package org.zlab.dinv.serializepoint;

import picocli.CommandLine;

public class Main {
    /**
     * Do similar things as visibility/runtimechecker
     */
    public static void main(String[] args) {
        // "-infoPath PATH_TO_INFO -targetSystemPath PATH_TO_SYSTEM"
        RewriteExec rewriteExec = new RewriteExec();
        new CommandLine((rewriteExec)).execute(args);
    }

}
