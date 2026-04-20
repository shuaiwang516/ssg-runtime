package org.zlab.net.tracker.diff;

import org.zlab.net.tracker.CanonicalKeyMode;
import org.zlab.net.tracker.Trace;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Three-way message identity diff for (old-old, old-new, new-new) traces.
 */
public final class DiffComputeMessageTriDiff {
    private DiffComputeMessageTriDiff() {
    }

    /**
     * Convenience entry point using the production default
     * {@link CanonicalKeyMode#GUIDANCE} identity. Prefer
     * {@link #computeSemantic(Trace, Trace, Trace, CanonicalKeyMode)} when the
     * caller needs to select the tier explicitly.
     */
    public static MessageTriDiffResult compute(Trace trace0, Trace trace1, Trace trace2) {
        return computeSemantic(trace0, trace1, trace2, CanonicalKeyMode.GUIDANCE);
    }

    /**
     * Three-way diff using canonical keys at the default
     * {@link CanonicalKeyMode#GUIDANCE} tier.
     */
    public static MessageTriDiffResult computeSemantic(Trace trace0, Trace trace1, Trace trace2) {
        return computeSemantic(trace0, trace1, trace2, CanonicalKeyMode.GUIDANCE);
    }

    /**
     * Three-way diff using canonical keys at the requested {@link CanonicalKeyMode}
     * tier.
     */
    public static MessageTriDiffResult computeSemantic(Trace trace0, Trace trace1, Trace trace2,
            CanonicalKeyMode mode) {
        CanonicalKeyMode resolved = mode != null ? mode : CanonicalKeyMode.GUIDANCE;
        List<String> seq0 = trace0 != null
                ? trace0.getCanonicalKeysForDiff(resolved)
                : new ArrayList<>();
        List<String> seq1 = trace1 != null
                ? trace1.getCanonicalKeysForDiff(resolved)
                : new ArrayList<>();
        List<String> seq2 = trace2 != null
                ? trace2.getCanonicalKeysForDiff(resolved)
                : new ArrayList<>();
        return computeFromSequences(seq0, seq1, seq2);
    }

    private static MessageTriDiffResult computeFromSequences(List<String> seq0, List<String> seq1,
            List<String> seq2) {
        LinkedHashMap<String, Integer> cnt0 = count(seq0);
        LinkedHashMap<String, Integer> cnt1 = count(seq1);
        LinkedHashMap<String, Integer> cnt2 = count(seq2);
        List<String> orderedKeys = orderedUnion(seq0, seq1, seq2);

        LinkedHashMap<String, Integer> inAllThree = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> only0 = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> only1 = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> only2 = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> in01Only = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> in12Only = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> in02Only = new LinkedHashMap<>();

        for (String key : orderedKeys) {
            int c0 = getCount(cnt0, key);
            int c1 = getCount(cnt1, key);
            int c2 = getCount(cnt2, key);

            int cAll3 = min3(c0, c1, c2);
            int c01 = Math.max(0, Math.min(c0, c1) - cAll3);
            int c12 = Math.max(0, Math.min(c1, c2) - cAll3);
            int c02 = Math.max(0, Math.min(c0, c2) - cAll3);
            int cOnly0 = Math.max(0, c0 - cAll3 - c01 - c02);
            int cOnly1 = Math.max(0, c1 - cAll3 - c01 - c12);
            int cOnly2 = Math.max(0, c2 - cAll3 - c02 - c12);

            putIfPositive(inAllThree, key, cAll3);
            putIfPositive(in01Only, key, c01);
            putIfPositive(in12Only, key, c12);
            putIfPositive(in02Only, key, c02);
            putIfPositive(only0, key, cOnly0);
            putIfPositive(only1, key, cOnly1);
            putIfPositive(only2, key, cOnly2);
        }

        List<String> lcs01 = lcs(seq0, seq1);
        List<String> lcs12 = lcs(seq1, seq2);
        List<String> lcs02 = lcs(seq0, seq2);
        List<String> lcs012 = lcs(lcs01, seq2);

        return new MessageTriDiffResult(seq0, seq1, seq2, inAllThree, only0, only1, only2, in01Only,
                in12Only, in02Only, lcs01.size(), lcs12.size(), lcs02.size(), lcs012.size(),
                lcs012);
    }

    public static final class MessageTriDiffResult {
        public final List<String> sequence0;
        public final List<String> sequence1;
        public final List<String> sequence2;

        public final Map<String, Integer> inAllThree;
        public final Map<String, Integer> only0;
        public final Map<String, Integer> only1;
        public final Map<String, Integer> only2;
        public final Map<String, Integer> in01Only;
        public final Map<String, Integer> in12Only;
        public final Map<String, Integer> in02Only;

        public final int lcs01Length;
        public final int lcs12Length;
        public final int lcs02Length;
        public final int lcs012Length;
        public final List<String> lcs012Sequence;

        private MessageTriDiffResult(List<String> sequence0, List<String> sequence1,
                List<String> sequence2, Map<String, Integer> inAllThree, Map<String, Integer> only0,
                Map<String, Integer> only1, Map<String, Integer> only2,
                Map<String, Integer> in01Only, Map<String, Integer> in12Only,
                Map<String, Integer> in02Only, int lcs01Length, int lcs12Length, int lcs02Length,
                int lcs012Length, List<String> lcs012Sequence) {
            this.sequence0 = sequence0;
            this.sequence1 = sequence1;
            this.sequence2 = sequence2;
            this.inAllThree = inAllThree;
            this.only0 = only0;
            this.only1 = only1;
            this.only2 = only2;
            this.in01Only = in01Only;
            this.in12Only = in12Only;
            this.in02Only = in02Only;
            this.lcs01Length = lcs01Length;
            this.lcs12Length = lcs12Length;
            this.lcs02Length = lcs02Length;
            this.lcs012Length = lcs012Length;
            this.lcs012Sequence = lcs012Sequence;
        }

        public int totalAllThreeCount() {
            return totalCount(inAllThree);
        }

        public int totalExclusiveCount() {
            return totalCount(only0) + totalCount(only1) + totalCount(only2) + totalCount(in01Only)
                    + totalCount(in12Only) + totalCount(in02Only);
        }

        /** Number of messages in the rolling lane (lane 1). */
        public int rollingLaneSize() {
            return sequence1.size();
        }

        /**
         * Messages present only in the rolling lane. Sum of {@code only1}. Always in
         * {@code [0, rollingLaneSize()]} because {@code only1} is a strict subset of
         * {@code sequence1}.
         */
        public int rollingExclusiveCount() {
            return totalCount(only1);
        }

        /**
         * Messages both baselines (lanes 0 and 2) have but the rolling lane is missing.
         * Sum of {@code in02Only}.
         */
        public int rollingMissingCount() {
            return totalCount(in02Only);
        }

        /**
         * Messages both baselines have in common, i.e. the sum over keys of
         * {@code min(c0, c2)}. Equals
         * {@code totalAllThreeCount() + rollingMissingCount()} because every message
         * both baselines share is either present in all three lanes or missing from
         * rolling.
         *
         * <p>
         * Used as the denominator of {@link #rollingMissingFraction()} so the fraction
         * stays bounded in [0, 1] regardless of lane-size skew.
         */
        public int baselineSharedCount() {
            return totalAllThreeCount() + rollingMissingCount();
        }

        /**
         * Fraction of the rolling lane that is exclusive to the rolling lane. Returns
         * {@code 0.0} when the rolling lane is empty. Always in {@code [0, 1]} because
         * {@code only1} is a strict subset of {@code sequence1}.
         */
        public double rollingExclusiveFraction() {
            int denom = rollingLaneSize();
            if (denom <= 0) {
                return 0.0;
            }
            return ((double) rollingExclusiveCount()) / denom;
        }

        /**
         * Fraction of baseline-shared messages that are missing from the rolling lane.
         * Returns {@code 0.0} when the baselines have no messages in common. Always in
         * {@code [0, 1]} because {@code in02Only <= min(c0, c2)} per key.
         */
        public double rollingMissingFraction() {
            int denom = baselineSharedCount();
            if (denom <= 0) {
                return 0.0;
            }
            return ((double) rollingMissingCount()) / denom;
        }

        public double orderedCommonRatio() {
            int minLen = Math.min(sequence0.size(), Math.min(sequence1.size(), sequence2.size()));
            if (minLen == 0) {
                return 1.0;
            }
            return ((double) lcs012Length) / minLen;
        }

        public boolean isInteresting(int minExclusiveCount, double minOrderedCommonRatio) {
            if (totalExclusiveCount() >= minExclusiveCount) {
                return true;
            }
            return orderedCommonRatio() < minOrderedCommonRatio;
        }

        public String toSummaryString() {
            return "MessageTriDiff{" + "len0=" + sequence0.size() + ", len1=" + sequence1.size()
                    + ", len2=" + sequence2.size() + ", all3=" + totalAllThreeCount() + ", only0="
                    + totalCount(only0) + ", only1=" + totalCount(only1) + ", only2="
                    + totalCount(only2) + ", in01Only=" + totalCount(in01Only) + ", in12Only="
                    + totalCount(in12Only) + ", in02Only=" + totalCount(in02Only) + ", lcs012="
                    + lcs012Length + ", orderRatio=" + String.format("%.4f", orderedCommonRatio())
                    + "}";
        }

        public String toDetailedString(int limitPerCategory) {
            StringBuilder sb = new StringBuilder();
            sb.append(toSummaryString()).append('\n');
            appendCategory(sb, "all3", inAllThree, limitPerCategory);
            appendCategory(sb, "only0", only0, limitPerCategory);
            appendCategory(sb, "only1", only1, limitPerCategory);
            appendCategory(sb, "only2", only2, limitPerCategory);
            appendCategory(sb, "in01Only", in01Only, limitPerCategory);
            appendCategory(sb, "in12Only", in12Only, limitPerCategory);
            appendCategory(sb, "in02Only", in02Only, limitPerCategory);
            sb.append("lcs012Sequence(sample)=")
                    .append(sampleSequence(lcs012Sequence, limitPerCategory));
            return sb.toString();
        }
    }

    private static LinkedHashMap<String, Integer> count(List<String> sequence) {
        LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();
        for (String token : sequence) {
            counts.put(token, getCount(counts, token) + 1);
        }
        return counts;
    }

    private static int getCount(Map<String, Integer> counts, String token) {
        Integer count = counts.get(token);
        return count == null ? 0 : count;
    }

    private static int min3(int left, int mid, int right) {
        return Math.min(left, Math.min(mid, right));
    }

    private static void putIfPositive(Map<String, Integer> map, String key, int count) {
        if (count > 0) {
            map.put(key, count);
        }
    }

    private static int totalCount(Map<String, Integer> counts) {
        int total = 0;
        for (Integer value : counts.values()) {
            total += value;
        }
        return total;
    }

    private static List<String> orderedUnion(List<String> seq0, List<String> seq1,
            List<String> seq2) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        set.addAll(seq0);
        set.addAll(seq1);
        set.addAll(seq2);
        return new ArrayList<>(set);
    }

    private static List<String> lcs(List<String> left, List<String> right) {
        int m = left.size();
        int n = right.size();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 1; i <= m; i++) {
            String li = left.get(i - 1);
            for (int j = 1; j <= n; j++) {
                if (li.equals(right.get(j - 1))) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        List<String> lcs = new ArrayList<>();
        int i = m;
        int j = n;
        while (i > 0 && j > 0) {
            if (left.get(i - 1).equals(right.get(j - 1))) {
                lcs.add(0, left.get(i - 1));
                i--;
                j--;
            } else if (dp[i - 1][j] >= dp[i][j - 1]) {
                i--;
            } else {
                j--;
            }
        }
        return lcs;
    }

    private static void appendCategory(StringBuilder sb, String name, Map<String, Integer> counts,
            int limit) {
        sb.append(name).append("={");
        int emitted = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (emitted > 0) {
                sb.append(", ");
            }
            if (emitted >= limit) {
                sb.append("...").append(" totalKeys=").append(counts.size());
                break;
            }
            sb.append(entry.getValue()).append("x ").append(entry.getKey());
            emitted++;
        }
        sb.append("}").append('\n');
    }

    private static String sampleSequence(List<String> sequence, int limit) {
        if (sequence.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        int max = Math.min(sequence.size(), Math.max(1, limit));
        for (int i = 0; i < max; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(sequence.get(i));
        }
        if (sequence.size() > max) {
            sb.append(", ... total=").append(sequence.size());
        }
        sb.append(']');
        return sb.toString();
    }
}
