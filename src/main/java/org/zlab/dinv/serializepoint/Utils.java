package org.zlab.dinv.serializepoint;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

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

    public static String logSerializePointStmt(String pName, String cName,
            SerializePoint.PrintableType type, boolean isStatic) {
        if (InstSerializePoint.DEBUG)
            return wrapWithSysPrintln(logSerializePointFieldRef(pName, cName, type, isStatic));
        else
            return wrapWithLogger(logSerializePointFieldRef(pName, cName, type, isStatic));
    }

    public static String wrapWithSysPrintln(String input) {
        // for test
        return String.format("System.out.println(%s);", input);
    }

    public static String wrapWithLogger(String input) {
        // for real system
        return String.format("seriailze_logger(%s);", input);
    }

    public static String logSerializePointFieldRef(String pName, String cName,
            SerializePoint.PrintableType type, boolean isStatic) {
        // If it's static, do not output class Hash and getClass
        // according to whether it's printable, we need to use different format
        String format = null;
        if (type != null) {
            switch (type) {
                case int_ :
                case byte_ :
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
                        pName, format, cName, type, pName, cName);
            } else {
                return String.format(
                        "String.format(\"[hklog] thread ID = %%d, pHash = %%d, pName = %s, pClass = %%s, cVal = %s, cName = %s, cClass = %s\",\n"
                                + "                Thread.currentThread().getId(),\n"
                                + "                System.identityHashCode(%s), %s.getClass(),\n"
                                + "                %s)",
                        pName, format, cName, type, pName, pName, cName);
            }

        } else {
            if (isStatic) {
                return String.format(
                        "String.format(\"[hklog] thread ID = %%d, pHash = NA, pName = %s, pClass = %%s, cHash = %%d, cName = %s, cClass = %%s\",\n"
                                + "                Thread.currentThread().getId(),\n"
                                + "                \"%s\",\n"
                                + "                System.identityHashCode(%s), %s.getClass())",
                        pName, cName, pName, cName, cName);
            } else {
                return String.format(
                        "String.format(\"[hklog] thread ID = %%d, pHash = %%d, pName = %s, pClass = %%s, cHash = %%d, cName = %s, cClass = %%s\",\n"
                                + "                Thread.currentThread().getId(),\n"
                                + "                System.identityHashCode(%s), %s.getClass(),\n"
                                + "                System.identityHashCode(%s), %s.getClass())",
                        pName, cName, pName, pName, cName, cName);
            }
        }
    }

    public static String logSerializePointStmt() {
        return "System.out.println(\"[Dinv] \" + Thread.currentThread().getName() + \" \" + System.currentTimeMillis());";
    }

}
