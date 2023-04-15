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
        Path targetIfBranchPath = Paths.get("input/targetIfInfo_example");
        Map<String, Map<String, Set<Integer>>> targetIfBranches = Utils.readIfInfo(targetIfBranchPath);

        rewriteVisibility(projectRootDir, targetIfBranches);
    }

    public static void rewriteVisibility(Path projectRootDir, Map<String, Map<String, Set<Integer>>> targetIfBranches) throws IOException {
        InstField instField = new InstField(targetIfBranches);

        // Walk the project directory structure and find all the Java source files
        Files.walk(projectRootDir)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(p -> {
                    try {
                        // debug
                        if (!p.toString().contains("/ColumnIndex.java")) return;
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
