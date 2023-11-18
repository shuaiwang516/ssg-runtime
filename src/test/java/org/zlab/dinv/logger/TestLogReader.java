package org.zlab.dinv.logger;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class TestLogReader {

    // @Test
    public void test() {
        Path filePath = Paths
                .get("/Users/hanke/Desktop/Project/cassandra/cassandra1/logs/serialize.log");
        List<LogEntry> logEntries = LogReader.read(filePath);

        for (LogEntry logEntry : logEntries) {
            System.out.println(logEntry);
        }
    }

}
