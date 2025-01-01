package org.zlab.ocov.tracker;

import org.junit.jupiter.api.Test;

public class TestInvariantCombination {
    @Test
    public void testExtractRefPath() {
        String inv = "<NullOnce>, iti = org.apache.cassandra.db.AtomicBTreeColumns->metadata=>org.apache.cassandra.config.CFMetaData->regularColumns=>Collection->collection_firstItem=>org.apache.cassandra.config.ColumnDefinition->indexType";
        String refPath = InvariantCombination.extractRefPath(inv);
        assert refPath.equals(
                "org.apache.cassandra.db.AtomicBTreeColumns->metadata=>org.apache.cassandra.config.CFMetaData->regularColumns=>Collection->collection_firstItem=>org.apache.cassandra.config.ColumnDefinition->indexType");
    }
}
