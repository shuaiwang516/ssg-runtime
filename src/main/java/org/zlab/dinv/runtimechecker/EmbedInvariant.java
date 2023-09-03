package org.zlab.dinv.runtimechecker;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import org.zlab.dinv.Config;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

@CommandLine.Command(name = "EmbedInvariant", mixinStandardHelpOptions = true, version = "1.0", description = "EmbedInvariant does amazing things.")
public class EmbedInvariant implements Runnable {

    @CommandLine.Option(names = {"-infoPath"}, description = "path to files generated from vasco")
    private Path infoPath;

    @CommandLine.Option(names = {
            "-targetSystemPath"}, description = "path to system being rewritten")
    private List<Path> targetSystemPath;

    @Override
    public void run() {
        System.out.println("[EmbedInvariant] infoPath = " + infoPath);
        System.out.println("[EmbedInvariant] targetSystemPath = " + targetSystemPath);

        Path targetInvPath = infoPath.resolve("inv.txt");
        Path targetIsSerializeInvPath = infoPath.resolve("isSerializeInvs.txt");

        Map<String, Set<String>> invs = LoadInvariant.load(targetInvPath);
        if (targetIsSerializeInvPath.toFile().exists()) {
            Map<String, Set<String>> isSerializeInvs = LoadInvariant.load(targetIsSerializeInvPath);
            Utils.mergeInv(invs, isSerializeInvs);
        } else {
            System.out.println("[Warning] isSerialized inv is not provided");
        }

        // Walk the project directory structure and find all the Java source files

        try {
            for (Path systemPath : targetSystemPath) {
                Files.walk(systemPath).filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                            try {
                                // debug
                                // if (!p.toString().contains("RangeTombstoneList")) return;
                                if (org.zlab.dinv.Utils.exclude(p))
                                    return;
                                CompilationUnit cu = StaticJavaParser.parse(p.toFile());
                                // Traverse the AST and perform the desired processing
                                cu.accept(new InstrumentInvariant.InstClassVisitor(invs), null);

                                Files.write(p, cu.toString().getBytes());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
