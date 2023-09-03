package org.zlab.dinv.visibility;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@CommandLine.Command(name = "RewriteExec", mixinStandardHelpOptions = true, version = "1.0", description = "RewriteExec does amazing things.")
public class RewriteExec implements Runnable {

    private final String MODIFIED_FOLDER_NAME = "modified";

    @CommandLine.Option(names = {"-infoPath"}, description = "path to files generated from vasco")
    private Path infoPath;

    @CommandLine.Option(names = {
            "-targetSystemPath"}, description = "path to system being rewritten")
    private List<Path> targetSystemPath;

    @CommandLine.Option(names = {
            "-upgradeVersion"}, required = true, description = "upgrade version folder name")
    private String upgradeVersion;

    @Override
    public void run() {
        // use arg to decide the systemInfo path
        Path modifiedPath = infoPath.resolve(MODIFIED_FOLDER_NAME).resolve(upgradeVersion);

        System.out.println("[RewriteExec] infoPath = " + infoPath);
        System.out.println("[RewriteExec] target targetSystemPath = " + targetSystemPath);
        System.out.println("[RewriteExec] upgrade version = " + upgradeVersion);

        // make sure at least one info file is provided
        Path outputStreamBranchLocationPath = infoPath
                .resolve("programLocations_alg4_outputstream_branches.json");
        Path dataBranchLocationPath = infoPath.resolve("programLocations_alg4_data_branches.json");
        Path serializeLocationsPath = modifiedPath.resolve("isSerializeProgramLocations.json");

        if (!serializeLocationsPath.toFile().exists()
                && !outputStreamBranchLocationPath.toFile().exists()
                && !dataBranchLocationPath.toFile().exists()) {
            throw new RuntimeException(
                    "no program location file is provided" + ", please check the input path");
        }

        // isSerialize inst
        Map<String, Set<Integer>> serializeLocations = null;
        if (serializeLocationsPath.toFile().exists()) {
            serializeLocations = org.zlab.dinv.modifiedfields.Utils
                    .replaceDollarWithDot(Utils.loadProgramLocations(serializeLocationsPath));
        } else {
            System.out.println("[Warning] serializeLocations is not provided, choose not to use");
        }

        // branch comparison inst
        Map<String, Set<Integer>> branchLocations = null;
        Map<String, Set<Integer>> dataBranchLocations = null;
        if (outputStreamBranchLocationPath.toFile().exists()
                && dataBranchLocationPath.toFile().exists()) {
            branchLocations = Utils.loadProgramLocations(outputStreamBranchLocationPath);
            dataBranchLocations = Utils.loadProgramLocations(dataBranchLocationPath);
            // merge two branch locations
            Utils.mergeProgramLocations(branchLocations, dataBranchLocations);
            branchLocations = org.zlab.dinv.modifiedfields.Utils
                    .replaceDollarWithDot(branchLocations);
        } else {
            System.out.println(
                    "[Warning] data/outputstream locations are not both provided, choose not to use");
        }

        try {
            rewriteVisibility(targetSystemPath, serializeLocations, branchLocations, infoPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void rewriteVisibility(List<Path> targetSystemPath,
            Map<String, Set<Integer>> serializeLocations, Map<String, Set<Integer>> branchLocations,
            Path infoPath) throws IOException {
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
        for (Path systemPath : targetSystemPath) {
            Files.walk(systemPath).filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                        try {
                            // debug
                            // if (!p.toString().contains("/RewindableDataInputStreamPlus.java"))
                            // return;
                            if (org.zlab.dinv.Utils.exclude(p))
                                return;
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
        }

        // isSerialize ppt vars
        if (instSer != null) {
            Utils.savePptVars(instSer.pptVars, infoPath.resolve("pptVars_alg3.json"));
            Utils.PPT2DaikonInput(instSer.pptVars, infoPath.resolve("instrument_alg3_vars_file"));
        }

        if (instField != null) {
            // branch ppt vars
            Utils.savePptVars(instField.pptVars, infoPath.resolve("pptVars_alg4.json"));
            Utils.PPT2DaikonInput(instField.pptVars, infoPath.resolve("instrument_alg4_vars_file"));
        }
    }
}
