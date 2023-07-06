package org.zlab.dinv.diffconfig;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import org.zlab.dinv.isserialize.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class DiffHandler {

    // public static Path oldProjectRootDir = Paths.get("/Users/hanke/Project/cassandra/cassandra1/src/java/org/apache/cassandra");
    // public static Path newProjectRootDir = Paths.get("/Users/hanke/Project/cassandra/cassandra1/src/java/org/apache/cassandra");

    public static Path oldProjectRootDir = Paths.get("/Users/hanke/Project/cassandra/cassandra1/src/java/org/apache/cassandra");
    public static Path newProjectRootDir = Paths.get("/Users/hanke/Project/cassandra/cassandra1/src/java/org/apache/cassandra");

    public static void main(String[] args) throws IOException {
        // Input: program locations + source code
        Map<String, Map<String, Map<String, Set<Integer>>>> oldConfig2branchProgramLocations = null;
        Map<String, Map<String, Map<String, Set<Integer>>>> newConfig2branchProgramLocations = null;

        // merge program locations
        Map<String, Set<Integer>> oldProgramLocations = mergeProgramLocations(oldConfig2branchProgramLocations);
        Map<String, Set<Integer>> newProgramLocations = mergeProgramLocations(newConfig2branchProgramLocations);

        // given the program locations, return:
        Map<String, Map<Integer, String>> oldProgramLocation2block =  retrieveHandlerBlock(oldProjectRootDir, oldProgramLocations);
        Map<String, Map<Integer, String>> newProgramLocation2block =  retrieveHandlerBlock(newProjectRootDir, newProgramLocations);

        // for each config do the comparison
        // Calculate the edit distance between two blocks (if the number is different
        // output: config names: edit distance
    }

    public static Map<String, Set<Integer>> mergeProgramLocations(Map<String, Map<String, Map<String, Set<Integer>>>> config2branchProgramLocations) {
        Map<String, Set<Integer>> programLocations = new HashMap<>();
        for (Map<String, Map<String, Set<Integer>>> v1 : config2branchProgramLocations.values()) {
            for (Map<String, Set<Integer>> tmpProgramLocations: v1.values()) {
                Utils.mergeProgramLocations(programLocations, tmpProgramLocations);

            }
        }
        return programLocations;
    }

    public static Map<String, Map<Integer, String>> retrieveHandlerBlock(Path projectRootDir, Map<String, Set<Integer>> programLocations) throws IOException {
        RetrieveHandlerBlock retrieveHandlerBlock = new RetrieveHandlerBlock(programLocations);

        // Walk the project directory structure and find all the Java source files
        Files.walk(projectRootDir)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(p -> {
                    try {
                        // debug
                        // if (!p.toString().contains("/ReadCommand.java")) return;
                        CompilationUnit cu = StaticJavaParser.parse(p.toFile());
                        // Traverse the AST and perform the desired processing
                        retrieveHandlerBlock.process(cu);

                        Files.write(p, cu.toString().getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        return retrieveHandlerBlock.getProgramLocation2block();
    }

    public static int editDistance(String string1, String string2) {
        int len1 = string1.length();
        int len2 = string2.length();

        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = (string1.charAt(i - 1) == string2.charAt(j - 1)) ? 0 : 1;

                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[len1][len2];
    }

    public static void computeConfigHandlerDiff() {

    }

}
