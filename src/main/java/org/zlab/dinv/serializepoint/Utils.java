package org.zlab.dinv.serializepoint;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static org.zlab.dinv.modifiedfields.Utils.createOutputDirIfNotExist;

public class Utils {

    public static void saveSerializePoints(Set<SerializePoint> serializePoints, Path filePath) {
        createOutputDirIfNotExist();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.writeValue(filePath.toFile(), serializePoints);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Set<SerializePoint> loadSerializePoints(Path filePath) {
        // Read the map from the JSON file
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Set<SerializePoint> mapFromFile = objectMapper.readValue(filePath.toFile(),
                    new TypeReference<Set<SerializePoint>>() {
                    });
            return mapFromFile;
        } catch (IOException e) {
            System.err.println("Exception happen when loading output from " + filePath);
            throw new RuntimeException(e);
        }
    }

    public static void saveWritePoints(Set<WritePoint> writePoints, Path filePath) {
        createOutputDirIfNotExist();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.writeValue(filePath.toFile(), writePoints);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Set<WritePoint> loadWritePoints(Path filePath) {
        // Read the map from the JSON file
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Set<WritePoint> mapFromFile = objectMapper.readValue(filePath.toFile(),
                    new TypeReference<Set<WritePoint>>() {
                    });
            return mapFromFile;
        } catch (IOException e) {
            System.err.println("Exception happen when loading output from " + filePath);
            throw new RuntimeException(e);
        }
    }

    public static boolean isPrimitive(SerializePoint.PrintableType type) {
        return type == SerializePoint.PrintableType.byte_
                || type == SerializePoint.PrintableType.int_
                || type == SerializePoint.PrintableType.long_
                || type == SerializePoint.PrintableType.float_
                || type == SerializePoint.PrintableType.double_
                || type == SerializePoint.PrintableType.char_
                || type == SerializePoint.PrintableType.boolean_;
    }

    public static String logWritePointStmt(String varName, SerializePoint.PrintableType type) {
        String format = PrintableType2Format(type);
        String s;
        if (type == null)
            s = String.format("String.format(\"write point: type = null, value = %%s\", %s)",
                    varName + ".toString()");
        else
            s = String.format("String.format(\"write point: type = %s, value = %s\", %s)", type,
                    format, varName);
        // String tmp = wrapWithSysPrintln(s);
        String tmp = org.zlab.dinv.runtimechecker.Utils.wrapWithTryCatch(wrapWithLogger(s));
        return tmp;
    }

    public static String logSerializePointStmt(String pName, String cName,
            SerializePoint.PrintableType type, boolean isStatic) {
        if (RewriteExec.useJson)
            return wrapWithSerializationVariableCheck(
                    createJSONLogStatement(pName, cName, type, isStatic));
        else
            return wrapWithSerializationVariableCheck(
                    createLogStatement(pName, cName, type, isStatic));
    }

    public static String createJSONLogStatement(String pName, String cName,
            SerializePoint.PrintableType type, boolean isStatic) {
        if (isStatic) {
            return String.format(
                    "serialize_logger.info(org.zlab.dinv.logger.LogEntry.constructLogEntry(%s.class, %s, \"%s\").toJsonString());",
                    pName, cName, cName);
        } else {
            return String.format(
                    "serialize_logger.info(org.zlab.dinv.logger.LogEntry.constructLogEntry(%s, %s, \"%s\").toJsonString());",
                    pName, cName, cName);
        }
    }

    public static String createLogStatement(String pName, String cName,
            SerializePoint.PrintableType type, boolean isStatic) {
        // wrap with null check for pName and cName
        String inputFieldNotNull, inputFieldNull;
        if (InstSerializePoint.USE_PRINT) {
            inputFieldNotNull = wrapWithSysPrintln(
                    logSerializePointFieldRef(pName, cName, type, isStatic));
            inputFieldNull = wrapWithSysPrintln(
                    logSerializePointFieldRefNullfield(pName, cName, type, isStatic));
        } else {
            inputFieldNotNull = wrapWithLogger(
                    logSerializePointFieldRef(pName, cName, type, isStatic));
            inputFieldNull = wrapWithLogger(
                    logSerializePointFieldRefNullfield(pName, cName, type, isStatic));
        }
        return wrapWithNullCheck(pName, cName, type, isStatic, inputFieldNotNull, inputFieldNull);
    }

    public static String wrapWithSerializationVariableCheck(String input) {
        // TODO: Add in configuration
        String loggerMonitorClass = "org.zlab.dinv.logger.SerializeMonitor";
        return String.format("if (%s.isSerializing) {\n" + " %s\n" + "}", loggerMonitorClass,
                input);
        // String clazz = "org.apache.cassandra.service.CassandraDaemon";
        // return String.format("if (%s.isSerializationInProgress) {\n" + " %s\n" + "}",
        // clazz,
        // input);
    }

    public static String wrapWithNullCheck(String pName, String cName,
            SerializePoint.PrintableType type, boolean isStatic, String inputFieldNotNull,
            String inputFieldNull) {
        if (isStatic) {
            // do not need to check parent, only check child is fine
            if (!isPrimitive(type))
                return String.format(
                        "        if (%s == null) {\n" + "            %s\n" + "        } else {\n"
                                + "            %s\n" + "        }",
                        cName, inputFieldNull, inputFieldNotNull);
            else
                return inputFieldNotNull;
        } else {
            if (!isPrimitive(type))
                return String.format(
                        "        if (%s != null) {\n" + "            if (%s == null) {\n"
                                + "                %s\n" + "            } else {\n"
                                + "                %s\n" + "            }\n" + "        }",
                        pName, cName, inputFieldNull, inputFieldNotNull);
            else
                return String.format(
                        "        if (%s != null) {\n" + "            %s\n" + "        }", pName,
                        inputFieldNotNull);
        }
    }

    public static String wrapWithSysPrintln(String input) {
        // for test
        return String.format("System.out.println(%s);", input);
    }

    public static String wrapWithLogger(String input) {
        // for real system
        return String.format("serialize_logger.info(%s);", input);
    }

    public static String PrintableType2Format(SerializePoint.PrintableType type) {
        String format = null;
        if (type != null) {
            switch (type) {
                case byte_ :
                case int_ :
                case long_ :
                    format = "%d";
                    break;
                case float_ :
                case double_ :
                    format = "%f";
                    break;
                case char_ :
                    format = "%c";
                    break;
                case boolean_ :
                    format = "%b";
                    break;
                case string_ :
                case enum_ :
                    format = "%s";
                    break;
            }
        }
        return format;
    }

    public static String logSerializePointFieldRef(String pName, String cName,
            SerializePoint.PrintableType type, boolean isStatic) {
        // If it's static, do not output class Hash and getClass
        // according to whether it's printable, we need to use different format
        if (type != null) {
            String format = PrintableType2Format(type);
            if (isStatic) {
                return String.format(
                        "String.format(\"[hklog] thread ID = %%d, pHash = NA, pName = %s, pClass = %%s, cVal = %s, cName = %s, cClass = %s\",\n"
                                + "                Thread.currentThread().getId(),\n"
                                + "                \"%s\",\n" + "                %s)",
                        replaceDoubleQuotesWithSingleQuote(pName), format, cName, type, pName,
                        cName);
            } else {
                return String.format(
                        "String.format(\"[hklog] thread ID = %%d, pHash = %%d, pName = %s, pClass = %%s, cVal = %s, cName = %s, cClass = %s\",\n"
                                + "                Thread.currentThread().getId(),\n"
                                + "                System.identityHashCode(%s), %s.getClass(),\n"
                                + "                %s)",
                        replaceDoubleQuotesWithSingleQuote(pName), format, cName, type, pName,
                        pName, cName);
            }

        } else {
            if (isStatic) {
                return String.format(
                        "String.format(\"[hklog] thread ID = %%d, pHash = NA, pName = %s, pClass = %%s, cHash = %%d, cName = %s, cClass = %%s\",\n"
                                + "                Thread.currentThread().getId(),\n"
                                + "                \"%s\",\n"
                                + "                System.identityHashCode(%s), %s.getClass())",
                        replaceDoubleQuotesWithSingleQuote(pName), cName, pName, cName, cName);
            } else {
                return String.format(
                        "String.format(\"[hklog] thread ID = %%d, pHash = %%d, pName = %s, pClass = %%s, cHash = %%d, cName = %s, cClass = %%s\",\n"
                                + "                Thread.currentThread().getId(),\n"
                                + "                System.identityHashCode(%s), %s.getClass(),\n"
                                + "                System.identityHashCode(%s), %s.getClass())",
                        replaceDoubleQuotesWithSingleQuote(pName), cName, pName, pName, cName,
                        cName);
            }
        }
    }

    public static String logSerializePointFieldRefNullfield(String pName, String cName,
            SerializePoint.PrintableType type, boolean isStatic) {
        // If it's static, do not output class Hash and getClass
        // according to whether it's printable, we need to use different format
        String format = null;
        if (type != null) {
            switch (type) {
                case byte_ :
                case int_ :
                case long_ :
                    format = "%d";
                    break;
                case float_ :
                case double_ :
                    format = "%f";
                    break;
                case char_ :
                    format = "%c";
                    break;
                case boolean_ :
                    format = "%b";
                    break;
                case string_ :
                case enum_ :
                    format = "%s";
                    break;
            }
            if (isStatic) {
                return String.format(
                        "String.format(\"[hklog] thread ID = %%d, pHash = NA, pName = %s, pClass = %%s, cVal = null, cName = %s, cClass = %s\",\n"
                                + "                Thread.currentThread().getId(),\n"
                                + "                \"%s\",\n" + "                %s)",
                        replaceDoubleQuotesWithSingleQuote(pName), cName, type, pName, cName);
            } else {
                return String.format(
                        "String.format(\"[hklog] thread ID = %%d, pHash = %%d, pName = %s, pClass = %%s, cVal = null, cName = %s, cClass = %s\",\n"
                                + "                Thread.currentThread().getId(),\n"
                                + "                System.identityHashCode(%s), %s.getClass(),\n"
                                + "                %s)",
                        replaceDoubleQuotesWithSingleQuote(pName), cName, type, pName, pName,
                        cName);
            }
        } else {
            if (isStatic) {
                return String.format(
                        "String.format(\"[hklog] thread ID = %%d, pHash = NA, pName = %s, pClass = %%s, cHash = null, cName = %s, cClass = null\",\n"
                                + "                Thread.currentThread().getId(),\n"
                                + "                \"%s\"\n" + "                )",
                        replaceDoubleQuotesWithSingleQuote(pName), cName, pName);
            } else {
                return String.format(
                        "String.format(\"[hklog] thread ID = %%d, pHash = %%d, pName = %s, pClass = %%s, cHash = null, cName = %s, cClass = null\",\n"
                                + "                Thread.currentThread().getId(),\n"
                                + "                System.identityHashCode(%s), %s.getClass()\n"
                                + "                )",
                        replaceDoubleQuotesWithSingleQuote(pName), cName, pName, pName);
            }
        }
    }

    public static boolean checkIfParentNameExists(Statement stmt, String parentName,
            boolean isStatic) {
        // iterate all child nodes, find FieldAccessExpr, check if the name is the same
        // as parentName
        if (isStatic || parentName.equals("this"))
            return true;
        return stmt.toString().contains(parentName + ".");
    }

    public static Map<String, Map<Integer, Set<SerializePoint>>> getSerializePointsMap(
            Set<SerializePoint> serializePoints) {
        Map<String, Map<Integer, Set<SerializePoint>>> serializePointsMap = new HashMap<>();
        for (SerializePoint serializePoint : serializePoints) {
            if (!serializePointsMap.containsKey(serializePoint.className)) {
                serializePointsMap.put(serializePoint.className, new HashMap<>());
            }
            Map<Integer, Set<SerializePoint>> lineMap = serializePointsMap
                    .get(serializePoint.className);
            if (!lineMap.containsKey(serializePoint.lineNumber)) {
                lineMap.put(serializePoint.lineNumber, new HashSet<>());
            }
            Set<SerializePoint> serializePointSet = lineMap.get(serializePoint.lineNumber);
            serializePointSet.add(serializePoint);
        }
        return serializePointsMap;
    }

    public static Map<String, Map<Integer, Set<WritePoint>>> getWritePointsMap(
            Set<WritePoint> serializePoints) {
        Map<String, Map<Integer, Set<WritePoint>>> writePointsMap = new HashMap<>();
        for (WritePoint writePoint : serializePoints) {
            if (!writePointsMap.containsKey(writePoint.className)) {
                writePointsMap.put(writePoint.className, new HashMap<>());
            }
            Map<Integer, Set<WritePoint>> lineMap = writePointsMap.get(writePoint.className);
            if (!lineMap.containsKey(writePoint.lineNumber)) {
                lineMap.put(writePoint.lineNumber, new HashSet<>());
            }
            Set<WritePoint> serializePointSet = lineMap.get(writePoint.lineNumber);
            serializePointSet.add(writePoint);
        }
        return writePointsMap;
    }

    public static String replaceDoubleQuotesWithSingleQuote(String input) {// replace \" with '
        return input.replace("\"", "'");
    }

    public static SerializePoint.PrintableType type2Printable(String type) {
        switch (type) {
            case "byte" :
                return SerializePoint.PrintableType.byte_;
            case "int" :
                return SerializePoint.PrintableType.int_;
            case "long" :
                return SerializePoint.PrintableType.long_;
            case "float" :
                return SerializePoint.PrintableType.float_;
            case "double" :
                return SerializePoint.PrintableType.double_;
            case "char" :
                return SerializePoint.PrintableType.char_;
            case "boolean" :
                return SerializePoint.PrintableType.boolean_;
            case "String" :
                return SerializePoint.PrintableType.string_;
            case "enum" :
                return SerializePoint.PrintableType.enum_;
            default :
                return null;
        }
    }

    public static void injectLogger(CompilationUnit cu) {
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
            if (!classDecl.getFullyQualifiedName().isPresent()) {
                return;
            }
            // check whether this class is an inner class since we only need to inject
            // logger at the top level
            if (classDecl.isNestedType()) {
                return;
            }
            // check whether this class has already injected logger
            for (FieldDeclaration fieldDeclaration : classDecl.getFields()) {
                if (fieldDeclaration.getVariables().get(0).getNameAsString()
                        .equals("serialize_logger")) {
                    return;
                }
            }
            // add logger declaration
            // String loggerDecl = "private static final org.slf4j.Logger serialize_logger =
            // org.slf4j.LoggerFactory.getLogger(\"serialize.logger\");";
            String type = "org.slf4j.Logger";
            String loggerName = "serialize_logger";
            String loggerInitExpr = "org.slf4j.LoggerFactory.getLogger(\"serialize.logger\");";
            // Transform loggerAssignExpr to Expression
            ExpressionStmt stmt = (ExpressionStmt) StaticJavaParser.parseStatement(loggerInitExpr);

            FieldDeclaration fieldDeclaration;
            if (classDecl.isInterface())
                fieldDeclaration = classDecl.addFieldWithInitializer(type, loggerName,
                        stmt.getExpression(), Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);
            else
                fieldDeclaration = classDecl.addFieldWithInitializer(type, loggerName,
                        stmt.getExpression(), Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC,
                        Modifier.Keyword.FINAL);
            // Move it to the front position
            classDecl.getMembers().remove(fieldDeclaration);
            classDecl.getMembers().addFirst(fieldDeclaration);
        });
    }
}
