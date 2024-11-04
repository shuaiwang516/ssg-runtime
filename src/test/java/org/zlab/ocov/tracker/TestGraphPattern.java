package org.zlab.ocov.tracker;

import org.junit.jupiter.api.Test;
import org.zlab.ocov.tracker.graph.GraphPattern;

import java.util.HashMap;
import java.util.Map;

public class TestGraphPattern {

    @Test
    public void testMFCompute() {
        Map<String, Map<String, String>> matchableClassInfo = new HashMap<>();
        Map<String, String> classInfo = new HashMap<>();
        classInfo.put("columnsIndex", "java.util.List");
        matchableClassInfo.put("org.apache.cassandra.db.RowIndexEntry$IndexedEntry", classInfo);

        String iti1 = "org.apache.cassandra.db.RowIndexEntry$IndexedEntry->columnsIndex";
        String iti2 = "org.apache.cassandra.db.RowIndexEntry$IndexedEntry->columnsIndex=>Collection->collection_firstItem";

        boolean result = GraphPattern.Vertex.isMatchableFormat(matchableClassInfo, iti1);
        assert result;

        boolean result2 = GraphPattern.Vertex.isMatchableFormat(matchableClassInfo, iti2);
        assert result2;
    }
}
