package org.zlab.dinv.visibility;

import picocli.CommandLine;

public class Main {

    public static void main(String[] args) {
        // "-infopath PATH_TO_INFO"
        RewriteExec rewriteExec = new RewriteExec();
        new CommandLine((rewriteExec)).execute(args);
    }

}
