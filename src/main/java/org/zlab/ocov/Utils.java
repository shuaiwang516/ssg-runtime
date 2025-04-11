package org.zlab.ocov;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.zlab.ocov.tracker.Runtime;
import org.zlab.ocov.tracker.graph.GraphPattern;

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

    public static <K, V> int count(Map<K, Set<V>> map) {
        int count = 0;
        for (Map.Entry<K, Set<V>> entry : map.entrySet()) {
            count += entry.getValue().size();
        }
        return count;
    }

    public static void merge(Map<String, Set<String>> leftMap, Map<String, Set<String>> rightMap) {
        // merge to left
        for (Map.Entry<String, Set<String>> entry : rightMap.entrySet()) {
            String key = entry.getKey();
            Set<String> value = entry.getValue();
            if (!leftMap.containsKey(key)) {
                leftMap.put(key, new HashSet<>());
            }
            leftMap.get(key).addAll(value);
        }
    }

    public static Map<String, Set<String>> intersect(Map<String, Set<String>> left,
            Map<String, Set<String>> right) {
        Map<String, Set<String>> result = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : left.entrySet()) {
            String key = entry.getKey();
            if (right.containsKey(key)) {
                Set<String> intersection = new HashSet<>(entry.getValue());
                intersection.retainAll(right.get(key));
                if (!intersection.isEmpty()) {
                    result.put(key, intersection);
                }
            }
        }
        return result;
    }

    public static Map<String, Set<String>> onlyExistInLeft(Map<String, Set<String>> left,
            Map<String, Set<String>> right) {
        Map<String, Set<String>> result = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : left.entrySet()) {
            String key = entry.getKey();
            if (!right.containsKey(key)) {
                result.put(key, entry.getValue());
            }
            if (right.containsKey(key)) {
                Set<String> intersection = new HashSet<>(entry.getValue());
                intersection.removeAll(right.get(key));
                if (!intersection.isEmpty()) {
                    result.put(key, intersection);
                }
            }
        }
        return result;
    }

    // Deprecated: based on number of modified ref path
    public static boolean isMatchableFormat(Map<String, Map<String, String>> matchableClassInfo,
            String itinerary) {
        // If it's null, the ret value won't be used anyway
        if (matchableClassInfo == null)
            return false;

        String[] refs = itinerary.split(GraphPattern.ItiInstanceEdge);
        for (String ref : refs) {
            String[] items = ref.split(GraphPattern.ItiRefEdge);
            if (items.length == 1) {
                // Only check classname
                if (!matchableClassInfo.containsKey(items[0]))
                    return false;
            } else {
                assert items.length == 2;
                // Check the pair
                String className = items[0];
                String fieldName = items[1];

                // Special handle the condition where declaration class is involved
                if (fieldName.contains(")")) {
                    // extract the declaration class: e.g.
                    // (org.zlab.ocov.tracker.TargetClass$TargetClassE)fList by split ")"
                    className = fieldName.substring(fieldName.indexOf("(") + 1,
                            fieldName.indexOf(")"));
                    fieldName = fieldName.substring(fieldName.indexOf(")") + 1);
                }

                // Skip Collection/Map/Array
                if (className.equals("Collection") || className.equals("Map")
                        || className.equals("Array"))
                    continue;

                if (!matchableClassInfo.containsKey(className)
                        || !matchableClassInfo.get(className).containsKey(fieldName)) {
                    // Runtime.log("[debug] unmatchable format: " + className + ", " +
                    // fieldName);
                    return false;
                }
            }
        }
        return true;
    }

    public static class LogisticModel {
        // Constants based on the logistic model parameters
        // N = 0, P = 0,
        // N = 1, P = 0.1,
        // N = 5, P = 0.8...
        double A = 0.95;
        double B = 3.53;

        // Logistic model (increasing)
        public double calculateProbLogisticModel(double N) {
            return 1 / (1 + Math.exp(-A * (N - B)));
        }
    }
    public static LogisticModel logisticModel = new LogisticModel();

    public static class LinearModel {
        // Linear decreasing model
        public double calculateProbLinearModel(int N) {
            // Define the boundary conditions
            int N1 = 1; // When N = 1, prob = 90%
            int N2 = 6; // When N = 6, prob = 10%
            double P1 = 90.0; // Probability at N = 1
            double P2 = 10.0; // Probability at N = 6

            // Linearly interpolate the probability for the given N
            if (N >= N1 && N <= N2) {
                return P1 + (P2 - P1) * ((double) (N - N1) / (N2 - N1));
            } else if (N > N2) {
                return P2; // Probability levels off at 10% beyond N = 6
            } else {
                return P1; // Probability levels off at 90% below N = 1
            }
        }
    }
    public static LinearModel linearModel = new LinearModel();

    public static class ExponentialProbabilityModel {
        private final double c; // Initial probability
        private final double k; // Decay constant

        public ExponentialProbabilityModel() {
            this.c = 0.8; // Probability when N = 0
            this.k = -Math.log(0.1 / 0.8) / 4; // Calculating k using N = 1
        }

        public double calculateProbability(int N) {
            return c * Math.exp(-k * N); // Direct use of N for simpler and correct formula
        }
    }

    public static ExponentialProbabilityModel expDecreaseModel = new ExponentialProbabilityModel();

    public static boolean computeNonMatchable(int nonMatchableNum) {
        return rand.nextDouble() < logisticModel.calculateProbLogisticModel(nonMatchableNum);
    }

    // Decrease based on closest idx of modified ref path
    public static boolean computeNonMatchableProb(int closestModifiedRefIdx) {
        if (closestModifiedRefIdx == -1)
            return false;
        assert closestModifiedRefIdx >= 0;
        return rand.nextDouble() < expDecreaseModel.calculateProbability(closestModifiedRefIdx);
    }

    public static boolean isNonMatchableFormat(Map<String, Map<String, String>> matchableClassInfo,
            Set<String> changedClasses, String itinerary) {
        if (matchableClassInfo == null || changedClasses == null)
            return false;

        // Iterate the ref path reversely
        // 1. Check the object type (if exists)
        // 2. Find the closest modified ref path
        boolean isObjectDirectlyChanged = false;
        int closestModifiedRefIdx = -1;

        String[] refs = itinerary.split(GraphPattern.ItiInstanceEdge);

        // Iterate reversely
        int refCount = -1;
        for (int i = refs.length - 1; i >= 0; i--) {
            String ref = refs[i];
            String[] items = ref.split(GraphPattern.ItiRefEdge);

            // ClassA -> f1 => ClassB (A new ENUM, or a Class first appears)
            if (i == refs.length - 1 && items.length == 1 && changedClasses.contains(items[0])) {
                isObjectDirectlyChanged = true;
                continue;
            }

            if (items.length == 2) {
                refCount++;
                // Check the pair
                String className = items[0];
                String fieldName = items[1];

                // Special handle the condition where declaration class is involved
                if (fieldName.contains(")")) {
                    // extract the declaration class: e.g.
                    // (org.zlab.ocov.tracker.TargetClass$TargetClassE)fList by split ")"
                    className = fieldName.substring(fieldName.indexOf("(") + 1,
                            fieldName.indexOf(")"));
                    fieldName = fieldName.substring(fieldName.indexOf(")") + 1);
                }

                // Skip Collection/Map/Array
                if (className.equals("Collection") || className.equals("Map")
                        || className.equals("Array"))
                    continue;

                // ref is not matchable
                if (!matchableClassInfo.containsKey(className)
                        || !matchableClassInfo.get(className).containsKey(fieldName)) {
                    closestModifiedRefIdx = refCount;
                    break;
                }
            }
        }

        if (Runtime.captureObjectChangedDirectly && isObjectDirectlyChanged)
            return true;
        if (closestModifiedRefIdx == -1)
            return false;
        boolean ret = Runtime.useProbabilityModel
                ? computeNonMatchableProb(closestModifiedRefIdx)
                : closestModifiedRefIdx <= Runtime.distanceThreshold;
        // Debug
        // if (ret) {
        // Runtime.log("[Debug: Non-matchable] " + itinerary);
        // }
        return ret;
    }

    // Store version delta information
    public static class DeltaInfo {
        public Map<String, Map<String, String>> matchableClassInfo;
        public Set<String> changedClasses;

        public DeltaInfo(Map<String, Map<String, String>> matchableClassInfo,
                Set<String> changedClasses) {
            this.matchableClassInfo = matchableClassInfo;
            this.changedClasses = changedClasses;
        }
    }
}
