package org.zlab.dinv.extractvars;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class NumericConfigs {

    public static void extractCassandraNumericConfigs() throws IOException {
        String projectRootDir =
                "/Users/hanke/Project/cassandra/cassandra1/src/java/org/apache/cassandra";
        Path outputPath = Paths.get("output/numeric_configs.json");

        List<String> targetConfigClasses = new LinkedList<>();
        targetConfigClasses.add("org.apache.cassandra.config.Config");

        Map<String, List<String>> classToNumericFields =
                NumericFields.extractNumericFields(projectRootDir, targetConfigClasses);
        NumericFields.writeNumericFields(outputPath, classToNumericFields);
    }

    public static void main(String[] args) throws IOException {
        extractCassandraNumericConfigs();
    }
}
