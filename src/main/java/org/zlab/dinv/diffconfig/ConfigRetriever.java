package org.zlab.dinv.diffconfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public abstract class ConfigRetriever {

    public static void saveConfigs(Set<String> configs, Path filePath) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.writeValue(filePath.toFile(), configs);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, String> removeClassInfo(Map<String, Map<String, String>> classToFieldsWith_TYPE_OR_INIT) {
        Map<String, String> ret = new HashMap<>();
        for (String className: classToFieldsWith_TYPE_OR_INIT.keySet()) {
            for (String configName: classToFieldsWith_TYPE_OR_INIT.get(className).keySet()) {
                ret.put(configName, classToFieldsWith_TYPE_OR_INIT.get(className).get(configName));
            }
        }
        return ret;
    }

    public static void saveConfigInfo(Map<String, String> configInfo, Path filePath) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.writeValue(filePath.toFile(), configInfo);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveEnumInfo(Map<String, List<String>> enumClass2Constants, Path filePath) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.writeValue(filePath.toFile(), enumClass2Constants);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static ConfigInfo extractConfigs(
            Path projectRootDir, List<String> targetClasses) throws IOException {
        // set up symbolSolver
        ReflectionTypeSolver typeSolver = new ReflectionTypeSolver();
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
        ParserConfiguration config = new ParserConfiguration().setSymbolResolver(symbolSolver);
        StaticJavaParser.setConfiguration(config);

        // remove $
        List<String> targetClassesNoDollar = new LinkedList<>();
        for (String classFullName: targetClasses) {
            targetClassesNoDollar.add(org.zlab.dinv.runtimechecker.Utils.replaceDollarWithDot(classFullName));
        }

        // Walk the project directory structure and find all the Java source files
        ConfigInfo configInfo = new ConfigInfo();

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
                                // Enum collector
                                Map<String, List<String>> enumCollector = new HashMap<>();
                                classDecl.accept(new EnumVisitor(), enumCollector);
                                configInfo.enumClass2Constants.putAll(enumCollector);

                                if (!targetClassesNoDollar.contains(classFullName))
                                    return;
                                System.out.println("process class: " + classFullName);
                                SingleClassConfigInfo singleClassConfigInfo = new SingleClassConfigInfo();
                                classDecl.accept(new ConfigVisitor(), singleClassConfigInfo);

                                configInfo.classToFieldsWithType.put(classFullName, singleClassConfigInfo.typeCollector);
                                configInfo.classToFieldsWithInit.put(classFullName, singleClassConfigInfo.initCollector);
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
        return configInfo;
    }
}
