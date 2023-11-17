package org.zlab.dinv.serializepoint;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    public static String logSerializePointStmt(String pName, String cName,
            SerializePoint.PrintableType type, boolean isStatic) {
        return wrapWithSerializationVariableCheck(createLogStatement(pName, cName, type, isStatic));
    }

    public static String createLogStatement(String pName, String cName,
            SerializePoint.PrintableType type, boolean isStatic) {
        // wrap with try/catch:
        // org.zlab.dinv.runtimechecker.Utils.wrapWithTryCatch(java.lang.String,
        // java.lang.String)

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
        String clazz = "org.apache.cassandra.service.CassandraDaemon";
        return String.format("if (%s.isSerializationInProgress) {\n" + "    %s\n" + "}", clazz,
                input);
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

    public static String logSerializePointFieldRef(String pName, String cName,
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

    public static String logSerializePointStmt() {
        return "System.out.println(\"[Dinv] \" + Thread.currentThread().getName() + \" \" + System.currentTimeMillis());";
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
}
