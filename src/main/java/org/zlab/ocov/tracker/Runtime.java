package org.zlab.ocov.tracker;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Runtime {
    /**
     * Collect & update coverage information, dump coverage when program finishes.
     */
    public static Path baseClassPath = Paths.get(
            "/Users/hanke/Desktop/Project/vasco/system/cassandra/apache-cassandra-2.2.8/serializedFields_alg1.json");
    public static Path topObjectsPath = Paths
            .get("/Users/hanke/Desktop/Project/ssg-runtime/input/topObjects_cass.json");
    public static ObjectCoverage objectCoverage = new ObjectCoverage(baseClassPath, topObjectsPath);

    public static String filePath = "/Users/hanke/Desktop/Project/cassandra/cassandra1/coverage.log";

    public static BufferedWriter writer;
    public static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static {
        try {
            writer = new BufferedWriter(new FileWriter(filePath, true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void log(String message) {
        try {
            String timeStamp = dateFormat.format(new Date());
            writer.write(timeStamp + " - " + message);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean update(Object obj) {
        boolean val = objectCoverage.update(obj);
        System.out.println("Update coverage " + val);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write("content");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return val;
    }

}
