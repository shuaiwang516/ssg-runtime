package org.zlab.net.tracker.flow;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;

/**
 * Line reader that replaces {@link java.io.BufferedReader#readLine()} for the
 * Apr16 replay parser. Unlike {@code BufferedReader}, this reader never
 * allocates a character buffer larger than {@code maxLineChars}: once a line
 * exceeds that cap, the reader keeps draining characters until the next newline
 * but discards them without retaining a {@code String}. Callers receive the
 * sentinel {@link #SKIPPED} so they can count the drop.
 *
 * <p>
 * Rationale: Apr16 {@code upfuzz_server.log} files contain stray multi-MB lines
 * (stack traces, unusually large payload dumps). The default
 * {@code BufferedReader.readLine()} grows its internal {@code StringBuilder} to
 * the full line length before returning, which OOMs Gradle's 512 MB test worker
 * on pathological lines. The cap here keeps per-line memory bounded to roughly
 * {@code 2 * maxLineChars} bytes of heap (UTF-16 buffer plus the returned
 * {@code String}).
 *
 * <p>
 * Thread-safety: not safe for concurrent use; each replay host runs
 * sequentially.
 */
final class BoundedLineReader implements Closeable {

    /**
     * Sentinel string returned in place of a parsed line that went past the length
     * cap. Identity comparison ({@code ==}) is intentional — callers should compare
     * against this instance, not its contents, so the sentinel never gets confused
     * with a legitimately empty ("") line.
     */
    public static final String SKIPPED = new String();

    private static final int NO_PENDING = -2;

    private final Reader reader;
    private final int maxLineChars;
    private boolean eof;
    private int pending = NO_PENDING;

    BoundedLineReader(Reader reader, int maxLineChars) {
        if (reader == null) {
            throw new IllegalArgumentException("reader must not be null");
        }
        if (maxLineChars <= 0) {
            throw new IllegalArgumentException("maxLineChars must be > 0");
        }
        this.reader = reader;
        this.maxLineChars = maxLineChars;
    }

    /**
     * Read the next line. Returns {@code null} at end of input, {@link #SKIPPED}
     * when the line exceeded {@link #maxLineChars} (any remaining characters up to
     * the next newline are drained and discarded), or the line itself as a
     * {@code String} (without the trailing {@code \n} / {@code \r\n} / {@code \r}).
     */
    public String readLine() throws IOException {
        if (eof && pending == NO_PENDING) {
            return null;
        }
        StringBuilder sb = new StringBuilder(128);
        boolean overflow = false;
        boolean sawAny = false;
        while (true) {
            int c;
            if (pending != NO_PENDING) {
                c = pending;
                pending = NO_PENDING;
            } else {
                c = reader.read();
            }
            if (c == -1) {
                eof = true;
                if (!sawAny) {
                    return null;
                }
                return overflow ? SKIPPED : sb.toString();
            }
            sawAny = true;
            if (c == '\n') {
                return overflow ? SKIPPED : sb.toString();
            }
            if (c == '\r') {
                // Peek one character to absorb an optional '\n' (CRLF),
                // otherwise stash it for the next readLine invocation so
                // a lone '\r' still ends the current line without
                // consuming the next character.
                int next = reader.read();
                if (next == -1) {
                    eof = true;
                    return overflow ? SKIPPED : sb.toString();
                }
                if (next != '\n') {
                    pending = next;
                }
                return overflow ? SKIPPED : sb.toString();
            }
            if (overflow) {
                continue;
            }
            if (sb.length() >= maxLineChars) {
                overflow = true;
                sb.setLength(0);
                continue;
            }
            sb.append((char) c);
        }
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}
