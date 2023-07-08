package org.zlab.dinv.diffconfig;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import org.zlab.dinv.Config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DiffHandler {

    // public static Path oldProjectRootDir = Paths.get("/Users/hanke/Project/cassandra/cassandra1/src/java/org/apache/cassandra");
    // public static Path newProjectRootDir = Paths.get("/Users/hanke/Project/cassandra/cassandra1/src/java/org/apache/cassandra");

    public static Path oldProjectRootDir = Paths.get("/Users/hanke/Project/dinv-test");
    public static Path newProjectRootDir = Paths.get("/Users/hanke/Project/dinv-test1");
    public static Path oldConfig2programLocationsPath = Paths.get("input/oldConfig2programLocations.json");
    public static Path newConfig2programLocationsPath = Paths.get("input/newConfig2programLocations.json");

    public static void main(String[] args) throws IOException {
        // Input: program locations + source code
        Map<String, Map<String, Map<String, Set<Integer>>>> oldConfig2branchProgramLocations = Utils.loadFields2Locations(oldConfig2programLocationsPath);
        Map<String, Map<String, Map<String, Set<Integer>>>> newConfig2branchProgramLocations = Utils.loadFields2Locations(newConfig2programLocationsPath);

        // Merge program locations
        Map<String, Set<Integer>> oldProgramLocations = mergeProgramLocations(oldConfig2branchProgramLocations);
        Map<String, Set<Integer>> newProgramLocations = mergeProgramLocations(newConfig2branchProgramLocations);

        // Given the program locations, return:
        Map<String, Map<Integer, String>> oldProgramLocation2block =  retrieveHandlerBlock(oldProjectRootDir, oldProgramLocations);
        Map<String, Map<Integer, String>> newProgramLocation2block =  retrieveHandlerBlock(newProjectRootDir, newProgramLocations);

        Map<String, Map<String, Set<String>>> oldConfig2handlers = constructConfig2Handlers(
                oldConfig2branchProgramLocations,
                oldProgramLocation2block
        );
        Map<String, Map<String, Set<String>>> newConfig2handlers = constructConfig2Handlers(
                newConfig2branchProgramLocations,
                newProgramLocation2block
        );

        Map<String, Set<String>> modifiedHandlerConfig = new HashMap<>();

        // location is not important, now we want Map<String, Set<String>>
        // Calculate the edit distance between two blocks (if the number is different
        // For each config do the comparison
        // Output: config names: edit distance
        for (String configClassName: oldConfig2handlers.keySet()) {
            if (!newConfig2handlers.containsKey(configClassName))
                continue;
            for (String configName: oldConfig2handlers.get(configClassName).keySet()) {
                if (!newConfig2handlers.get(configClassName).containsKey(configName))
                    continue;
                Set<String> oldHandlerStrings = oldConfig2handlers.get(configClassName).get(configName);
                Set<String> newHandlerStrings = newConfig2handlers.get(configClassName).get(configName);

                boolean changedHandler = false;

                if (oldHandlerStrings.isEmpty() && newHandlerStrings.isEmpty())
                    continue;

                if (oldHandlerStrings.size() != newHandlerStrings.size())
                    changedHandler = true;

                if (!changedHandler) {
                    for (String oldHandlerString: oldHandlerStrings) {
                        int minEditDistance = Integer.MAX_VALUE;
                        // if non of the comparison has edit distance smaller than xxx, we pick the configuration!
                        for (String newHandlerString: newHandlerStrings) {
                            int editDistance = calEditDistance(oldHandlerString, newHandlerString);
                            minEditDistance = Math.min(editDistance, minEditDistance);
                        }
                        if (minEditDistance > Config.EDIT_DISTANCE_THRESHOLD)
                            changedHandler = true;
                    }
                }


                if (changedHandler) {
                    // include this config
                    if (!modifiedHandlerConfig.containsKey(configClassName)) {
                        modifiedHandlerConfig.put(configClassName, new HashSet<>());
                    }
                    modifiedHandlerConfig.get(configClassName).add(configName);
                }
            }
        }

        org.zlab.dinv.extractvars.Utils.saveModifiedFields(modifiedHandlerConfig, Paths.get("output/modifiedHandlerConfigs.json"));
    }

    public static Map<String, Map<String, Set<String>>> constructConfig2Handlers(
            Map<String, Map<String, Map<String, Set<Integer>>>> config2programLocations,
            Map<String, Map<Integer, String>> programLocation2block) {
        Map<String, Map<String, Set<String>>> config2handlers = new HashMap<>();
        for (String configClassName: config2programLocations.keySet()) {
            for (String configName: config2programLocations.get(configClassName).keySet()) {
                Map<String, Set<Integer>> programLocations = config2programLocations.get(configClassName).get(configName);
                for (String className: programLocations.keySet()) {
                    for (Integer lineNumber: programLocations.get(className)) {
                        // retrieve the block!
                        if (programLocation2block.containsKey(className)
                                && programLocation2block.get(className).containsKey(lineNumber)) {
                            String handlerString = programLocation2block.get(className).get(lineNumber);
                            // add it to output
                            if (!config2handlers.containsKey(configClassName)) {
                                config2handlers.put(configClassName, new HashMap<>());
                            }
                            if (!config2handlers.get(configClassName).containsKey(configName)) {
                                config2handlers.get(configClassName).put(configName, new HashSet<>());
                            }
                            config2handlers.get(configClassName).get(configName).add(handlerString);
                        }
                    }
                }
            }
        }
        return config2handlers;
    }

    public static Map<String, Set<Integer>> mergeProgramLocations(Map<String, Map<String, Map<String, Set<Integer>>>> config2branchProgramLocations) {
        Map<String, Set<Integer>> programLocations = new HashMap<>();
        for (Map<String, Map<String, Set<Integer>>> v1 : config2branchProgramLocations.values()) {
            for (Map<String, Set<Integer>> tmpProgramLocations: v1.values()) {
                org.zlab.dinv.isserialize.Utils.mergeProgramLocations(programLocations, tmpProgramLocations);
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

                        // Files.write(p, cu.toString().getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        return retrieveHandlerBlock.getProgramLocation2block();
    }

    public static int calEditDistance(String string1, String string2) {
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
