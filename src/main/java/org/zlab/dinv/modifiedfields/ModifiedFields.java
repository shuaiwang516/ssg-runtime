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
 * This class is to iterate all the class definitions and extract the fields
 * whose declaration is modified between versions.
 */
@CommandLine.Command(name = "ModifiedFields", mixinStandardHelpOptions = true, version = "1.0", description = "ModifiedFields does amazing things.")
public class ModifiedFields implements Runnable {

    @CommandLine.Option(names = {
            "-infoPath"}, required = true, description = "path to files generated from vasco")
    private Path infoPath;

    @CommandLine.Option(names = {
            "-targetOldSystemPath"}, required = true, description = "path to old system")
    private Path targetOldSystemPath;

    @CommandLine.Option(names = {
            "-targetNewSystemPath"}, required = true, description = "path to new system")
    private Path targetNewSystemPath;

    @CommandLine.Option(names = {
            "-tp"}, split = ",", required = true, description = "target prefix for filtering")
    private List<String> targetPrefixes;

    @Override
    public void run() {
        try {
            FieldEnumInfo oldFieldEnumInfo = extractFields(targetOldSystemPath, targetPrefixes);
            FieldEnumInfo newFieldEnumInfo = extractFields(targetNewSystemPath, targetPrefixes);

            // calculate the modified fields
            Map<String, Set<String>> modifiedFields = captureModifiedFields(
                    oldFieldEnumInfo.fieldInfo, newFieldEnumInfo.fieldInfo);
            Set<String> modifiedEnums = captureModifiedEnum(oldFieldEnumInfo.enumInfo,
                    newFieldEnumInfo.enumInfo);

            System.out.println("modified enums: " + modifiedEnums);

            // readNumericFields(outputPath);
            org.zlab.dinv.modifiedfields.Utils.saveModifiedFields(modifiedFields,
                    infoPath.resolve("modifiedFields.json"));
            org.zlab.dinv.modifiedfields.Utils.saveModifiedEnums(modifiedEnums,
                    infoPath.resolve("modifiedEnums.json"));

            // print diff fields
            printDiffFields(modifiedFields, oldFieldEnumInfo.fieldInfo, newFieldEnumInfo.fieldInfo);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class FieldEnumInfo {
        public Map<String, Map<String, String>> fieldInfo = new HashMap<>();
        public Map<String, List<String>> enumInfo = new HashMap<>();
    }

    /**
     * Simple static analysis to extract the numeric fields from a list of target
     * classes. The class should be given with full qualified. input: (1) system
     * source code (2) a list of classes output: Map<ClassName, Map<Field, Type>>
     */
    public static FieldEnumInfo extractFields(Path projectRootDir, List<String> targetPrefixes)
            throws IOException {

        // remove $
        List<String> targetPrefixesNoDollar = new LinkedList<>();
        for (String classFullName : targetPrefixes) {
            targetPrefixesNoDollar
                    .add(org.zlab.dinv.runtimechecker.Utils.replaceDollarWithDot(classFullName));
        }

        FieldEnumInfo fieldEnumInfo = new FieldEnumInfo();
        // Walk the project directory structure and find all the Java source files

        Files.walk(projectRootDir).filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                    try {
                        // debug
                        // if (!p.toString().contains("/FSEditLogAsync.java")) return;
                        if (org.zlab.dinv.Utils.exclude(p))
                            return;
                        System.out.println("processing file " + p);
                        CompilationUnit cu = StaticJavaParser.parse(p.toFile());
                        // field info
                        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
                            if (classDecl.getFullyQualifiedName().isPresent()) {
                                String classFullName = classDecl.getFullyQualifiedName().get();
                                if (!org.zlab.dinv.modifiedfields.Utils.startWithTargetPrefix(
                                        targetPrefixesNoDollar, classFullName))
                                    return;
                                System.out.println("process class: " + classFullName);
                                Map<String, String> fields = new HashMap<>();
                                classDecl.accept(new FieldVisitor(), fields);
                                fieldEnumInfo.fieldInfo.put(classFullName, fields);
                            } else {
                                System.out.println(
                                        "class " + classDecl.getName() + " full name is null");
                            }
                        });
                        // enum info
                        cu.accept(new EnumVisitor(), fieldEnumInfo.enumInfo);
                        // Files.write(p, cu.toString().getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        // for (String clazz: classToFields.keySet()) {
        // System.out.println("class: " + clazz);
        // for (String field: classToFields.get(clazz).keySet()) {
        // System.out.printf("\t\tfield = %s, type = %s\n", field,
        // classToFields.get(clazz).get(field));
        // }
        // }
        return fieldEnumInfo;
    }

    public static void addField(String className, String fieldName,
            Map<String, Set<String>> modifiedFields) {
        if (!modifiedFields.containsKey(className)) {
            modifiedFields.put(className, new HashSet<>());
        }
        modifiedFields.get(className).add(fieldName);
    }

    public static void printModifiedFields(Map<String, Set<String>> modifiedFields) {
        for (String clazz : modifiedFields.keySet()) {
            System.out.println("class: " + clazz);
            for (String field : modifiedFields.get(clazz)) {
                System.out.printf("\t\tfield = %s\n", field);
            }
        }
    }

    public static Map<String, Set<String>> captureModifiedFields(
            Map<String, Map<String, String>> oldClassToFields,
            Map<String, Map<String, String>> newClassToFields) {
        // capture modified fields from the old version
        Map<String, Set<String>> modifiedFields = new HashMap<>();
        for (String className : oldClassToFields.keySet()) {
            if (!newClassToFields.containsKey(className)) {
                // The entire class is removed
                modifiedFields.put(className,
                        new HashSet<>(oldClassToFields.get(className).keySet()));
                continue;
            }

            // fields are modified/removed
            Map<String, String> oldFields = oldClassToFields.get(className);
            Map<String, String> newFields = newClassToFields.get(className);
            boolean addedNewFields = false;
            // check added New fields
            for (String fieldName : newFields.keySet()) {
                if (!oldFields.containsKey(fieldName)) {
                    addedNewFields = true;
                    break;
                }
            }
            if (addedNewFields) {
                // If some fields are added, include all old fields: HBase-25238, proto mismatch
                for (String fieldName : oldFields.keySet())
                    addField(className, fieldName, modifiedFields);
            } else {
                // removed/type changed fields
                for (String fieldName : oldFields.keySet()) {
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

    public Set<String> captureModifiedEnum(Map<String, List<String>> oldEnumInfo,
            Map<String, List<String>> newEnumInfo) {
        Set<String> modifiedEnums = new HashSet<>();
        // Removed Enum
        for (String enumClassFullName : oldEnumInfo.keySet()) {
            boolean modified = false;
            if (!newEnumInfo.containsKey(enumClassFullName))
                modified = true;
            else {
                // modified Enum?
                List<String> oldConstants = oldEnumInfo.get(enumClassFullName);
                List<String> newConstants = newEnumInfo.get(enumClassFullName);
                if (oldConstants.size() != newConstants.size())
                    modified = true;
                else {
                    for (int i = 0; i < oldConstants.size(); i++) {
                        if (!oldConstants.get(i).equals(newConstants.get(i))) {
                            modified = true;
                            break;
                        }
                    }
                }
            }
            if (modified)
                modifiedEnums.add(enumClassFullName);
        }
        return modifiedEnums;
    }

    public void printDiffFields(Map<String, Set<String>> modifiedFields,
            Map<String, Map<String, String>> oldClassToFields,
            Map<String, Map<String, String>> newClassToFields) {

        for (String className : modifiedFields.keySet()) {
            for (String fieldName : modifiedFields.get(className)) {
                System.out.printf("field: %s.%s\n", className, fieldName);
                if (!newClassToFields.containsKey(className)
                        || !newClassToFields.get(className).containsKey(fieldName)) {
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
