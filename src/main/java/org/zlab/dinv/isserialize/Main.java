package org.zlab.dinv.isserialize;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Main {

    // public static Path systemInfoPath = Paths.get("/Users/hanke/Desktop/Project/vasco/system/hdfs/hadoop-3.3.4/");
    public static Path systemInfoPath = Paths.get("/Users/hanke/Desktop/Project/vasco/system/cassandra/apache-cassandra-3.11.15/");

    public static void main(String[] args) throws IOException {
        Path projectRootDir = Paths.get("/Users/hanke/Project/cassandra/cassandra1/src/java/org/apache/cassandra");
        Path serializeLocationsPath =
                systemInfoPath.resolve("isSerializeProgramLocations.json");
        Map<String, Set<Integer>> serializeLocations = Utils.loadProgramLocations(serializeLocationsPath);
        serializeLocations = org.zlab.dinv.extractvars.Utils.replaceDollarWithDot(serializeLocations);
        rewriteVisibility(projectRootDir, serializeLocations);
    }

    public static void rewriteVisibility(Path projectRootDir, Map<String, Set<Integer>> serializeLocations) throws IOException {
        InstrumentSerializeLocation instrumentSerializeLocation = new InstrumentSerializeLocation(serializeLocations);

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
                        instrumentSerializeLocation.process(cu);

                        Files.write(p, cu.toString().getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        Utils.savePptVars(instrumentSerializeLocation.pptVars, Paths.get("output/pptVars_alg3.json"));
        Utils.PPT2DaikonInput(instrumentSerializeLocation.pptVars, Paths.get("output/instrument_alg3_vars_file"));
    }

}
