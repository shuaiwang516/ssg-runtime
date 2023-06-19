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
import java.util.Map;
import java.util.Set;

public class Main {
    // input: inv, source code path
    // output: embed the invariants into the program points

    // process the entire project
    public static String cassandrRootDir = "/Users/hanke/Project/cassandra/cassandra1/src/java/org/apache/cassandra";
    public static String cassInvPath = "/Users/hanke/Desktop/Project/vasco/output/system/cassandra/apache-cassandra-3.11.15/inv.txt";
    public static String cassIsSerializeInvPath = "/Users/hanke/Desktop/Project/vasco/output/system/cassandra/apache-cassandra-3.11.15/isSerializeInvs.txt";

    public static String hdfsRootDir = "/Users/hanke/Desktop/Project/hadoop/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/server";
    public static String hdfsInvPath = "input/target_inv_hdfs_jml";
    public static String hdfsIsSerializeInvPath = "/Users/hanke/Desktop/Project/vasco/output/system/cassandra/apache-cassandra-3.11.15/isSerializeInvs.txt";

    public static String targetSystem = "cassandra";

    public static void test() throws IOException {
        String projectRootDir;
        Path targetInv;
        Path targetIsSerializeInv;

        if (targetSystem.equals("cassandra")) {
            projectRootDir = cassandrRootDir;
            targetInv = Paths.get(cassInvPath);
            targetIsSerializeInv = Paths.get(cassIsSerializeInvPath);
        } else if (targetSystem.equals("hdfs")) {
            projectRootDir = hdfsRootDir;
            targetInv = Paths.get(hdfsInvPath);
            targetIsSerializeInv = Paths.get(hdfsIsSerializeInvPath);
        } else {
            throw new RemoteException("only tested on cassandra or hdfs");
        }

        Map<String, Set<String>> invs =  LoadInvariant.load(targetInv);
        Map<String, Set<String>> isSerializeInvs =  LoadInvariant.load(targetIsSerializeInv);
        Utils.mergeInv(invs, isSerializeInvs);

        // Walk the project directory structure and find all the Java source files
        Files.walk(Paths.get(projectRootDir))
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(p -> {
                    try {
                        // debug
                        // if (!p.toString().contains("DataLimits")) return;
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
