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

@CommandLine.Command(name = "RewriteExec", mixinStandardHelpOptions = true, version = "1.0",
        description = "RewriteExec does amazing things.")
public class RewriteExec implements Runnable {

    @CommandLine.Option(names = { "-infoPath" }, description = "path to files generated from vasco")
    private String infopath;

    @CommandLine.Option(names = { "-targetSystemPath" }, description = "path to system being rewritten")
    private String targetSystemPath;

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
        Path projectRootDir = Config.projectRootDir;
        if (targetSystemPath != null) {
            projectRootDir = Paths.get(targetSystemPath);
        }
        System.out.println("[RewriteExec] systemInfoPath = " + systemInfoPath);
        System.out.println("[RewriteExec] target projectRootDir = " + projectRootDir);

        // make sure at least one info file is provided
        Path outputStreamBranchLocationPath =
                systemInfoPath.resolve("programLocations_alg4_outputstream_branches.json");
        Path dataBranchLocationPath =
                systemInfoPath.resolve("programLocations_alg4_data_branches.json");
        Path serializeLocationsPath = systemInfoPath.resolve("isSerializeProgramLocations.json");

        if (!serializeLocationsPath.toFile().exists()
                && !outputStreamBranchLocationPath.toFile().exists()
                && !dataBranchLocationPath.toFile().exists()) {
            throw new RuntimeException("no program location file is provided" +
                    ", please check the input path");
        }

        // isSerialize inst
        Map<String, Set<Integer>> serializeLocations = null;
        if (serializeLocationsPath.toFile().exists()) {
            serializeLocations = org.zlab.dinv.modifiedfields.Utils.replaceDollarWithDot(Utils.loadProgramLocations(serializeLocationsPath));
        } else {
            System.out.println("[Warning] serializeLocations is not provided, choose not to use");
        }

        // branch comparison inst
        Map<String, Set<Integer>> branchLocations = null;
        Map<String, Set<Integer>> dataBranchLocations = null;
        if (outputStreamBranchLocationPath.toFile().exists() && dataBranchLocationPath.toFile().exists()) {
            branchLocations = Utils.loadProgramLocations(outputStreamBranchLocationPath);
            dataBranchLocations = Utils.loadProgramLocations(dataBranchLocationPath);
            // merge two branch locations
            Utils.mergeProgramLocations(branchLocations, dataBranchLocations);
            branchLocations = org.zlab.dinv.modifiedfields.Utils.replaceDollarWithDot(branchLocations);
        } else {
            System.out.println("[Warning] data/outputstream locations are not both provided, choose not to use");
        }

        try {
            rewriteVisibility(projectRootDir, serializeLocations, branchLocations, systemInfoPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void rewriteVisibility(Path projectRootDir,
                                         Map<String, Set<Integer>> serializeLocations,
                                         Map<String, Set<Integer>> branchLocations,
                                         Path systemInfoPath
                                  )
            throws IOException {
        if (serializeLocations == null && branchLocations == null) {
            System.out.println("no location is not provided, return");
        }
        InstSer instSer;
        InstField instField;
        if (serializeLocations != null)
            instSer = new InstSer(serializeLocations);
        else {
            instSer = null;
        }
        if (branchLocations != null)
            instField = new InstField(branchLocations);
        else {
            instField = null;
        }

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
                        if (instSer != null)
                            instSer.process(cu);
                        if (instField != null)
                            instField.process(cu);
                        Files.write(p, cu.toString().getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        // isSerialize ppt vars
        if (instSer != null) {
            Utils.savePptVars(instSer.pptVars, systemInfoPath.resolve("pptVars_alg3.json"));
            Utils.PPT2DaikonInput(instSer.pptVars, systemInfoPath.resolve("instrument_alg3_vars_file"));
        }

        if (instField != null) {
            // branch ppt vars
            Utils.savePptVars(instField.pptVars, systemInfoPath.resolve("pptVars_alg4.json"));
            Utils.PPT2DaikonInput(instField.pptVars, systemInfoPath.resolve("instrument_alg4_vars_file"));
        }
    }
}
