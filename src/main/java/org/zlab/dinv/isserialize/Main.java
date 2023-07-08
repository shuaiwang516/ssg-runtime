package org.zlab.dinv.isserialize;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import org.zlab.dinv.Config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Main {

    public static void main(String[] args) throws IOException {
        Path serializeLocationsPath = Config.systemInfoPath.resolve("isSerializeProgramLocations.json");
        Map<String, Set<Integer>> serializeLocations = Utils.loadProgramLocations(serializeLocationsPath);
        serializeLocations = org.zlab.dinv.extractvars.Utils.replaceDollarWithDot(serializeLocations);
        rewriteVisibility(Config.projectRootDir, serializeLocations);
    }

    public static void rewriteVisibility(Path projectRootDir, Map<String, Set<Integer>> serializeLocations) throws IOException {
        InstSer instSer = new InstSer(serializeLocations);

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
                        instSer.process(cu);

                        Files.write(p, cu.toString().getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        Utils.savePptVars(instSer.pptVars, Paths.get("output/pptVars_alg3.json"));
        Utils.PPT2DaikonInput(instSer.pptVars, Paths.get("output/instrument_alg3_vars_file"));
    }

}
