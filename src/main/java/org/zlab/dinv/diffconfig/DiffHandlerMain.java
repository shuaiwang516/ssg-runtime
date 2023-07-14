package org.zlab.dinv.diffconfig;

import picocli.CommandLine;

public class DiffHandlerMain {

    public static void main(String[] args) {
        DiffHandler diffHandler = new DiffHandler();
        new CommandLine(diffHandler).execute(args);
    }
}
