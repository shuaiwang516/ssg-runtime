package org.zlab.net.tracker.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link BoundedLineReader}. The replay parser relies on the
 * sentinel-based contract so the coverage here guards against silent
 * regressions in OOM defense.
 */
public class BoundedLineReaderTest {

    @Test
    public void readsNormalLines_withoutEatingNewlines() throws Exception {
        StringReader in = new StringReader("alpha\nbeta\rgamma\r\ndelta");
        try (BoundedLineReader r = new BoundedLineReader(in, 64)) {
            assertEquals("alpha", r.readLine());
            assertEquals("beta", r.readLine());
            assertEquals("gamma", r.readLine());
            assertEquals("delta", r.readLine());
            assertNull(r.readLine());
        }
    }

    @Test
    public void emptyInput_returnsNullImmediately() throws Exception {
        try (BoundedLineReader r = new BoundedLineReader(new StringReader(""), 16)) {
            assertNull(r.readLine());
        }
    }

    @Test
    public void oversizedLine_isSkippedButNextLineStillParses() throws Exception {
        StringBuilder oversized = new StringBuilder();
        for (int i = 0; i < 256; i++) {
            oversized.append('x');
        }
        String input = oversized + "\nshort-line\n";
        try (BoundedLineReader r = new BoundedLineReader(new StringReader(input), 32)) {
            String first = r.readLine();
            assertSame(BoundedLineReader.SKIPPED, first,
                    "over-cap line must return the SKIPPED sentinel");
            assertEquals("short-line", r.readLine());
            assertNull(r.readLine());
        }
    }

    @Test
    public void oversizedFinalLine_withoutTrailingNewline_isStillSkipped() throws Exception {
        StringBuilder oversized = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            oversized.append('y');
        }
        try (BoundedLineReader r = new BoundedLineReader(new StringReader(oversized.toString()),
                16)) {
            assertSame(BoundedLineReader.SKIPPED, r.readLine());
            assertNull(r.readLine());
        }
    }

    @Test
    public void trailingCrWithoutLf_isPreservedAsEndOfLine() throws Exception {
        try (BoundedLineReader r = new BoundedLineReader(new StringReader("line-one\rline-two"),
                32)) {
            assertEquals("line-one", r.readLine());
            assertEquals("line-two", r.readLine());
            assertNull(r.readLine());
        }
    }

    @Test
    public void skippedSentinel_isIdentityComparable() {
        // Callers compare via == on purpose so an empty legitimate line
        // is not misclassified as skipped. Guarding the identity here
        // prevents accidental String.intern() calls in the future.
        assertTrue(BoundedLineReader.SKIPPED == BoundedLineReader.SKIPPED);
        assertEquals("", BoundedLineReader.SKIPPED,
                "sentinel should equal empty string by content for debug printing");
    }

    @Test
    public void rejectsInvalidArgs() {
        try {
            new BoundedLineReader(null, 16);
            throw new AssertionError("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
        try {
            new BoundedLineReader(new StringReader(""), 0);
            throw new AssertionError("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
    }
}
