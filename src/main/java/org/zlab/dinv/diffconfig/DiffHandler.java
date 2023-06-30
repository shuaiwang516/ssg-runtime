package org.zlab.dinv.diffconfig;

import java.util.Map;
import java.util.Set;

import static org.zlab.dinv.diffconfig.RetrieveHandlerBlock.retrieveBlock;

public class DiffHandler {

    public static void main(String[] args) {
        // input
        Map<String, Map<String, Map<String, Set<Integer>>>> config2branchProgramLocations;

        // process: for each config, retrieve the block that directly contains the
        // if branch

        // output
        Map<String, Map<String, Map<String, Set<String>>>> oldConfig2handlerBlocks =
                retrieveBlock();
        Map<String, Map<String, Map<String, Set<String>>>> newConfig2handlerBlocks =
                retrieveBlock();
        // Comparison
        // Calculate the edit distance between two blocks (if the number is different,
        // also mark it
    }

    public static int editDistance(String string1, String string2) {
        int len1 = string1.length();
        int len2 = string2.length();

        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = (string1.charAt(i - 1) == string2.charAt(j - 1)) ? 0 : 1;

                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[len1][len2];
    }
}
