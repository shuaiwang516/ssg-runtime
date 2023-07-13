package org.zlab.dinv.modifiedfields;

import picocli.CommandLine;

public class Main {

    public static void main(String[] args) {
        ModifiedFields rewriteExec = new ModifiedFields();
        new CommandLine((rewriteExec)).execute(args);
    }
}
