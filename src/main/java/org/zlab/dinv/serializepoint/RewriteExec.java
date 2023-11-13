package org.zlab.dinv.serializepoint;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@CommandLine.Command(name = "RewriteExec", mixinStandardHelpOptions = true, version = "1.0", description = "RewriteExec does amazing things.")
public class RewriteExec implements Runnable {

    @CommandLine.Option(names = {
            "-serializePointsPath"}, description = "path to files generated from vasco")
    private Path serializePointsPath;

    @CommandLine.Option(names = {"-infoPath"}, description = "path to files generated from vasco")
    private Path infoPath;

    @CommandLine.Option(names = {
            "-targetSystemPath"}, description = "path to system being rewritten")
    private List<Path> targetSystemPath;

    @CommandLine.Option(names = {"-upgradeVersion"}, description = "upgrade version folder name")
    private String upgradeVersion;

    @Override
    public void run() {
        // use arg to decide the systemInfo path
        System.out.println("[RewriteExec] infoPath = " + infoPath);
        System.out.println("[RewriteExec] target targetSystemPath = " + targetSystemPath);
        System.out.println("[RewriteExec] upgrade version = " + upgradeVersion);

        // ----------Load serialize points----------
        Set<SerializePoint> serializePoints = Utils.loadSerializePoints(serializePointsPath);

        for (SerializePoint serializePoint : serializePoints) {
            System.out.println(serializePoint);
        }

        // ----------Instrument Logs---------

        // isSerialize inst
        // relace dollar with dot for all the serialization points
        for (SerializePoint serializePoint : serializePoints) {
            serializePoint.className = serializePoint.className.replace("$", ".");
        }

        try {
            instSerializePointLog(targetSystemPath, serializePoints, infoPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void instSerializePointLog(List<Path> targetSystemPath,
            Set<SerializePoint> serializePoints, Path infoPath) throws IOException {

        InstSerializePoint instSerializePoint = new InstSerializePoint(serializePoints);

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
                            instSerializePoint.process(cu);

                            Files.write(p, cu.toString().getBytes());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        }
    }
}
