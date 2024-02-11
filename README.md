# ssg-runtime

Monitor data format at runtime

Requirement: JDK8

## Generate runtime jar

Generate rt jar
```bash
./gradlew fatJar
cp /Users/hanke/Desktop/Project/ssg-runtime/build/libs/ssgFatJar.jar PATH_TO_LIB
```

Make sure rt class is loaded
```java
// CassandraDaemon.java
public static void main(String[] args)
{
    try {
        Class.forName("org.zlab.ocov.tracker.Runtime");
    } catch (ClassNotFoundException e) {
        e.printStackTrace();
    }
    instance.activate();
}
```

Update monitor coverage
```java
/*
 * Table metadata serialization/deserialization.
 */

public static Mutation makeCreateTableMutation(KSMetaData keyspace, CFMetaData table, long timestamp)
{
    boolean ret = org.zlab.ocov.tracker.Runtime.update(table);
    logger.info("[hklog] ret = " + ret);
    // Include the serialized keyspace in case the target node missed a CREATE KEYSPACE migration (see CASSANDRA-5631).
    Mutation mutation = makeCreateKeyspaceMutation(keyspace, timestamp, false);
    addTableToSchemaMutation(table, timestamp, true, mutation);
    return mutation;
}
```


## Likely invariants

Enable range check (max, min size of collection/map), set it to true.
```java
// SequenceType.java
boolean enableRangeCheck = false;
```


## Embed Cassandra with runtime jar
Add the jar to `lib` folder

## Embed HDFS with runtime jar
When compile HDFS, we modify the `$HADOOP_ROOT_PATH/hadoop-hdfs-project/hadoop-hdfs/pom.xml`, 
create a lib folder: `$HADOOP_ROOT_PATH/hadoop-hdfs-project/hadoop-hdfs/lib/` and put
the jar here.
```xml
<dependency>
    <groupId>org.zlab</groupId>
    <artifactId>dinv-monitor</artifactId>
    <version>1.0</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/lib/dinv-monitor-shadow.jar</systemPath>
</dependency>
```

Test the dist version of HDFS, we need to put the runtime jar to the HDFS's classpath.
```bash
$HADOOP_ROOT_PATH/share/hadoop/hdfs/lib/dinv-monitor-shadow.jar
```

After building the dist version, after untar, also create a lib folder
and add this jar file.


## Corner cases

1. Statically, it's an object type, but dynamically, it's a map/set/collection, check whether we handle it correctly?
2. Accumulated size for collection type, if it's integer, do we handle it?

