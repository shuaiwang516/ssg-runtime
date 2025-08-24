package org.zlab.net.tracker;

import java.util.BitSet;
import java.util.List;

public class Utils {
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

    // Not in use
    private static BitSet toBitSet(int[] path) {
        BitSet bs = new BitSet();
        for (int id : path) {
            int bit = Math.floorMod(id, 1024); // choose size e.g. 1k bits
            bs.set(bit);
        }
        return bs;
    }
}
