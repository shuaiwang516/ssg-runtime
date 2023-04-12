package org.zlab.dinv.runtimechecker;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

import static com.github.javaparser.StaticJavaParser.parse;

public class Main {
    // input: inv, source code path
    // output: embed the invariants into the program points

    // process the entire project

    public static String cassandrRootDir = "/Users/hanke/Project/cassandra/cassandra1/src/java/org/apache/cassandra";
    public static String cassInvPath = "input/target_inv_cass_jml";

    public static String hdfsRootDir = "/Users/hanke/Desktop/Project/hadoop/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/server/namenode";
    public static String hdfsInvPath = "input/target_inv_hdfs_jml";

    public static String targetSystem = "cassandra";

    public static void test() throws IOException {
        String projectRootDir;
        Path targetInv;

        if (targetSystem.equals("cassandra")) {
            projectRootDir = cassandrRootDir;
            targetInv = Paths.get(cassInvPath);
        } else if (targetSystem.equals("hdfs")) {
            projectRootDir = hdfsRootDir;
            targetInv = Paths.get(hdfsInvPath);
        } else {
            throw new RemoteException("only tested on cassandra or hdfs");
        }

        Map<String, List<String>> invs =  LoadInvariant.load(targetInv);

        // Walk the project directory structure and find all the Java source files
        Files.walk(Paths.get(projectRootDir))
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(p -> {
                    try {
                        // debug
//                         if (!p.toString().contains("/CommitLog.java")) return;
                        CompilationUnit cu = StaticJavaParser.parse(p.toFile());
                        // Traverse the AST and perform the desired processing
                        cu.accept(new InstrumentInvariant.InstClassVisitor(invs), null);

                        Files.write(p, cu.toString().getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    public static void main(String[] args) throws IOException {
        test();
    }

    public static void rewriteFieldsAsPublic(CompilationUnit cu) {
        // Iterate over all non-abstract/interface classes
        cu.findAll(ClassOrInterfaceDeclaration.class, c -> !c.isAbstract() && !c.isInterface())
                .forEach(cls -> {
                    // Iterate over all fields in class
                    cls.getFields().forEach(field -> {
                        field.removeModifier(Modifier.Keyword.PRIVATE, Modifier.Keyword.PROTECTED);
                        field.setModifier(Modifier.Keyword.PUBLIC, true);
                    });
                });
    }
}
