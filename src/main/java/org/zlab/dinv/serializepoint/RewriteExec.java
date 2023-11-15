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
            serializePoint.className = serializePoint.className.replace("$", ".");
            // System.out.println(serializePoint);
        }

        Set<SerializePoint> filteredSerializePoints = new HashSet<>();
        for (SerializePoint serializePoint : serializePoints) {
            if (serializePoint.type == SerializePoint.Type.fieldRef
                    && !serializePoint.parentName.contains("$")
                    && !serializePoint.fieldName.contains("$")
                    && !serializePoint.parentName.contains("#")
                    && !serializePoint.fieldName.contains("#")) {
                filteredSerializePoints.add(serializePoint);
            }
        }
        serializePoints = filteredSerializePoints;

        // Maintain a more efficient data structure for serialization points
        Map<String, Map<Integer, Set<SerializePoint>>> serializePointsMap = new HashMap<>();
        for (SerializePoint serializePoint : serializePoints) {
            if (!serializePointsMap.containsKey(serializePoint.className)) {
                serializePointsMap.put(serializePoint.className, new HashMap<>());
            }
            Map<Integer, Set<SerializePoint>> lineMap = serializePointsMap
                    .get(serializePoint.className);
            if (!lineMap.containsKey(serializePoint.lineNumber)) {
                lineMap.put(serializePoint.lineNumber, new HashSet<>());
            }
            Set<SerializePoint> serializePointSet = lineMap.get(serializePoint.lineNumber);
            serializePointSet.add(serializePoint);
        }

        // ----------Instrument Logs---------
        try {
            instSerializePointLog(targetSystemPath, serializePointsMap, infoPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void instSerializePointLog(List<Path> targetSystemPath,
            Map<String, Map<Integer, Set<SerializePoint>>> serializePointsMap, Path infoPath)
            throws IOException {

        InstSerializePoint instSerializePoint = new InstSerializePoint(serializePointsMap);

        // Walk the project directory structure and find all the Java source files
        for (Path systemPath : targetSystemPath) {
            Files.walk(systemPath).filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                        try {
                            // ==hotspot==
                            // if (p.toString().contains("/DecoratedKey.java"))
                            // return;
                            // if (p.toString().contains("/BufferDecoratedKey.java"))
                            // return;
                            // if (p.toString().contains("composites"))
                            // return;

                            // ==debug==
                            // if (!p.toString().contains("org/apache/cassandra/db/"))
                            // return;
                            // if (!p.toString().contains("/TestArray2.java"))
                            // return;

                            if (org.zlab.dinv.Utils.exclude(p))
                                return;
                            CompilationUnit cu = StaticJavaParser.parse(p.toFile());
                            // Traverse the AST and perform the desired processing
                            boolean injected = instSerializePoint.process(cu);
                            if (injected)
                                Files.write(p, cu.toString().getBytes());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        }
    }
}
