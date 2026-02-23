# ssg-runtime

Collect data format coverage at runtime

Requirement: JDK8

## Generate runtime jar

Generate rt jar
```bash
./gradlew fatJar
cp /Users/hanke/Desktop/Project/ssg-runtime/build/libs/ssgFatJar.jar PATH_TO_LIB
```

Init format coverage collector in the main function
```java
org.zlab.ocov.tracker.Runtime.init();
```
E.g. in cassandra
```java
// CassandraDaemon.java
class CassandraDaemon {
    public static void main(String[] args) {
        org.zlab.ocov.tracker.Runtime.init(); // Add this line
        instance.activate();
    }
}
```

Update monitor coverage
```java
/*
 * Table metadata serialization/deserialization.
 */
public class LegacySchemaTables {
    public static Mutation makeCreateTableMutation(KSMetaData keyspace, CFMetaData table, long timestamp)
    {
        org.zlab.ocov.tracker.Runtime.update(table); // auto-instrumented
        Mutation mutation = makeCreateKeyspaceMutation(keyspace, timestamp, false);
        addTableToSchemaMutation(table, timestamp, true, mutation);
        return mutation;
    }
}
```

## Run
The format coverage is disabled by default
```bash
# Enable format coverage
export ENABLE_FORMAT_COVERAGE=true
```

## Likely invariants Configurations
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
    <artifactId>ssg-runtime</artifactId>
    <version>1.0</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/lib/ssgFatJar.jar</systemPath>
</dependency>
```

Test the dist version of HDFS, we need to put the runtime jar to the HDFS's classpath.
```bash
$HADOOP_ROOT_PATH/share/hadoop/hdfs/lib/ssgFatJar.jar
```

After building the dist version, after untar, also create a lib folder
and add this jar file.

## Corner cases
1. Statically, it's an object type, but dynamically, it's a map/set/collection, check whether we handle it correctly?
2. Accumulated size for collection type, if it's integer, is it handled correctly?

# Network

```java
org.zlab.net.tracker.Runtime.init(); // Add this line
```

Sender instrumentation (legacy `record(...)` still works):
```java
org.zlab.net.tracker.Runtime.recordSend(
    "sendMutation",
    1001,
    message,
    org.zlab.net.tracker.SendMeta.builder()
        .nodeId("nodeA")
        .peerId("nodeB")
        .fanoutType("UNICAST") // UNICAST, MULTICAST, BROADCAST
        .logicalMessageId("msg-42")
        .deliveryId("msg-42-nodeB")
        .messageType("Mutation")
        .messageVersion("v2")
        .build(),
    message
);
```

Receiver instrumentation with before/after branch capture:
```java
long token = org.zlab.net.tracker.Runtime.beginReceive(
    "onMutation",
    2001,
    message,
    org.zlab.net.tracker.RecvMeta.builder()
        .nodeId("nodeB")
        .peerId("nodeA")
        .logicalMessageId("msg-42")
        .deliveryId("msg-42-nodeB")
        .messageType("Mutation")
        .messageVersion("v2")
        .build(),
    message
);

// process message ...

org.zlab.net.tracker.Runtime.endReceive(token);
```

Optional network tracing env vars:
```bash
export ENABLE_NETWORK_TRACE=true
export NET_TRACE_NODE_ID=nodeA
export NET_TRACE_PORT=62000
export NET_TRACE_MAX_AFTER_BRANCHES=128
export NET_TRACE_RECEIVE_TIMEOUT_MS=1000
```
