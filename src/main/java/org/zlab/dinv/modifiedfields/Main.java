package org.zlab.dinv.modifiedfields;

import picocli.CommandLine;

public class Main {

    public static void main(String[] args) {
        ModifiedFields modifiedFields = new ModifiedFields();
        new CommandLine((modifiedFields)).execute(args);
    }
}
