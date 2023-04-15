package org.zlab.dinv.visibility;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Main {
    // Input: local variables, output: rewrite them as fields

    public static void main(String[] args) throws IOException {
        Path targetIfBranchPath = Paths.get("input/targetIfInfo_example");
        Path targetFilePath = Paths.get("/Users/hanke/Desktop/Project/vasco/src/test/java/vasco/tests/Template1TestCase.java");

        Map<String, Map<String, Set<Integer>>> targetIfBranches;
        try {
            targetIfBranches = Utils.readIfInfo(targetIfBranchPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println(targetIfBranches);

        CompilationUnit cu = StaticJavaParser.parse(targetFilePath.toFile());
        InstField instField = new InstField(targetIfBranches);
        instField.process(cu);
        System.out.println(cu);

        // Write the modified AST back out to a Java file
        FileOutputStream out = new FileOutputStream(targetFilePath.toFile());
        out.write(cu.toString().getBytes());
        out.close();
    }

}
