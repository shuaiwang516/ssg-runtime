package org.zlab.ocov;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public class Utils {

    public static ObjectMapper mapper = new ObjectMapper();

    // json: save map to a file
    public static void saveMapToFile(Map<String, Map<String, String>> map, String filename) {
        try {
            mapper.writeValue(new File(filename), map);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // json: load map from a file
    public static Map<String, Map<String, String>> loadMapFromFile(String filename) {
        try {
            return mapper.readValue(new File(filename), Map.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // json: save set to a file
    public static void saveSetToFile(Set<String> map, String filename) {
        try {
            mapper.writeValue(new File(filename), map);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    // json: load set from a file
    public static Set<String> loadSetFromFile(String filename) {
        try {
            return mapper.readValue(new File(filename), Set.class);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static void saveModifiedFields(Map<String, Set<String>> serializedFields,
            String filename) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.writeValue(new File(filename), serializedFields);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, Set<String>> loadModifiedFields(String filename) {
        // Read the map from the JSON file
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map<String, Set<String>> mapFromFile = objectMapper.readValue(new File(filename),
                    new TypeReference<Map<String, Set<String>>>() {
                    });
            return mapFromFile;
        } catch (IOException e) {
            System.err.println("Exception happen when loading output from " + new File(filename));
            throw new RuntimeException(e);
        }
    }

    public static boolean isPrimitiveType(String type) {
        // Also include int, integer, long, ....
        return type.equals("int") || type.equals("java.lang.Integer") || type.equals("long")
                || type.equals("java.lang.Long") || type.equals("double")
                || type.equals("java.lang.Double") || type.equals("float")
                || type.equals("java.lang.Float") || type.equals("boolean")
                || type.equals("java.lang.Boolean") || type.equals("char")
                || type.equals("java.lang.Character") || type.equals("short")
                || type.equals("java.lang.Short") || type.equals("byte")
                || type.equals("java.lang.Byte");
    }

}
