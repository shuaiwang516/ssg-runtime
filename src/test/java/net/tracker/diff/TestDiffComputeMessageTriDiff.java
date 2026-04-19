package net.tracker.diff;

import org.junit.jupiter.api.Test;
import org.zlab.net.tracker.CanonicalKeyMode;
import org.zlab.net.tracker.SendMeta;
import org.zlab.net.tracker.Trace;
import org.zlab.net.tracker.diff.DiffComputeMessageTriDiff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class TestDiffComputeMessageTriDiff {
    @Test
    public void testThreeWayMembership() {
        Trace oldOld = traceOf("m1", "m2", "m5", "m6");
        Trace oldNew = traceOf("m1", "m2", "m3", "m5", "m6");
        Trace newNew = traceOf("m2", "m3", "m4", "m5", "m6");

        DiffComputeMessageTriDiff.MessageTriDiffResult result = DiffComputeMessageTriDiff
                .compute(oldOld, oldNew, newNew);

        assertEquals(3, result.totalAllThreeCount()); // m2,m5,m6
        assertEquals(1, total(result.in01Only)); // m1
        assertEquals(1, total(result.in12Only)); // m3
        assertEquals(0, total(result.in02Only));
        assertEquals(0, total(result.only0));
        assertEquals(0, total(result.only1));
        assertEquals(1, total(result.only2)); // m4
    }

    @Test
    public void testOrderDifference() {
        Trace seq0 = traceOf("m1", "m2", "m3");
        Trace seq1 = traceOf("m1", "m3", "m2");
        Trace seq2 = traceOf("m1", "m2", "m3");

        DiffComputeMessageTriDiff.MessageTriDiffResult result = DiffComputeMessageTriDiff
                .compute(seq0, seq1, seq2);

        assertEquals(3, result.totalAllThreeCount());
        assertTrue(result.orderedCommonRatio() < 1.0);
        assertTrue(result.isInteresting(1, 0.95));
    }

    // Phase 1 regression tests for tri-diff fraction correctness. These prove
    // that missing-message churn no longer produces fractions above 1.0 and
    // that missing-only windows are distinguishable from rolling-exclusive
    // windows via the new accessors.

    @Test
    public void rollingExclusiveFractionRemainsBounded() {
        Trace oldOld = traceOf("m1", "m2", "m3");
        Trace rolling = traceOf("m1", "m2", "m3", "m4", "m5");
        Trace newNew = traceOf("m1", "m2", "m3");

        DiffComputeMessageTriDiff.MessageTriDiffResult result = DiffComputeMessageTriDiff
                .compute(oldOld, rolling, newNew);

        assertEquals(2, result.rollingExclusiveCount());
        assertEquals(5, result.rollingLaneSize());
        assertEquals(0.4, result.rollingExclusiveFraction(), 1e-9);
        assertTrue(result.rollingExclusiveFraction() >= 0.0);
        assertTrue(result.rollingExclusiveFraction() <= 1.0);
    }

    @Test
    public void rollingMissingFractionBoundedWhenRollingShort() {
        // Baselines share nine messages but rolling dropped every one of them
        // (short rolling lane). Under the pre-Phase-1 normalization
        // (missing / rollingLaneSize) this would produce 9/1 = 9.0. The new
        // baseline-shared denominator must keep the value in [0, 1].
        Trace oldOld = traceOf("m1", "m2", "m3", "m4", "m5", "m6", "m7", "m8", "m9");
        Trace rolling = traceOf("keepAlive");
        Trace newNew = traceOf("m1", "m2", "m3", "m4", "m5", "m6", "m7", "m8", "m9");

        DiffComputeMessageTriDiff.MessageTriDiffResult result = DiffComputeMessageTriDiff
                .compute(oldOld, rolling, newNew);

        assertEquals(9, result.rollingMissingCount());
        assertEquals(0, result.totalAllThreeCount());
        assertEquals(9, result.baselineSharedCount());
        assertEquals(1.0, result.rollingMissingFraction(), 1e-9);
        assertTrue(result.rollingMissingFraction() <= 1.0,
                "missing fraction must stay in [0,1], got " + result.rollingMissingFraction());
    }

    @Test
    public void rollingMissingFractionDefaultsToZeroWhenBaselinesShareNothing() {
        Trace oldOld = traceOf("a1", "a2");
        Trace rolling = traceOf("b1", "b2");
        Trace newNew = traceOf("c1", "c2");

        DiffComputeMessageTriDiff.MessageTriDiffResult result = DiffComputeMessageTriDiff
                .compute(oldOld, rolling, newNew);

        assertEquals(0, result.rollingMissingCount());
        assertEquals(0, result.baselineSharedCount());
        assertEquals(0.0, result.rollingMissingFraction(), 1e-9);
    }

    @Test
    public void pureMissingWindowProducesNoRollingExclusive() {
        // Both baselines share m1..m5 but rolling dropped m3 and m5. No messages
        // are exclusive to rolling. This mirrors the "benign rolling drop"
        // pattern that dominated Apr 12 missing-only admissions.
        Trace oldOld = traceOf("m1", "m2", "m3", "m4", "m5");
        Trace rolling = traceOf("m1", "m2", "m4");
        Trace newNew = traceOf("m1", "m2", "m3", "m4", "m5");

        DiffComputeMessageTriDiff.MessageTriDiffResult result = DiffComputeMessageTriDiff
                .compute(oldOld, rolling, newNew);

        assertEquals(0, result.rollingExclusiveCount());
        assertEquals(2, result.rollingMissingCount());
        assertEquals(3, result.totalAllThreeCount());
        assertEquals(5, result.baselineSharedCount());
        assertEquals(0.4, result.rollingMissingFraction(), 1e-9);
        assertEquals(0.0, result.rollingExclusiveFraction(), 1e-9);
    }

    @Test
    public void realRollingExclusiveCaseIsDetected() {
        Trace oldOld = traceOf("m1", "m2", "m3");
        Trace rolling = traceOf("m1", "m2", "m3", "rolling_only_1", "rolling_only_2",
                "rolling_only_3");
        Trace newNew = traceOf("m1", "m2", "m3");

        DiffComputeMessageTriDiff.MessageTriDiffResult result = DiffComputeMessageTriDiff
                .compute(oldOld, rolling, newNew);

        assertEquals(3, result.rollingExclusiveCount());
        assertEquals(0, result.rollingMissingCount());
        assertTrue(result.rollingExclusiveFraction() > 0.0);
        assertTrue(result.rollingExclusiveFraction() <= 1.0);
    }

    @Test
    public void fractionInvariantsHoldOnLargeAsymmetricSequences() {
        // Smoke test: construct 100 shared messages and a sparse rolling lane.
        // Guards against off-by-one mistakes in the counting formula.
        String[] shared = new String[100];
        for (int i = 0; i < shared.length; i++) {
            shared[i] = "shared-" + i;
        }
        Trace oldOld = traceOf(shared);
        Trace newNew = traceOf(shared);
        Trace rolling = traceOf("shared-0", "shared-1", "shared-2", "rolling-only");

        DiffComputeMessageTriDiff.MessageTriDiffResult result = DiffComputeMessageTriDiff
                .compute(oldOld, rolling, newNew);

        // 3 survived in all three lanes; 97 shared-* are missing from rolling.
        assertEquals(3, result.totalAllThreeCount());
        assertEquals(97, result.rollingMissingCount());
        assertEquals(100, result.baselineSharedCount());
        assertTrue(result.rollingMissingFraction() >= 0.0);
        assertTrue(result.rollingMissingFraction() <= 1.0);
        assertEquals(1, result.rollingExclusiveCount());
        assertEquals(4, result.rollingLaneSize());
        assertTrue(result.rollingExclusiveFraction() >= 0.0);
        assertTrue(result.rollingExclusiveFraction() <= 1.0);
        assertFalse(result.rollingMissingFraction() > 1.0,
                "regression: rollingMissingFraction exceeded 1.0");
    }

    @Test
    public void testSemanticIdentityNormalizesVolatileHeaderNumbers() {
        Trace oldOld = traceOfSummaries("PING_REQ",
                "Message|Header|13135262944949|13145262944949|0|InetAddressAndPort|byte[]|[len=4]|-64|-88|33|2|30|EnumMap|{size=0}|PING_REQ|PingRequest|SMALL_MESSAGES|-1|-1|0|0",
                "Message|Header|13135262944949|13145262944949|0|InetAddressAndPort|byte[]|[len=4]|-64|-88|33|2|31|EnumMap|{size=0}|PING_REQ|PingRequest|LARGE_MESSAGES|-1|-1|0|0");
        Trace oldNew = traceOfSummaries("PING_REQ",
                "Message|Header|23135262944949|23145262944949|0|InetAddressAndPort|byte[]|[len=4]|-64|-88|44|2|130|EnumMap|{size=0}|PING_REQ|PingRequest|SMALL_MESSAGES|-1|-1|0|0",
                "Message|Header|23135262944949|23145262944949|0|InetAddressAndPort|byte[]|[len=4]|-64|-88|44|2|131|EnumMap|{size=0}|PING_REQ|PingRequest|LARGE_MESSAGES|-1|-1|0|0");
        Trace newNew = traceOfSummaries("PING_REQ",
                "Message|Header|33135262944949|33145262944949|0|InetAddressAndPort|byte[]|[len=4]|-64|-88|55|2|230|EnumMap|{size=0}|PING_REQ|PingRequest|SMALL_MESSAGES|-1|-1|0|0",
                "Message|Header|33135262944949|33145262944949|0|InetAddressAndPort|byte[]|[len=4]|-64|-88|55|2|231|EnumMap|{size=0}|PING_REQ|PingRequest|LARGE_MESSAGES|-1|-1|0|0");

        // SEMANTIC_SHAPE_SUMMARY is retained after Phase 1 for offline
        // diagnosis; it is the tier where summary-level volatile-token
        // normalization still governs identity collapse.
        DiffComputeMessageTriDiff.MessageTriDiffResult result = DiffComputeMessageTriDiff
                .computeSemantic(oldOld, oldNew, newNew, CanonicalKeyMode.SEMANTIC_SHAPE_SUMMARY);

        assertEquals(2, result.totalAllThreeCount());
        assertEquals(0, result.totalExclusiveCount());
    }

    private static Trace traceOf(String... messages) {
        Trace trace = new Trace();
        int idx = 0;
        for (String message : messages) {
            // Carry each synthetic message id through {@code messageType} so
            // the Phase 1 GUIDANCE key — which collapses unclassified traffic
            // into a single {@code UNKNOWN:<rawSemanticType>} bucket per type
            // — keeps the messages distinguishable for tri-diff mechanics.
            trace.recordSend("MessagingService.doSend", 4110001, new int[]{idx}, message,
                    SendMeta.builder().messageType(message).build(), message);
            idx++;
        }
        return trace;
    }

    private static Trace traceOfSummaries(String messageType, String... summaries) {
        Trace trace = new Trace();
        int idx = 0;
        for (String summary : summaries) {
            trace.recordSend("MessagingService.doSend", 4110001, new int[]{idx}, summary,
                    SendMeta.builder().messageType(messageType).build(), summary);
            idx++;
        }
        return trace;
    }

    private static int total(java.util.Map<String, Integer> counts) {
        int sum = 0;
        for (Integer value : counts.values()) {
            sum += value;
        }
        return sum;
    }
}
