package org.zlab.dinv.diffconfig;

import picocli.CommandLine;

public class RetrieveDiffConfigMain {

    public static void main(String[] args) {
        RetrieveDiffConfig retrieveDiffConfig = new RetrieveDiffConfig();
        new CommandLine(retrieveDiffConfig).execute(args);
    }
}
