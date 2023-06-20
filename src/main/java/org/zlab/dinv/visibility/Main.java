package org.zlab.dinv.visibility;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Main {
    // input: Map<String, Map<String, Set<Integer>>> targetIfBranches
    //      - {Class-> {MethodName, lineSet}}
    // output: overwrite the if branches as local fields, so daikon can instrument them

    public static void main(String[] args) throws IOException {
        // arg1
        Path projectRootDir = Paths.get("/Users/hanke/Project/cassandra/cassandra1/src/java/org/apache/cassandra");

        // arg2
        Path branchLocationPath =
                Paths.get("/Users/hanke/Desktop/Project/vasco/system/cassandra/apache-cassandra-3.11.15/programLocations_alg4_outputstream_branches.json");
        Map<String, Set<Integer>> branchLocations = org.zlab.dinv.isserialize.Utils.loadProgramLocations(branchLocationPath);
        branchLocations = org.zlab.dinv.extractvars.Utils.replaceDollarWithDot(branchLocations);
        rewriteVisibility(projectRootDir, branchLocations);

        // output ppts
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
                        if (!p.toString().contains("/CommitLogReader.java")) return;
                        CompilationUnit cu = StaticJavaParser.parse(p.toFile());
                        // Traverse the AST and perform the desired processing
                        instField.process(cu);

                        Files.write(p, cu.toString().getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

}
