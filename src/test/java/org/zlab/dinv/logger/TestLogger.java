package org.zlab.dinv.logger;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class TestLogger {
    @Test
    public void test() {
        int a = 0;
        int b = System.identityHashCode(a);
        System.out.println(b);
    }

    @Test
    public void testType() {
        Tmp t = new Tmp();
        int a = t.a;
        String name = "a";
        LogEntry logEntry = new LogEntry(t, a, name);
        String s1 = logEntry.toString();
        String json = logEntry.toJsonString();

        // open a file /tmp/log.json and write json to it
        String filename = "/tmp/testLog";
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(json);
        } catch (IOException e) {
            e.printStackTrace();
        }

        LogEntry newLogEntry = LogEntry.fromJsonString(json);
        String s2 = newLogEntry.toString();
        System.out.println(s1);
        System.out.println(s2);

        assert s1.equals(s2);

        String json3 = readFirstLineFromFile(filename);
        LogEntry newLogEntry2 = LogEntry.fromJsonString(json3);
        String s3 = newLogEntry2.toString();
        System.out.println(s3);
        assert s1.equals(s3);
    }

    @Test
    public void testReadLog() {
        LogEntry a = LogEntry.constructLogEntry(org.zlab.dinv.logger.TestLogger.class, null, "a");
        System.out.println(a);
    }

    public static class Tmp {
        public int a = 0;
    }

    public static void foo(Object a) {

    }

    public static String readFirstLineFromFile(String filename) {
        String line = null;

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            line = reader.readLine(); // Reads the first line of the file
        } catch (IOException e) {
            e.printStackTrace();
        }
        return line;
    }
}
