package org.zlab.dinv.extractvars;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Utils {

    public static boolean startWithTargetPrefix(List<String> targetPrefixes, String className) {
        if (targetPrefixes != null) {
            for (String targetClass: targetPrefixes) {
                if (className.startsWith(targetClass)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    public static void createOutputDirIfNotExist() {
        Path path = Paths.get("output");

        // If directory doesn't exist, create it
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create directory: " + e.getMessage());
            }
        }
    }

    public static void saveModifiedFields(Map<String, Set<String>> serializedFields, Path filePath) {
        createOutputDirIfNotExist();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.writeValue(filePath.toFile(), serializedFields);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, Set<String>> loadModifiedFields(Path filePath) {
        // Read the map from the JSON file
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map<String, Set<String>> mapFromFile = objectMapper.readValue(filePath.toFile(),
                    new TypeReference<Map<String, Set<String>>>() {});
            return mapFromFile;
        } catch (IOException e) {
            System.err.println("Exception happen when loading output from " + filePath);
            throw new RuntimeException(e);
        }
    }

    public static Map<String, Set<Integer>> replaceDollarWithDot(Map<String, Set<Integer>> fields) {
        Map<String, Set<Integer>> ret = new HashMap<>();
        for (Map.Entry<String, Set<Integer>> entry: fields.entrySet()) {
            if (entry.getKey().contains("$")) {
                ret.put(entry.getKey().replace("$", "."), entry.getValue());
            } else {
                ret.put(entry.getKey(), entry.getValue());
            }
        }
        return ret;
    }

}
