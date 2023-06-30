package org.zlab.dinv.extractvars;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import org.zlab.dinv.runtimechecker.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * This class is to iterate all the class definitions and extract the fields whose
 * declaration is modified between versions.
 */
public class ModifiedFields {
    // Old version: String -> Map<String, Type>
    // New version: String -> Map<String, Type>
    public static String oldCassandraRootDir = "/Users/hanke/Project/cassandra/cassandra1/src/java/org/apache/cassandra";
    public static String newCassandraRootDir = "/Users/hanke/Project/cassandra/apache-cassandra-4.1.2-src/src/java/org/apache/cassandra";
    public static String oldHdfsRootDir = "/Users/hanke/Desktop/Project/hadoop/hadoop1/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/";
    public static String newHdfsRootDir = "/Users/hanke/Desktop/Project/hadoop/hadoop2/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/";

    public static List<String> targetPrefixes = new LinkedList<>();

    /**
     * Simple static analysis to extract the numeric fields from a list of target classes.
     * The class should be given with full qualified.
     * input: (1) system source code (2) a list of classes
     * output: a list of numeric fields written into output/numeric_fields.json
     */
    public static Map<String, Map<String, String>> extractFields(
            String projectRootDir, List<String> targetPrefixes) throws IOException {

        // remove $
        List<String> targetPrefixesNoDollar = new LinkedList<>();
        for (String classFullName: targetPrefixes) {
            targetPrefixesNoDollar.add(Utils.replaceDollarWithDot(classFullName));
        }

        // Walk the project directory structure and find all the Java source files
        Map<String, Map<String, String>> classToFields = new HashMap<>();

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
                                if (!org.zlab.dinv.extractvars.Utils.startWithTargetPrefix(targetPrefixesNoDollar, classFullName))
                                    return;
                                System.out.println("process class: " + classFullName);
                                Map<String, String> fields = new HashMap<>();
                                classDecl.accept(new FieldVisitor(), fields);
                                classToFields.put(classFullName, fields);
                            } else {
                                System.out.println("class " +  classDecl.getName() + " full name is null");
                            }
                        });
                        // Files.write(p, cu.toString().getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        // for (String clazz: classToFields.keySet()) {
        //     System.out.println("class: " + clazz);
        //     for (String field: classToFields.get(clazz).keySet()) {
        //         System.out.printf("\t\tfield = %s, type = %s\n", field, classToFields.get(clazz).get(field));
        //     }
        // }
        return classToFields;
    }

    public static void addField(String className, String fieldName, Map<String, Set<String>> modifiedFields) {
        if (!modifiedFields.containsKey(className)) {
            modifiedFields.put(className, new HashSet<>());
        }
        modifiedFields.get(className).add(fieldName);
    }

    public static void printModifiedFields(Map<String, Set<String>> modifiedFields) {
        for (String clazz: modifiedFields.keySet()) {
            System.out.println("class: " + clazz);
            for (String field: modifiedFields.get(clazz)) {
                System.out.printf("\t\tfield = %s\n", field);
            }
        }
    }

    public static Map<String, Set<String>> captureModifiedFields(Map<String, Map<String, String>> oldClassToFields,
                                                   Map<String, Map<String, String>> newClassToFields) {
        // capture modified fields from the old version
        Map<String, Set<String>> modifiedFields = new HashMap<>();
        for (String className: oldClassToFields.keySet()) {
            if (!newClassToFields.containsKey(className)) {
                // The entire class is removed
                modifiedFields.put(className, new HashSet<>(oldClassToFields.get(className).keySet()));
                continue;
            }
            // fields are modified/removed
            Map<String, String> oldFields = oldClassToFields.get(className);
            Map<String, String> newFields = newClassToFields.get(className);
            for (String fieldName: oldFields.keySet()) {
                if (!newFields.containsKey(fieldName)) {
                    // removed field
                    addField(className, fieldName, modifiedFields);
                } else {
                    if (!oldFields.get(fieldName).equals(newFields.get(fieldName))) {
                        // modified field
                        addField(className, fieldName, modifiedFields);
                    }
                }
            }
        }
        return modifiedFields;
    }

    public static void main(String[] args) throws IOException {
        String targetSystem = "hdfs";
        String oldProjectRootDir;
        String newProjectRootDir;

        if (targetSystem.equals("cassandra")) {
            oldProjectRootDir = oldCassandraRootDir;
            newProjectRootDir = newCassandraRootDir;
            targetPrefixes.add("org.apache.cassandra");
        } else if (targetSystem.equals("hdfs")) {
            oldProjectRootDir = oldHdfsRootDir;
            newProjectRootDir = newHdfsRootDir;
            targetPrefixes.add("org.apache.hadoop.hdfs");
        } else {
            throw new RuntimeException("Cannot handle system " + targetSystem);
        }

        Map<String, Map<String, String>> oldClassToFields = extractFields(oldProjectRootDir, targetPrefixes);
        Map<String, Map<String, String>> newClassToFields = extractFields(newProjectRootDir, targetPrefixes);

        // calculate the modified fields
        Map<String, Set<String>> modifiedFields = captureModifiedFields(oldClassToFields, newClassToFields);

        // readNumericFields(outputPath);
        org.zlab.dinv.extractvars.Utils.saveModifiedFields(modifiedFields, Paths.get("output/modifiedFields.json"));

        for (String className: modifiedFields.keySet()) {
            for (String fieldName: modifiedFields.get(className)) {
                System.out.printf("field: %s.%s\n", className, fieldName);
                if (!newClassToFields.containsKey(className) || !newClassToFields.get(className).containsKey(fieldName)) {
                    System.out.println("\tremoved in new version");
                } else {
                    String oldType = oldClassToFields.get(className).get(fieldName);
                    String newType = newClassToFields.get(className).get(fieldName);
                    System.out.println("\told: " + oldType);
                    System.out.println("\tnew: " + newType);
                }
            }
        }
    }

}
