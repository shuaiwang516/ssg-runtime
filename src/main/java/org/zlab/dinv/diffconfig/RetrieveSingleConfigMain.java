package org.zlab.dinv.diffconfig;

import picocli.CommandLine;

public class RetrieveSingleConfigMain {

    public static void main(String[] args) {
        RetrieveSingleConfig retrieveSingleConfig = new RetrieveSingleConfig();
        new CommandLine(retrieveSingleConfig).execute(args);
    }
}
