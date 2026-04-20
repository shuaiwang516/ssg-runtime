package org.zlab.net.tracker.diff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.zlab.net.tracker.classifier.ProtocolFamily;
import org.zlab.net.tracker.classifier.ProtocolFamilyClass;
import org.zlab.net.tracker.flow.CorrelationSource;
import org.zlab.net.tracker.flow.TraceFlowKey;

/**
 * Phase 3 regression tests for {@link FamilyFlowSimilarity}. These tests lock
 * in the family-weighting, explicit-flow filter, and compressed order rules
 * that the scorer depends on.
 */
class FamilyFlowSimilarityTest {

    @Test
    void familyJaccardIsPerfectForEmptyMaps() {
        assertEquals(1.0,
                FamilyFlowSimilarity.familyJaccard(Collections.<ProtocolFamily, Integer>emptyMap(),
                        Collections.<ProtocolFamily, Integer>emptyMap()),
                1e-9);
        assertEquals(1.0, FamilyFlowSimilarity.familyJaccard(null, null), 1e-9);
    }

    @Test
    void familyJaccardCountsIntersectionOverUnion() {
        Map<ProtocolFamily, Integer> a = new LinkedHashMap<>();
        a.put(ProtocolFamily.CASSANDRA_SCHEMA_SYNC, 4);
        a.put(ProtocolFamily.BACKGROUND, 10);
        Map<ProtocolFamily, Integer> b = new LinkedHashMap<>();
        b.put(ProtocolFamily.CASSANDRA_SCHEMA_SYNC, 4);
        b.put(ProtocolFamily.BACKGROUND, 6);
        double jaccard = FamilyFlowSimilarity.familyJaccard(a, b);
        // min(4,4) + min(10,6) = 4 + 6 = 10
        // max(4,4) + max(10,6) = 4 + 10 = 14
        assertEquals(10.0 / 14.0, jaccard, 1e-9);
    }

    @Test
    void weightedJaccardDownweightsBackground() {
        Map<ProtocolFamily, Integer> a = new LinkedHashMap<>();
        a.put(ProtocolFamily.CASSANDRA_SCHEMA_SYNC, 4);
        a.put(ProtocolFamily.BACKGROUND, 10);
        Map<ProtocolFamily, Integer> b = new LinkedHashMap<>();
        b.put(ProtocolFamily.CASSANDRA_SCHEMA_SYNC, 4);
        b.put(ProtocolFamily.BACKGROUND, 6);
        // With upgrade-critical weight 1.0 and background weight 0.2,
        // the schema sync overlap dominates even though the background
        // counts are larger.
        double weighted = FamilyFlowSimilarity.familyClassWeightedJaccard(a, b,
                /* upgradeCriticalWeight */ 1.0, /* backgroundWeight */ 0.2,
                /* unknownWeight */ 0.4);
        double plain = FamilyFlowSimilarity.familyJaccard(a, b);
        assertTrue(weighted > plain,
                "weighted similarity must exceed plain when UPGRADE_CRITICAL agrees and BACKGROUND disagrees");
    }

    @Test
    void zeroWeightFamilyClassIsIgnored() {
        // If the caller passes weight = 0 for a class, that class must
        // drop out of the score entirely.
        Map<ProtocolFamily, Integer> a = new LinkedHashMap<>();
        a.put(ProtocolFamily.CASSANDRA_SCHEMA_SYNC, 1);
        a.put(ProtocolFamily.BACKGROUND, 100);
        Map<ProtocolFamily, Integer> b = new LinkedHashMap<>();
        b.put(ProtocolFamily.CASSANDRA_SCHEMA_SYNC, 1);
        b.put(ProtocolFamily.BACKGROUND, 0);
        double weighted = FamilyFlowSimilarity.familyClassWeightedJaccard(a, b,
                /* upgradeCriticalWeight */ 1.0, /* backgroundWeight */ 0.0,
                /* unknownWeight */ 0.4);
        // Schema sync overlaps perfectly; background was zeroed out —
        // weighted similarity must be exactly 1.0.
        assertEquals(1.0, weighted, 1e-9);
    }

    @Test
    void explicitFlowJaccardFiltersFallbackKeys() {
        TraceFlowKey explicitKey = new TraceFlowKey("s", "A", "B",
                ProtocolFamily.CASSANDRA_SCHEMA_SYNC, "lmid-1",
                CorrelationSource.EXPLICIT_LOGICAL_ID);
        TraceFlowKey fallbackKey = new TraceFlowKey("s", "A", "B",
                ProtocolFamily.CASSANDRA_SCHEMA_SYNC, "bucket-0",
                CorrelationSource.DETERMINISTIC_FALLBACK);
        Map<TraceFlowKey, Integer> a = new LinkedHashMap<>();
        a.put(explicitKey, 2);
        a.put(fallbackKey, 5);
        Map<TraceFlowKey, Integer> b = new LinkedHashMap<>();
        b.put(explicitKey, 2);
        b.put(fallbackKey, 0);

        double explicit = FamilyFlowSimilarity.explicitFlowJaccard(a, b);
        assertEquals(1.0, explicit, 1e-9,
                "explicit flow matches perfectly when fallback keys are filtered out");
        double all = FamilyFlowSimilarity.flowJaccard(a, b);
        assertTrue(all < explicit,
                "the raw flow jaccard dilutes explicit agreement with fallback drift");
    }

    @Test
    void singleContextOrderLcsIsNormalizedByLongerSequence() {
        List<ProtocolFamily> a = Arrays.asList(ProtocolFamily.CASSANDRA_SCHEMA_SYNC,
                ProtocolFamily.CASSANDRA_PAXOS);
        List<ProtocolFamily> b = Arrays.asList(ProtocolFamily.CASSANDRA_SCHEMA_SYNC,
                ProtocolFamily.CASSANDRA_PAXOS, ProtocolFamily.CASSANDRA_REPAIR_OR_STREAM);
        double sim = FamilyFlowSimilarity.singleContextOrderSimilarity(a, b);
        assertEquals(2.0 / 3.0, sim, 1e-9);
    }

    @Test
    void perRolePairOrderAveragesAcrossSharedPairsOnly() {
        // Shared pair "A->B" on both lanes with one order mismatch,
        // independent pair "A->C" on lane a only, independent pair
        // "D->E" on lane b only. The scorer must evaluate only the
        // shared pair and skip the independent ones.
        Map<String, List<ProtocolFamily>> a = new LinkedHashMap<>();
        a.put("A->B", Arrays.asList(ProtocolFamily.CASSANDRA_SCHEMA_SYNC,
                ProtocolFamily.CASSANDRA_PAXOS));
        a.put("A->C", Arrays.asList(ProtocolFamily.CASSANDRA_REPAIR_OR_STREAM));
        Map<String, List<ProtocolFamily>> b = new LinkedHashMap<>();
        b.put("A->B", Arrays.asList(ProtocolFamily.CASSANDRA_PAXOS,
                ProtocolFamily.CASSANDRA_SCHEMA_SYNC));
        b.put("D->E", Arrays.asList(ProtocolFamily.CASSANDRA_REPAIR_OR_STREAM));

        double sim = FamilyFlowSimilarity.perRolePairCompressedOrderSimilarity(a, b,
                ProtocolFamilyClass.UPGRADE_CRITICAL);
        // "A->B": LCS([SCHEMA, PAXOS], [PAXOS, SCHEMA]) = 1, normalized by 2 => 0.5
        // Only one shared pair so the average is 0.5.
        assertEquals(0.5, sim, 1e-9);
    }

    @Test
    void perRolePairOrderReturnsOneWhenNoSharedPairs() {
        Map<String, List<ProtocolFamily>> a = new LinkedHashMap<>();
        a.put("A->B", Arrays.asList(ProtocolFamily.CASSANDRA_PAXOS));
        Map<String, List<ProtocolFamily>> b = new LinkedHashMap<>();
        b.put("X->Y", Arrays.asList(ProtocolFamily.CASSANDRA_SCHEMA_SYNC));

        double sim = FamilyFlowSimilarity.perRolePairCompressedOrderSimilarity(a, b,
                ProtocolFamilyClass.UPGRADE_CRITICAL);
        assertEquals(1.0, sim, 1e-9,
                "no shared role pair must be treated as perfect ordering agreement");
    }

    @Test
    void perRolePairOrderIgnoresBackgroundChatter() {
        // Shared pair "A->B". Both sides carry an upgrade-critical
        // backbone (SCHEMA -> PAXOS). The rolling lane additionally
        // interleaves heavy BACKGROUND gossip. With the
        // UPGRADE_CRITICAL class filter in effect, the reordered
        // background chatter must not move the score.
        Map<String, List<ProtocolFamily>> a = new LinkedHashMap<>();
        a.put("A->B", Arrays.asList(ProtocolFamily.CASSANDRA_SCHEMA_SYNC,
                ProtocolFamily.CASSANDRA_PAXOS));
        Map<String, List<ProtocolFamily>> b = new LinkedHashMap<>();
        b.put("A->B",
                Arrays.asList(ProtocolFamily.BACKGROUND, ProtocolFamily.CASSANDRA_SCHEMA_SYNC,
                        ProtocolFamily.BACKGROUND, ProtocolFamily.CASSANDRA_PAXOS,
                        ProtocolFamily.BACKGROUND));

        double sim = FamilyFlowSimilarity.perRolePairCompressedOrderSimilarity(a, b,
                ProtocolFamilyClass.UPGRADE_CRITICAL);
        assertEquals(1.0, sim, 1e-9,
                "background reordering around a perfect upgrade-critical backbone must not produce divergence");
    }

    @Test
    void filterByClassKeepsUpgradeCriticalOnly() {
        List<ProtocolFamily> sequence = Arrays.asList(ProtocolFamily.BACKGROUND,
                ProtocolFamily.CASSANDRA_SCHEMA_SYNC, ProtocolFamily.BACKGROUND,
                ProtocolFamily.CASSANDRA_PAXOS);
        List<ProtocolFamily> filtered = FamilyFlowSimilarity.filterByClass(sequence,
                ProtocolFamilyClass.UPGRADE_CRITICAL);
        assertEquals(2, filtered.size());
        assertEquals(ProtocolFamily.CASSANDRA_SCHEMA_SYNC, filtered.get(0));
        assertEquals(ProtocolFamily.CASSANDRA_PAXOS, filtered.get(1));
    }

    @Test
    void filterByClassCollapsesConsecutiveDuplicatesPostFilter() {
        List<ProtocolFamily> sequence = Arrays.asList(ProtocolFamily.CASSANDRA_SCHEMA_SYNC,
                ProtocolFamily.BACKGROUND, ProtocolFamily.CASSANDRA_SCHEMA_SYNC);
        // After filtering out BACKGROUND the two SCHEMA_SYNC entries
        // become adjacent and must collapse to a single entry.
        List<ProtocolFamily> filtered = FamilyFlowSimilarity.filterByClass(sequence,
                ProtocolFamilyClass.UPGRADE_CRITICAL);
        assertEquals(1, filtered.size());
        assertEquals(ProtocolFamily.CASSANDRA_SCHEMA_SYNC, filtered.get(0));
    }

    @Test
    void classShareComputesProportionOfTargetedClass() {
        Map<ProtocolFamily, Integer> counts = new LinkedHashMap<>();
        counts.put(ProtocolFamily.CASSANDRA_SCHEMA_SYNC, 2);
        counts.put(ProtocolFamily.BACKGROUND, 8);
        assertEquals(0.2,
                FamilyFlowSimilarity.classShare(counts, ProtocolFamilyClass.UPGRADE_CRITICAL),
                1e-9);
        assertEquals(0.8, FamilyFlowSimilarity.classShare(counts, ProtocolFamilyClass.BACKGROUND),
                1e-9);
        assertEquals(0.0, FamilyFlowSimilarity.classShare(counts, ProtocolFamilyClass.UNKNOWN),
                1e-9);
    }
}
