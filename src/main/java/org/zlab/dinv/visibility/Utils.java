package org.zlab.dinv.visibility;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
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
                    new TypeReference<Map<String, Set<Integer>>>() {
                    });
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
            Map<String, Map<String, Set<String>>> mapFromFile = objectMapper.readValue(
                    filePath.toFile(), new TypeReference<Map<String, Map<String, Set<String>>>>() {
                    });
            return mapFromFile;
        } catch (IOException e) {
            System.err.println("Exception happen when loading output from " + filePath);
            throw new RuntimeException(e);
        }
    }

    public static void PPT2DaikonInput(Map<String, Map<String, Set<String>>> pptVars,
            Path daikonInputVarPath) {
        // read json file
        try {
            // Create an instance of BufferedWriter
            BufferedWriter writer = Files.newBufferedWriter(daikonInputVarPath);

            // Write each entry of pptVars to a separate line
            for (Map.Entry<String, Map<String, Set<String>>> entry : pptVars.entrySet()) {
                // construct method sig
                String clazzName = entry.getKey();
                for (String methodName : entry.getValue().keySet()) {
                    String methodSigDaikon = String.format("%s.%s", clazzName, methodName);
                    for (String fieldName : entry.getValue().get(methodName)) {
                        writer.write(methodSigDaikon + " " + fieldName);
                        writer.newLine();
                    }
                }
            }
            writer.close();
        } catch (IOException e) {
            // Handle any exceptions
            System.out.println("An error occurred while writing to the file.");
            e.printStackTrace();
        }
    }

    public static void mergeProgramLocations(Map<String, Set<Integer>> programLocations,
            Map<String, Set<Integer>> addedProgramLocations) {
        for (String clazzName : addedProgramLocations.keySet()) {
            Set<Integer> fields = addedProgramLocations.get(clazzName);
            if (programLocations.containsKey(clazzName)) {
                programLocations.get(clazzName).addAll(fields);
            } else {
                programLocations.put(clazzName, new HashSet<>(fields));
            }
        }
    }

}
