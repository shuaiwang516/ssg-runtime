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

    @CommandLine.Option(names = {
            "-writePointsPath"}, description = "path to files generated from vasco")
    private Path writePointsPath;

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
        Set<SerializePoint> serializePoints;
        Set<WritePoint> writePoints;
        if (serializePointsPath == null) {
            System.out.println("serializePointsPath is null");
            serializePoints = new HashSet<>();
        } else {
            serializePoints = Utils.loadSerializePoints(serializePointsPath);
        }
        if (writePointsPath == null) {
            System.out.println("writePointsPath is null");
            writePoints = new HashSet<>();
        } else {
            writePoints = Utils.loadWritePoints(writePointsPath);
        }

        for (SerializePoint serializePoint : serializePoints) {
            serializePoint.className = serializePoint.className.replace("$", ".");
            if (serializePoint.isStatic) {
                serializePoint.parentName = serializePoint.parentName.replace("$", ".");
            }
            // System.out.println(serializePoint);
        }

        Set<SerializePoint> filteredSerializePoints = new HashSet<>();
        for (SerializePoint serializePoint : serializePoints) {
            if (serializePoint.type == SerializePoint.Type.fieldRef) {
                if (!serializePoint.parentName.contains("$")
                        && !serializePoint.fieldName.contains("$")
                        && !serializePoint.parentName.contains("#")
                        && !serializePoint.fieldName.contains("#")) {
                    filteredSerializePoints.add(serializePoint);
                } else {
                    // System.out.println("filtered out: " + serializePoint);
                }
            } else {
                filteredSerializePoints.add(serializePoint);
            }
        }
        serializePoints = filteredSerializePoints;

        // Maintain a more efficient data structure for serialization points
        Map<String, Map<Integer, Set<SerializePoint>>> serializePointsMap = Utils
                .getSerializePointsMap(serializePoints);
        Map<String, Map<Integer, Set<WritePoint>>> writePointsMap = Utils
                .getWritePointsMap(writePoints);

        // System.out.println("size = " + serializePointsMap.keySet().size());
        // for (String clazz : serializePointsMap.keySet()) {
        // System.out.println("clazz = " + clazz + " size = "
        // + serializePointsMap.get(clazz).keySet().size());
        // }

        // ----------Instrument Logs---------
        try {
            instSerializePointLog(targetSystemPath, serializePointsMap, writePointsMap, infoPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void instSerializePointLog(List<Path> targetSystemPath,
            Map<String, Map<Integer, Set<SerializePoint>>> serializePointsMap,
            Map<String, Map<Integer, Set<WritePoint>>> writePointsMap, Path infoPath)
            throws IOException {
        // You can set other configurations as needed

        InstSerializePoint instSerializePoint = new InstSerializePoint(serializePointsMap);
        InstWritePoint instWritePoint = new InstWritePoint(writePointsMap);

        // Walk the project directory structure and find all the Java source files
        for (Path systemPath : targetSystemPath) {
            Files.walk(systemPath).filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                        try {
                            // ==hotspot==
                            // if (p.toString().contains("/DecoratedKey.java"))
                            // return;
                            // if (!p.toString().contains("/CompactionMetadata.java"))
                            // return;
                            // if (p.toString().contains("composites"))
                            // return;

                            // ==debug==
                            // if (!p.toString().contains("org/apache/cassandra/db/composites/"))
                            // return;
                            // if (!p.toString().contains("org/apache/cassandra/db/"))
                            // return;
                            // if (!p.toString().contains("/TestStreamCapture.java"))
                            // return;

                            if (org.zlab.dinv.Utils.exclude(p))
                                return;
                            CompilationUnit cu = StaticJavaParser.parse(p.toFile());
                            // Traverse the AST and perform the desired processing
                            boolean injected = false;
                            // if (instSerializePoint.process(cu))
                            // injected = true;
                            if (instWritePoint.process(cu))
                                injected = true;
                            if (injected)
                                Files.write(p, cu.toString().getBytes());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        }
    }
}
