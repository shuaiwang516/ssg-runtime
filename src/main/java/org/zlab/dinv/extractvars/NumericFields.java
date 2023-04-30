package org.zlab.dinv.extractvars;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import org.zlab.dinv.runtimechecker.Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class NumericFields {
    /**
     * Simple static analysis to extract the numeric fields from a list of target classes.
     * The class should be given with full qualified.
     * input: (1) system source code (2) a list of classes
     * output: a list of numeric fields written into output/numeric_fields.json
     */
    public static Map<String, List<String>> extractNumericFields(String projectRootDir, List<String> targetClasses) throws IOException {

        // remove $
        List<String> targetClassesNoDollar = new LinkedList<>();
        for (String classFullName: targetClasses) {
            targetClassesNoDollar.add(Utils.replaceDollarWithDot(classFullName));
        }

        // Walk the project directory structure and find all the Java source files
        Map<String, List<String>> classToNumericFields = new HashMap<>();

        Files.walk(Paths.get(projectRootDir))
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(p -> {
                    try {
                        // debug
                        // if (!p.toString().contains("/FSEditLogAsync.java")) return;
                        CompilationUnit cu = StaticJavaParser.parse(p.toFile());
                        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
                            if (classDecl.getFullyQualifiedName().isPresent()) {
                                String classFullName = classDecl.getFullyQualifiedName().get();
                                if (!targetClassesNoDollar.contains(classFullName))
                                    return;
                                System.out.println("process class: " + classFullName);
                                List<String> numericFields = new LinkedList<>();
                                classDecl.accept(new NumericFieldVisitor(), numericFields);
                                classToNumericFields.put(classFullName, numericFields);
                            } else {
                                System.out.println("class " +  classDecl.getName() + " full name is null");
                            }
                        });
                        Files.write(p, cu.toString().getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        for (String clazz: classToNumericFields.keySet()) {
            System.out.println("class: " + clazz);
            for (String field: classToNumericFields.get(clazz)) {
                System.out.println("    numeric field: " + field);
            }
        }

        return classToNumericFields;
    }

    public static void writeNumericFields(Path path, Map<String, List<String>> classToNumericFields) {
        ObjectMapper mapper = new ObjectMapper();
        String json;
        try {
            json = mapper.writeValueAsString(classToNumericFields);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        // Save the JSON string to a file
        try (FileWriter fileWriter = new FileWriter(path.toFile())) {
            fileWriter.write(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Map<String, List<String>> readNumericFields(Path path) {
        ObjectMapper mapper = new ObjectMapper();
        File jsonFile = path.toFile();
        Map<String, List<String>> map;
        try {
            map = mapper.readValue(jsonFile, new TypeReference<Map<String, List<String>>>() {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return map;
    }

    public static void main(String[] args) throws IOException {

        String projectRootDir = "/Users/hanke/Project/cassandra/cassandra1/src/java/org/apache/cassandra";
        Path outputPath = Paths.get("output/numeric_fields.json");
        List<String> targetClasses = Utils.readFile(Paths.get("input/cassandra_input_classes.txt"));

        Map<String, List<String>> classToNumericFields = extractNumericFields(projectRootDir, targetClasses);
        writeNumericFields(outputPath, classToNumericFields);
        // readNumericFields(outputPath);
    }
}
