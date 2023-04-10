package org.zlab.dinv.runtimechecker;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static com.github.javaparser.StaticJavaParser.parse;

public class Main {
    // input: inv, source code path
    // output: embed the invariants into the program points

    // process the entire project

    public static void test() throws IOException {
        String projectRootDir = "/Users/hanke/Project/cassandra/cassandra1/src/java/org/apache/cassandra";

        Path targetInv = Paths.get("input/cassandra_inv");
        Map<String, List<String>> invs =  LoadInvariant.load(targetInv);

        // Walk the project directory structure and find all the Java source files
        Files.walk(Paths.get(projectRootDir))
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(p -> {
                    try {
                        // debug
                        // if (!p.toString().contains("/Clustering.java")) return;

                        CompilationUnit cu = StaticJavaParser.parse(p.toFile());
                        // Traverse the AST and perform the desired processing
                        cu.accept(new InstrumentInvariant.InstClassVisitor(invs), null);
                        // Optionally, write the modified AST back to the original file
                        Files.write(p, cu.toString().getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    public static void main(String[] args) throws IOException {
        test();
    }


}
