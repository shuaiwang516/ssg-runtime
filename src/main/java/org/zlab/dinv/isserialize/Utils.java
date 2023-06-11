package org.zlab.dinv.isserialize;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Utils {

    public static Map<String, Set<Integer>> loadProgramLocations(Path filePath) {
        // Read the map from the JSON file
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map<String, Set<Integer>> mapFromFile = objectMapper.readValue(filePath.toFile(),
                    new TypeReference<Map<String, Set<Integer>>>() {});
            return mapFromFile;
        } catch (IOException e) {
            System.err.println("Exception happen when loading output from " + filePath);
            throw new RuntimeException(e);
        }
    }

    public static void recordStaticPptVar(Map<String, Map<String, Set<String>>> pptVars,
                                          String classFullName, String methodName, String fieldName) {
        fieldName = classFullName + "." + fieldName;
        recordPptVar(pptVars, classFullName, methodName, fieldName);
    }

    public static void recordNonStaticPptVar(Map<String, Map<String, Set<String>>> pptVars,
                                          String classFullName, String methodName, String fieldName) {
        // field name should contain "this" by default
        recordPptVar(pptVars, classFullName, methodName, fieldName);
    }

    public static void recordPptVar(Map<String, Map<String, Set<String>>> pptVars,
                                          String classFullName, String methodName, String fieldName) {
        if (!pptVars.containsKey(classFullName)) {
            pptVars.put(classFullName, new HashMap<>());
        }
        if (!pptVars.get(classFullName).containsKey(methodName)) {
            pptVars.get(classFullName).put(methodName, new HashSet<>());
        }
        pptVars.get(classFullName).get(methodName).add(fieldName);
    }

    public static void savePptVars(Map<String, Map<String, Set<String>>> pptVars, Path filePath) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.writeValue(filePath.toFile(), pptVars);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, Map<String, Set<String>>> loadPptVars(Path filePath) {
        // Read the map from the JSON file
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map<String, Map<String, Set<String>>> mapFromFile = objectMapper.readValue(filePath.toFile(),
                    new TypeReference<Map<String, Map<String, Set<String>>>>() {});
            return mapFromFile;
        } catch (IOException e) {
            System.err.println("Exception happen when loading output from " + filePath);
            throw new RuntimeException(e);
        }
    }

}
