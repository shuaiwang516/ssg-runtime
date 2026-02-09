package org.zlab.net.tracker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class Utils {
    private static final Random rand = new Random();
    private static final ObjectMapper mapper = new ObjectMapper();

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

    public static long computeHash(int[] path) {
        if (path == null || path.length == 0)
            return -1; // Return 0 for empty paths
        long h = 0xcbf29ce484222325L; // FNV-1a offset basis
        for (int id : path) {
            h ^= id;
            h *= 0x100000001b3L; // FNV-1a prime
        }
        return h;
    }

    public static long computeHash(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return -1;
        }
        List<String> sorted = new ArrayList<>(values);
        Collections.sort(sorted, Comparator.naturalOrder());
        long h = 0xcbf29ce484222325L;
        for (String value : sorted) {
            if (value == null) {
                continue;
            }
            for (int i = 0; i < value.length(); i++) {
                h ^= value.charAt(i);
                h *= 0x100000001b3L;
            }
        }
        return h;
    }

    public static Map<String, Set<String>> loadModifiedFields(Path filePath) {
        try {
            return mapper.readValue(filePath.toFile(),
                    new TypeReference<Map<String, Set<String>>>() {
                    });
        } catch (IOException e) {
            throw new RuntimeException("Failed to read modified fields from " + filePath, e);
        }
    }

    public static List<Integer> sampleIdxFromSize(int size, int sampleSize) {
        List<Integer> idxs = new ArrayList<>();
        int minSize = Math.min(size, sampleSize);
        if (minSize <= 0) {
            return idxs;
        }

        int count = 0;
        while (idxs.size() < minSize) {
            int idx = rand.nextInt(size);
            if (!idxs.contains(idx)) {
                idxs.add(idx);
            }
            count++;
            if (count > 4 * size) {
                break;
            }
        }
        return idxs;
    }

    public static boolean isPrimitiveType(String type) {
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
}
