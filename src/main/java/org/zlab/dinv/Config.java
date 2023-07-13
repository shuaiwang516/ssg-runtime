package org.zlab.dinv;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

public class Config {

    // public static Path systemInfoPath = Paths.get("/Users/hanke/Desktop/Project/vasco/system/hdfs/hadoop-3.3.4/");
    public static Path systemInfoPath = Paths.get("/Users/hanke/Desktop/Project/vasco/system/cassandra/apache-cassandra-2.2.8/");

    public static Path projectRootDir = Paths.get("/Users/hanke/Project/cassandra/cassandra1/src/java/org/apache/cassandra");
    // public static Path newProjectRootDir = Paths.get("/Users/hanke/Project/cassandra/cassandra2/src/java/org/apache/cassandra");
    // public static String projectRootDir = "/Users/hanke/Desktop/Project/hadoop/hadoop1/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/";
    // public static String newProjectRootDir = "/Users/hanke/Desktop/Project/hadoop/hadoop2/hadoop-hdfs-project/hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/";

    public static final int EDIT_DISTANCE_THRESHOLD = 60;

    // public static List<String> targetPrefixes = new LinkedList<>();
    // static {
    //     if (projectRootDir.toString().contains("cassandra")) {
    //         targetPrefixes.add("org.apache.cassandra");
    //     } else if (projectRootDir.toString().contains("hdfs")) {
    //         targetPrefixes.add("org.apache.hadoop.hdfs");
    //     }
    // }
}
