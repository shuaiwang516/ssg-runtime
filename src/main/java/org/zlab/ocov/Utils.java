package org.zlab.ocov;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class Utils {

    public static ObjectMapper mapper = new ObjectMapper();
    public static Random rand = new Random();

    // json: save map to a file
    public static void saveMapToFile(Map<String, Map<String, String>> map, Path filePath) {
        try {
            mapper.writeValue(filePath.toFile(), map);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // json: load map from a file
    public static Map<String, Map<String, String>> loadMapFromFile(Path filePath) {
        try {
            return mapper.readValue(filePath.toFile(), Map.class);
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
    public static void saveSetToFile(Set<String> map, Path filePath) {
        try {
            mapper.writeValue(filePath.toFile(), map);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    // json: load set from a file
    public static Set<String> loadSetFromFile(Path filePath) {
        try {
            return mapper.readValue(filePath.toFile(), Set.class);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    // json: save set to a file
    public static void saveIntSetToFile(Set<Integer> map, Path filePath) {
        try {
            mapper.writeValue(filePath.toFile(), map);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    // json: load set from a file
    public static Set<Integer> loadIntSetFromFile(Path filePath) {
        try {
            return mapper.readValue(filePath.toFile(), Set.class);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static void saveModifiedFields(Map<String, Set<String>> serializedFields,
            Path filePath) {
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
                    new TypeReference<Map<String, Set<String>>>() {
                    });
            return mapFromFile;
        } catch (IOException e) {
            System.err.println("Exception happen when loading output from " + filePath);
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

    public static boolean isStringType(String type) {
        return type.equals("java.lang.String");
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

    public static boolean computeBinaryComparison(long lhs, long rhs, String operator) {
        boolean status;
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
        return status;
    }

    public static long toLong(Object obj) {
        if (obj instanceof Integer) {
            return (Integer) obj;
        } else if (obj instanceof Long) {
            return (Long) obj;
        } else {
            throw new RuntimeException(
                    "Unsupported type for boundary: " + obj.getClass().getName());
        }
    }

    public static String getStackTrace() {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement stackTraceElement : stackTraceElements) {
            sb.append(stackTraceElement);
            sb.append("\n");
        }
        return sb.toString();
    }

    public static Map<String, Map<String, String>> replaceDollar(
            Map<String, Map<String, String>> map) {
        // ClassName -> <fieldname, type>
        // replace doller with dot for both classname and type
        Map<String, Map<String, String>> newMap = new java.util.HashMap<>();
        for (Map.Entry<String, Map<String, String>> entry : map.entrySet()) {
            String className = entry.getKey().replace('$', '.');
            Map<String, String> fields = new java.util.HashMap<>();
            for (Map.Entry<String, String> field : entry.getValue().entrySet()) {
                String fieldName = field.getKey();
                String type = field.getValue().replace('$', '.');
                fields.put(fieldName, type);
            }
            newMap.put(className, fields);
        }
        return newMap;
    }

    public static Set<String> replaceDollarWithDot(Set<String> set) {
        Set<String> newSet = new java.util.HashSet<>();
        for (String str : set) {
            newSet.add(str.replace('$', '.'));
        }
        return newSet;
    }

    public static List<String> mapStackTraceToSymbol(String stacktrace) {
        List<String> symbols = new java.util.ArrayList<>();
        String[] lines = stacktrace.split("\n");
        for (String line : lines) {
            // compute hash
            String symbol = String.valueOf(line.hashCode());
            symbols.add(symbol);
        }
        return symbols;
    }

    public static int computeEditDistance(List<String> list1, List<String> list2) {
        int m = list1.size();
        int n = list2.size();

        // Create a 2D array to store distances
        int[][] dp = new int[m + 1][n + 1];

        // Initialize the first row and column to represent incremental insertions and
        // deletions
        for (int i = 0; i <= m; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= n; j++) {
            dp[0][j] = j;
        }

        // Fill the dp array
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (list1.get(i - 1).equals(list2.get(j - 1))) {
                    dp[i][j] = dp[i - 1][j - 1]; // No operation needed
                } else {
                    dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, // Deletion
                            dp[i][j - 1] + 1), // Insertion
                            dp[i - 1][j - 1] + 1); // Substitution
                }
            }
        }

        return dp[m][n];
    }

    public static Set<String> tokenize(String trace) {
        Set<String> tokens = new HashSet<>();
        for (String line : trace.split("\n")) {
            tokens.add(line.trim());
        }
        return tokens;
    }

}
