package net.tracker.diff;

import org.junit.jupiter.api.Test;
import org.zlab.net.tracker.SendMeta;
import org.zlab.net.tracker.Trace;
import org.zlab.net.tracker.diff.DiffComputeMessageTriDiff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        DiffComputeMessageTriDiff.MessageTriDiffResult result = DiffComputeMessageTriDiff
                .compute(oldOld, oldNew, newNew);

        assertEquals(2, result.totalAllThreeCount());
        assertEquals(0, result.totalExclusiveCount());
    }

    private static Trace traceOf(String... messages) {
        Trace trace = new Trace();
        int idx = 0;
        for (String message : messages) {
            trace.recordSend("MessagingService.doSend", 4110001, new int[]{idx}, message,
                    SendMeta.builder().messageType("UnitMessage").build(), message);
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
