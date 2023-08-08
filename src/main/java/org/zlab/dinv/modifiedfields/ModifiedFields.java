package org.zlab.dinv.modifiedfields;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * This class is to iterate all the class definitions and extract the fields whose
 * declaration is modified between versions.
 */
@CommandLine.Command(name = "ModifiedFields", mixinStandardHelpOptions = true, version = "1.0",
        description = "ModifiedFields does amazing things.")
public class ModifiedFields implements Runnable {

    @CommandLine.Option(names = { "-infoPath" }, required = true, description = "path to files generated from vasco")
    private Path infoPath;

    @CommandLine.Option(names = { "-targetOldSystemPath" }, required = true, description = "path to old system")
    private Path targetOldSystemPath;

    @CommandLine.Option(names = { "-targetNewSystemPath" }, required = true, description = "path to new system")
    private Path targetNewSystemPath;

    @CommandLine.Option(names = { "-tp" }, split = ",", required = true, description = "target prefix for filtering")
    private List<String> targetPrefixes;

    @Override
    public void run() {
        try {
            Map<String, Map<String, String>> oldClassToFields = extractFields(targetOldSystemPath, targetPrefixes);
            Map<String, Map<String, String>> newClassToFields = extractFields(targetNewSystemPath, targetPrefixes);

            // calculate the modified fields
            Map<String, Set<String>> modifiedFields = captureModifiedFields(oldClassToFields, newClassToFields);

            // readNumericFields(outputPath);
            org.zlab.dinv.modifiedfields.Utils.saveModifiedFields(modifiedFields, infoPath.resolve("modifiedFields.json"));

            // print diff fields
            printDiffFields(modifiedFields, oldClassToFields, newClassToFields);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Simple static analysis to extract the numeric fields from a list of target classes.
     * The class should be given with full qualified.
     * input: (1) system source code (2) a list of classes
     * output: Map<ClassName, Map<Field, Type>>
     */
    public static Map<String, Map<String, String>> extractFields(
            Path projectRootDir, List<String> targetPrefixes) throws IOException {

        // remove $
        List<String> targetPrefixesNoDollar = new LinkedList<>();
        for (String classFullName: targetPrefixes) {
            targetPrefixesNoDollar.add(org.zlab.dinv.runtimechecker.Utils.replaceDollarWithDot(classFullName));
        }

        // Walk the project directory structure and find all the Java source files
        Map<String, Map<String, String>> classToFields = new HashMap<>();

        Files.walk(projectRootDir)
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
                                if (!org.zlab.dinv.modifiedfields.Utils.startWithTargetPrefix(targetPrefixesNoDollar, classFullName))
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
            boolean addedNewFields = false;
            // check added New fields
            for (String fieldName: newFields.keySet()) {
                if (!oldFields.containsKey(fieldName)) {
                    addedNewFields = true;
                    break;
                }
            }
            if (addedNewFields) {
                // If some fields are added, include all old fields: HBase-25238, proto mismatch
                for (String fieldName: oldFields.keySet())
                    addField(className, fieldName, modifiedFields);
            } else {
                // removed/type changed fields
                for (String fieldName: oldFields.keySet()) {
                    if (!newFields.containsKey(fieldName)) {
                        // removed field
                        addField(className, fieldName, modifiedFields);
                    } else {
                        if (!oldFields.get(fieldName).equals(newFields.get(fieldName))) {
                            // type changed modified field
                            addField(className, fieldName, modifiedFields);
                        }
                    }
                }
            }
        }
        return modifiedFields;
    }

    public void printDiffFields(Map<String, Set<String>> modifiedFields,
                                Map<String, Map<String, String>> oldClassToFields,
                                Map<String, Map<String, String>> newClassToFields) {

        for (String className : modifiedFields.keySet()) {
            for (String fieldName : modifiedFields.get(className)) {
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
