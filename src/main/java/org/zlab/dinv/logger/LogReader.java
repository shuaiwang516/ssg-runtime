package org.zlab.dinv.logger;

import org.zlab.dinv.serializepoint.Utils;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

public class LogReader {

    public static List<LogEntry> read(Path logPath) {
        List<LogEntry> logEntries = new LinkedList<>();

        List<String> lines = Utils.readLinesFromFile(logPath);

        // extract json string
        for (String line : lines) {
            String jsonStr = line.substring(line.indexOf("{"), line.lastIndexOf("}") + 1);
            LogEntry logEntry = LogEntry.fromJsonString(jsonStr);
            logEntries.add(logEntry);
        }
        return logEntries;
    }

}
