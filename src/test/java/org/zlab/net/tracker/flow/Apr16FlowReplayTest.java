package org.zlab.net.tracker.flow;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.zlab.net.tracker.Trace;
import org.zlab.net.tracker.TraceEntry;
import org.zlab.net.tracker.classifier.ProtocolFamily;
import org.zlab.net.tracker.classifier.ProtocolFamilyClass;

/**
 * Phase 2 acceptance-gate replay: runs the real {@link TraceFlowExtractor}
 * against the Apr16 trace dumps (server logs under
 * {@code <apr16-root>/<host>/runner_result/upfuzz_server.log}) and emits
 * per-host, per-lane CSVs plus an aggregate summary.
 *
 * <p>
 * <b>Opt-in only.</b> This test is a maintenance tool, not part of the default
 * build. It stays quiet unless the maintainer sets {@code -Dreplay.apr16=true}
 * on the Gradle command line. The dedicated Gradle task {@code apr16FlowReplay}
 * in {@code ssg-runtime-shuai/build.gradle} wires this up with a larger JVM
 * heap so the multi-GB Apr16 logs do not push the default test worker into OOM.
 * Without the flag the test returns before even looking at the filesystem —
 * this matches how Phase 0's replay validator is invoked outside the default
 * test task.
 *
 * <p>
 * <b>Bounded reader.</b> Apr16 server logs contain stray multi-MB lines (stack
 * traces, unusual payload dumps). {@link BoundedLineReader} drops any line
 * longer than {@link #MAX_LINE_CHARS} before the parser ever sees it, so
 * {@code BufferedReader.readLine()} cannot grow its internal
 * {@code StringBuilder} past the cap and trigger a heap OOM. Entries on skipped
 * lines are counted as {@link HostResult#skippedLongLines} and written to the
 * per-host CSV so the replay owner can see how much of the log was dropped.
 *
 * <p>
 * Parsing constraints: Apr16 predates Phase 1, so the trace dumps do not carry
 * {@code rpcService} / {@code rpcMethod} / {@code messageKind}. Classification
 * therefore leans on {@code protocol} (inferred from the host's configured
 * system) and {@code messageType} / payload class suffix. The replay is there
 * to show the fallback tier produces visible, bounded grouping and that the
 * {@code explicit-ID vs deterministic-fallback} split is meaningful per system.
 */
public class Apr16FlowReplayTest {

    private static final String OPT_IN_PROPERTY = "replay.apr16";
    private static final String ROOT_PROPERTY = "replay.apr16.root";
    private static final String OUT_PROPERTY = "replay.apr16.out";
    private static final Path DEFAULT_APR16_ROOT = Paths
            .get("/mnt/ssd/rupfuzz/cloudlab-results/apr16/raw_data");
    private static final Path DEFAULT_OUT_ROOT = Paths.get("..", "agent", "result",
            "2026-04-19-phase-2-replay");

    /** Cap per parsed entry; prevents runaway allocation. */
    private static final int MAX_ENTRIES_PER_HOST = 120_000;
    /**
     * Maximum characters per log line that the replay will try to parse. Lines
     * longer than this are dropped without ever being materialised into a
     * {@code String}. The ceiling is intentionally generous (1 MiB) — real
     * {@code TraceEntry{...}} dumps stay well below 100 KiB, so lifting this
     * wouldn't help parse more entries but would lower the OOM safety margin.
     */
    private static final int MAX_LINE_CHARS = 1 << 20;

    private static final Pattern ENTRY_RE = Pattern
            .compile("\\[(?<lane>Only Old|Rolling|Only New)\\].*TraceEntry\\{(?<body>.*)\\}\\s*$");

    private static final Pattern P_EVENT_TYPE = Pattern.compile("(?:^|, )eventType=(?<v>[A-Z_]+)");
    private static final Pattern P_NODE_ID = Pattern.compile("(?:^|, )nodeId='(?<v>[^']*)'");
    private static final Pattern P_PEER_ID = Pattern.compile("(?:^|, )peerId='(?<v>[^']*)'");
    private static final Pattern P_NODE_ROLE = Pattern.compile("(?:^|, )nodeRole='(?<v>[^']*)'");
    private static final Pattern P_PEER_ROLE = Pattern.compile("(?:^|, )peerRole='(?<v>[^']*)'");
    private static final Pattern P_MESSAGE_TYPE = Pattern
            .compile("(?:^|, )messageType='(?<v>[^']*)'");
    private static final Pattern P_LOGICAL_ID = Pattern
            .compile("(?:^|, )logicalMessageId='(?<v>[^']*)'");
    private static final Pattern P_DELIVERY_ID = Pattern
            .compile("(?:^|, )deliveryId='(?<v>[^']*)'");
    private static final Pattern P_PAYLOAD = Pattern.compile("(?:^|, )log='(?<v>[^']*)'");

    @Test
    public void replayApr16Flows_writesPerHostCsvAndAggregate() throws IOException {
        if (!Boolean.parseBoolean(System.getProperty(OPT_IN_PROPERTY))) {
            // Default path — leave existing artifacts alone, skip
            // quietly. Run with -Dreplay.apr16=true (or the
            // apr16FlowReplay task) to produce fresh CSVs.
            return;
        }
        Path root = resolveRoot();
        if (!Files.isDirectory(root)) {
            throw new IllegalStateException(
                    "Apr16 replay requested but root directory does not exist: " + root);
        }
        Path outRoot = resolveOutRoot();
        Files.createDirectories(outRoot);
        List<HostResult> results = new ArrayList<>();
        try (java.util.stream.Stream<Path> stream = Files.list(root)) {
            List<Path> hosts = stream.filter(Files::isDirectory).sorted()
                    .collect(java.util.stream.Collectors.toList());
            for (Path host : hosts) {
                HostResult result = replayHost(host);
                if (result == null) {
                    continue;
                }
                writeHostCsv(outRoot, result);
                results.add(result);
            }
        }
        writeAggregateCsv(outRoot, results);
        writeAggregateMarkdown(outRoot, results);
    }

    private static Path resolveRoot() {
        String override = System.getProperty(ROOT_PROPERTY);
        if (override != null && !override.isEmpty()) {
            return Paths.get(override);
        }
        return DEFAULT_APR16_ROOT;
    }

    private static Path resolveOutRoot() {
        String override = System.getProperty(OUT_PROPERTY);
        if (override != null && !override.isEmpty()) {
            return Paths.get(override);
        }
        return DEFAULT_OUT_ROOT;
    }

    private HostResult replayHost(Path host) throws IOException {
        Path log = host.resolve("runner_result").resolve("upfuzz_server.log");
        Path configPath = host.resolve("runner_result").resolve("config.json");
        if (!Files.isRegularFile(log) || !Files.isRegularFile(configPath)) {
            return null;
        }
        String configJson = new String(Files.readAllBytes(configPath), StandardCharsets.UTF_8);
        String system = extractJsonString(configJson, "system");
        String originalVersion = extractJsonString(configJson, "originalVersion");
        String upgradedVersion = extractJsonString(configJson, "upgradedVersion");
        String protocol = protocolForSystem(system);

        Map<String, Trace> lanes = new LinkedHashMap<>();
        lanes.put("old_old", new Trace());
        lanes.put("rolling", new Trace());
        lanes.put("new_new", new Trace());

        int parsed = 0;
        long skippedLongLines = 0;
        try (InputStream in = Files.newInputStream(log);
                Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
                BoundedLineReader lines = new BoundedLineReader(reader, MAX_LINE_CHARS)) {
            String line;
            while ((line = lines.readLine()) != null && parsed < MAX_ENTRIES_PER_HOST) {
                if (line == BoundedLineReader.SKIPPED) {
                    skippedLongLines++;
                    continue;
                }
                Matcher m = ENTRY_RE.matcher(line);
                if (!m.find()) {
                    continue;
                }
                String laneLabel = m.group("lane");
                String body = m.group("body");
                String laneKey = laneKeyFor(laneLabel);
                TraceEntry entry = parseEntry(body, protocol);
                if (entry == null) {
                    continue;
                }
                lanes.get(laneKey).addEntry(entry);
                parsed++;
            }
        }

        Map<String, FlowExtractionResult> extractions = new LinkedHashMap<>();
        for (Map.Entry<String, Trace> e : lanes.entrySet()) {
            extractions.put(e.getKey(), TraceFlowExtractor.extract("POST_STAGE_AGGREGATE",
                    e.getValue(), BoundaryOracle.NO_BOUNDARY));
        }

        return new HostResult(host.getFileName().toString(), system, originalVersion,
                upgradedVersion, parsed, skippedLongLines, lanes, extractions);
    }

    private static String laneKeyFor(String label) {
        switch (label) {
            case "Only Old" :
                return "old_old";
            case "Rolling" :
                return "rolling";
            case "Only New" :
                return "new_new";
            default :
                return "rolling";
        }
    }

    private static String protocolForSystem(String system) {
        if (system == null) {
            return null;
        }
        switch (system.toLowerCase(Locale.ROOT)) {
            case "cassandra" :
                return "cassandra";
            case "hdfs" :
                return "hdfs-rpc";
            case "hbase" :
                return "hbase";
            default :
                return null;
        }
    }

    private static TraceEntry parseEntry(String body, String protocol) {
        String eventTypeStr = findGroup(P_EVENT_TYPE, body);
        if (eventTypeStr == null) {
            return null;
        }
        TraceEntry.EventType eventType;
        try {
            eventType = TraceEntry.EventType.valueOf(eventTypeStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
        String nodeId = nullify(findGroup(P_NODE_ID, body));
        String peerId = nullify(findGroup(P_PEER_ID, body));
        String nodeRole = nullify(findGroup(P_NODE_ROLE, body));
        String peerRole = nullify(findGroup(P_PEER_ROLE, body));
        String messageType = nullify(findGroup(P_MESSAGE_TYPE, body));
        String logicalMessageId = nullify(findGroup(P_LOGICAL_ID, body));
        String deliveryId = nullify(findGroup(P_DELIVERY_ID, body));
        String payload = nullify(findGroup(P_PAYLOAD, body));

        return new TraceEntry(0, "replay", 0, eventType, false, System.currentTimeMillis(),
                System.nanoTime(), nodeId, peerId, nodeRole, peerRole, null, protocol, messageType,
                null, /* rpcService */ null, /* rpcMethod */ null, /* messageKind */ null,
                logicalMessageId, deliveryId, null, -1, 0L, 0L, null, null, false, 0L, null, 0L,
                null, payload);
    }

    private static String findGroup(Pattern pattern, String body) {
        Matcher m = pattern.matcher(body);
        if (m.find()) {
            return m.group("v");
        }
        return null;
    }

    private static String nullify(String s) {
        if (s == null || s.isEmpty() || "null".equals(s)) {
            return null;
        }
        return s;
    }

    private static String extractJsonString(String json, String key) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    // --- CSV writers --------------------------------------------------------

    private static void writeHostCsv(Path outRoot, HostResult result) throws IOException {
        Path dir = outRoot.resolve(result.host);
        Files.createDirectories(dir);
        Path laneCsv = dir.resolve("flow_lane_summary.csv");
        try (BufferedWriter w = Files.newBufferedWriter(laneCsv, StandardCharsets.UTF_8)) {
            w.write("host,system,original_version,upgraded_version,lane,total_entries,total_flows,explicit_id_flows,deterministic_fallback_flows,grouping_failed_flows,explicit_id_fraction,fallback_fraction,family_upgrade_critical_flows,family_background_flows,family_unknown_flows,skipped_long_lines");
            w.newLine();
            for (Map.Entry<String, FlowExtractionResult> e : result.extractions.entrySet()) {
                FlowExtractionResult r = e.getValue();
                int up = countByClass(r, ProtocolFamilyClass.UPGRADE_CRITICAL);
                int bg = countByClass(r, ProtocolFamilyClass.BACKGROUND);
                int unk = countByClass(r, ProtocolFamilyClass.UNKNOWN);
                double explicitFrac = r.flowCount() == 0
                        ? 0.0
                        : (double) r.flowsGroupedWithExplicitId() / r.flowCount();
                double fallbackFrac = r.flowCount() == 0
                        ? 0.0
                        : (double) r.flowsGroupedWithDeterministicFallback() / r.flowCount();
                w.write(String.format(Locale.ROOT,
                        "%s,%s,%s,%s,%s,%d,%d,%d,%d,%d,%.4f,%.4f,%d,%d,%d,%d", result.host,
                        nullSafe(result.system), nullSafe(result.originalVersion),
                        nullSafe(result.upgradedVersion), e.getKey(),
                        result.lanes.get(e.getKey()).size(), r.flowCount(),
                        r.flowsGroupedWithExplicitId(), r.flowsGroupedWithDeterministicFallback(),
                        r.flowsGroupingFailed(), explicitFrac, fallbackFrac, up, bg, unk,
                        result.skippedLongLines));
                w.newLine();
            }
        }

        Path familyCsv = dir.resolve("flow_family_breakdown.csv");
        try (BufferedWriter w = Files.newBufferedWriter(familyCsv, StandardCharsets.UTF_8)) {
            w.write("host,lane,family,flow_event_count");
            w.newLine();
            for (Map.Entry<String, FlowExtractionResult> e : result.extractions.entrySet()) {
                Map<ProtocolFamily, Integer> families = new TreeMap<>(
                        e.getValue().familyMultiset());
                for (Map.Entry<ProtocolFamily, Integer> f : families.entrySet()) {
                    w.write(String.format(Locale.ROOT, "%s,%s,%s,%d", result.host, e.getKey(),
                            f.getKey().name(), f.getValue()));
                    w.newLine();
                }
            }
        }
    }

    private static void writeAggregateCsv(Path outRoot, List<HostResult> results)
            throws IOException {
        Path csv = outRoot.resolve("aggregate_summary.csv");
        try (BufferedWriter w = Files.newBufferedWriter(csv, StandardCharsets.UTF_8)) {
            w.write("host,system,original_version,upgraded_version,total_entries,total_flows,explicit_id_flows,deterministic_fallback_flows,grouping_failed_flows,explicit_id_fraction,fallback_fraction,family_upgrade_critical_flows,family_background_flows,family_unknown_flows,skipped_long_lines");
            w.newLine();
            for (HostResult r : results) {
                int totalFlows = 0;
                int totalExplicit = 0;
                int totalFallback = 0;
                int totalFailed = 0;
                int totalUp = 0;
                int totalBg = 0;
                int totalUnk = 0;
                for (FlowExtractionResult fr : r.extractions.values()) {
                    totalFlows += fr.flowCount();
                    totalExplicit += fr.flowsGroupedWithExplicitId();
                    totalFallback += fr.flowsGroupedWithDeterministicFallback();
                    totalFailed += fr.flowsGroupingFailed();
                    totalUp += countByClass(fr, ProtocolFamilyClass.UPGRADE_CRITICAL);
                    totalBg += countByClass(fr, ProtocolFamilyClass.BACKGROUND);
                    totalUnk += countByClass(fr, ProtocolFamilyClass.UNKNOWN);
                }
                double explicitFrac = totalFlows == 0 ? 0.0 : (double) totalExplicit / totalFlows;
                double fallbackFrac = totalFlows == 0 ? 0.0 : (double) totalFallback / totalFlows;
                w.write(String.format(Locale.ROOT,
                        "%s,%s,%s,%s,%d,%d,%d,%d,%d,%.4f,%.4f,%d,%d,%d,%d", r.host,
                        nullSafe(r.system), nullSafe(r.originalVersion),
                        nullSafe(r.upgradedVersion), r.totalEntries, totalFlows, totalExplicit,
                        totalFallback, totalFailed, explicitFrac, fallbackFrac, totalUp, totalBg,
                        totalUnk, r.skippedLongLines));
                w.newLine();
            }
        }
    }

    private static void writeAggregateMarkdown(Path outRoot, List<HostResult> results)
            throws IOException {
        Path md = outRoot.resolve("aggregate_summary.md");
        try (BufferedWriter w = Files.newBufferedWriter(md, StandardCharsets.UTF_8)) {
            w.write("# Phase 2 flow-replay summary (Apr16 dataset)\n\n");
            w.write("Generated by `Apr16FlowReplayTest` — one row per host, three lanes merged. Run via the dedicated Gradle task: `./gradlew apr16FlowReplay`.\n\n");
            w.write("| Host | System | Versions | Entries | Flows | Explicit | Fallback | Grouping-Failed | Explicit frac | Fallback frac | Upgrade-critical family flows | Background | Unknown | Skipped long lines |\n");
            w.write("|------|--------|----------|--------:|------:|---------:|---------:|----------------:|-------------:|--------------:|-----------------------------:|-----------:|--------:|-------------------:|\n");
            for (HostResult r : results) {
                int totalFlows = 0;
                int totalExplicit = 0;
                int totalFallback = 0;
                int totalFailed = 0;
                int totalUp = 0;
                int totalBg = 0;
                int totalUnk = 0;
                for (FlowExtractionResult fr : r.extractions.values()) {
                    totalFlows += fr.flowCount();
                    totalExplicit += fr.flowsGroupedWithExplicitId();
                    totalFallback += fr.flowsGroupedWithDeterministicFallback();
                    totalFailed += fr.flowsGroupingFailed();
                    totalUp += countByClass(fr, ProtocolFamilyClass.UPGRADE_CRITICAL);
                    totalBg += countByClass(fr, ProtocolFamilyClass.BACKGROUND);
                    totalUnk += countByClass(fr, ProtocolFamilyClass.UNKNOWN);
                }
                double explicitFrac = totalFlows == 0 ? 0.0 : (double) totalExplicit / totalFlows;
                double fallbackFrac = totalFlows == 0 ? 0.0 : (double) totalFallback / totalFlows;
                w.write(String.format(Locale.ROOT,
                        "| %s | %s | %s → %s | %d | %d | %d | %d | %d | %.4f | %.4f | %d | %d | %d | %d |%n",
                        r.host, nullSafe(r.system), nullSafe(r.originalVersion),
                        nullSafe(r.upgradedVersion), r.totalEntries, totalFlows, totalExplicit,
                        totalFallback, totalFailed, explicitFrac, fallbackFrac, totalUp, totalBg,
                        totalUnk, r.skippedLongLines));
            }
            w.write("\n## Caveats\n\n");
            w.write("- Apr16 traces pre-date Phase 1, so `rpcService` / `rpcMethod` / `messageKind` are null. Family classification therefore uses Cassandra verb names from `messageType` and payload-class suffixes from the `log` field. HBase RECV events with `log='null'` fall back to UNKNOWN.\n");
            w.write("- `logicalMessageId` is populated only on HDFS `Client.call` sends; Apr16 Cassandra / HBase bridges did not ship request-level ids. The explicit-ID vs fallback share below reflects this directly.\n");
            w.write("- A single `POST_STAGE_AGGREGATE` bucket is used because Apr16 server logs do not emit per-stage tags inline. Live fuzzing runs will carry the real `comparisonStageId` per window.\n");
            w.write("- The replay parser uses a 1 MiB line cap (see `BoundedLineReader`). `skipped_long_lines` counts the number of over-cap lines dropped per host; it should stay small relative to `total_entries`.\n");
        }
    }

    private static int countByClass(FlowExtractionResult result, ProtocolFamilyClass cls) {
        int count = 0;
        for (Map.Entry<ProtocolFamily, Integer> e : result.familyMultiset().entrySet()) {
            if (e.getKey().familyClass() == cls) {
                count += e.getValue();
            }
        }
        return count;
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    // --- Result record ------------------------------------------------------

    private static final class HostResult {
        final String host;
        final String system;
        final String originalVersion;
        final String upgradedVersion;
        final int totalEntries;
        final long skippedLongLines;
        final Map<String, Trace> lanes;
        final Map<String, FlowExtractionResult> extractions;

        HostResult(String host, String system, String originalVersion, String upgradedVersion,
                int totalEntries, long skippedLongLines, Map<String, Trace> lanes,
                Map<String, FlowExtractionResult> extractions) {
            this.host = host;
            this.system = system;
            this.originalVersion = originalVersion;
            this.upgradedVersion = upgradedVersion;
            this.totalEntries = totalEntries;
            this.skippedLongLines = skippedLongLines;
            this.lanes = lanes;
            this.extractions = extractions;
        }
    }
}
