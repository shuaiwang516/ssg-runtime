package org.zlab.dinv.visibility;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import org.zlab.dinv.Config;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@CommandLine.Command(name = "SimpleAssignStmtSlicing", mixinStandardHelpOptions = true, version = "1.0",
        description = "SimpleAssignStmtSlicing does amazing things.")
public class RewriteExec implements Runnable {

    @CommandLine.Option(names = { "-infopath" }, description = "path to files generated from vasco")
    private String infopath;

    // input: Map<String, Map<String, Set<Integer>>> targetIfBranches
    //      - {Class-> {MethodName, lineSet}}
    // output: overwrite the if branches as local fields, so daikon can instrument them

    @Override
    public void run() {
        // use arg to decide the systemInfo path
        Path systemInfoPath = Config.systemInfoPath;
        if (infopath != null) {
            systemInfoPath = Paths.get(infopath);
        }
        System.out.println("[RewriteExec] using systemInfoPath = " + systemInfoPath);

        // Path outputStreamBranchLocationPath =
        //         systemInfoPath.resolve("programLocations_alg4_outputstream_branches.json");
        // Path dataBranchLocationPath =
        //         systemInfoPath.resolve("programLocations_alg4_data_branches.json");
        // Path serializeLocationsPath = systemInfoPath.resolve("isSerializeProgramLocations.json");
        //
        // // isSerialize inst
        // Map<String, Set<Integer>> serializeLocations = Utils.loadProgramLocations(serializeLocationsPath);
        // serializeLocations = org.zlab.dinv.extractvars.Utils.replaceDollarWithDot(serializeLocations);
        //
        // // branch comparison inst
        // Map<String, Set<Integer>> branchLocations = Utils.loadProgramLocations(outputStreamBranchLocationPath);
        // Map<String, Set<Integer>> dataBranchLocations = Utils.loadProgramLocations(dataBranchLocationPath);
        // // merge two branch locations
        // Utils.mergeProgramLocations(branchLocations, dataBranchLocations);
        // branchLocations = org.zlab.dinv.extractvars.Utils.replaceDollarWithDot(branchLocations);
        //
        // try {
        //     rewriteVisibility(Config.projectRootDir, serializeLocations, branchLocations);
        // } catch (IOException e) {
        //     throw new RuntimeException(e);
        // }
    }

    public void rewriteVisibility(Path projectRootDir,
                                         Map<String, Set<Integer>> serializeLocations,
                                         Map<String, Set<Integer>> branchLocations)
            throws IOException {
        InstSer instSer = new InstSer(serializeLocations);
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
                        instSer.process(cu);
                        instField.process(cu);

                        Files.write(p, cu.toString().getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        // isSerialize ppt vars
        Utils.savePptVars(instSer.pptVars, Paths.get("output/pptVars_alg3.json"));
        Utils.PPT2DaikonInput(instSer.pptVars, Paths.get("output/instrument_alg3_vars_file"));

        // branch ppt vars
        Utils.savePptVars(instField.pptVars,
                Paths.get("output/pptVars_alg4.json"));
        Utils.PPT2DaikonInput(
                instField.pptVars, Paths.get("output/instrument_alg4_vars_file"));
    }
}
