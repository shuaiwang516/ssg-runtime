package org.zlab.dinv.visibility;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import org.zlab.dinv.Config;
import org.zlab.dinv.isserialize.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Main {
    // input: Map<String, Map<String, Set<Integer>>> targetIfBranches
    //      - {Class-> {MethodName, lineSet}}
    // output: overwrite the if branches as local fields, so daikon can instrument them

    public static Path outputStreamBranchLocationPath =
            Config.systemInfoPath.resolve("programLocations_alg4_outputstream_branches.json");
    public static Path dataBranchLocationPath =
            Config.systemInfoPath.resolve("programLocations_alg4_data_branches.json");

    public static void main(String[] args) throws IOException {
        Map<String, Set<Integer>> branchLocations = org.zlab.dinv.isserialize.Utils.loadProgramLocations(outputStreamBranchLocationPath);
        Map<String, Set<Integer>> dataBranchLocations = org.zlab.dinv.isserialize.Utils.loadProgramLocations(dataBranchLocationPath);
        // merge two branch locations
        Utils.mergeProgramLocations(branchLocations, dataBranchLocations);
        branchLocations = org.zlab.dinv.extractvars.Utils.replaceDollarWithDot(branchLocations);
        rewriteVisibility(Config.projectRootDir, branchLocations);
    }

    public static void rewriteVisibility(Path projectRootDir, Map<String, Set<Integer>> branchLocations) throws IOException {
        InstField instField = new InstField(branchLocations);

        // Walk the project directory structure and find all the Java source files
        Files.walk(projectRootDir)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(p -> {
                    try {
                        // debug
                        // if (!p.toString().contains("/RewindableDataInputStreamPlus.java")) return;
                        CompilationUnit cu = StaticJavaParser.parse(p.toFile());
                        // Traverse the AST and perform the desired processing
                        instField.process(cu);

                        Files.write(p, cu.toString().getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        org.zlab.dinv.isserialize.Utils.savePptVars(instField.pptVars,
                Paths.get("output/pptVars_alg4.json"));
        org.zlab.dinv.isserialize.Utils.PPT2DaikonInput(
                instField.pptVars, Paths.get("output/instrument_alg4_vars_file"));
    }

}
