package org.zlab.ocov;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class Utils {

    public static ObjectMapper mapper = new ObjectMapper();
    public static Random rand = new Random();

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

    public static void saveBranch2Collection(Map<Integer, Set<Integer>> programLocations,
            Path filePath) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.writeValue(filePath.toFile(), programLocations);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<Integer, Set<Integer>> loadBranch2Collection(Path filePath) {
        // Read the map from the JSON file
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map<Integer, Set<Integer>> mapFromFile = objectMapper.readValue(filePath.toFile(),
                    new TypeReference<Map<Integer, Set<Integer>>>() {
                    });
            return mapFromFile;
        } catch (IOException e) {
            System.err.println("Exception happen when loading output from " + filePath);
            throw new RuntimeException(e);
        }
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

    public static List<Integer> sampleIdxFromSize(int size, int sampleSize) {
        List<Integer> idxs = new java.util.ArrayList<>();
        int minSize = Math.min(size, sampleSize);

        int count = 0;
        while (idxs.size() < minSize) {
            int idx = rand.nextInt(minSize);
            idxs.add(idx);
            count++;
            if (count > 2 * minSize) { // avoid infinite loop
                break;
            }
        }
        return idxs;
    }

    public static boolean computeBranchResult(Object lhsOp, Object rhsOp, String operator) {
        // Directly compute the branch status
        boolean status = false;
        if (lhsOp instanceof Integer && rhsOp instanceof Integer) {
            int lhs = (Integer) lhsOp;
            int rhs = (Integer) rhsOp;
            switch (operator) {
                case "<" :
                    status = lhs < rhs;
                    break;
                case "<=" :
                    status = lhs <= rhs;
                    break;
                case ">" :
                    status = lhs > rhs;
                    break;
                case ">=" :
                    status = lhs >= rhs;
                    break;
                default :
                    throw new RuntimeException("Unsupported operator: " + operator);
            }
        }
        return status;
    }

}
