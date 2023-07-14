package org.zlab.dinv.diffconfig;

import picocli.CommandLine;

public class ConfigMain {

    public static void main(String[] args) {
        RetrieveConfig retrieveConfig = new RetrieveConfig();
        new CommandLine(retrieveConfig).execute(args);
    }
}
