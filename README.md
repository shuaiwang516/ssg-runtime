# dinv-monitor
Performs source code instrumentation to embed the invariants. Use `Runtime` 
to collect violations and send this information back to the user. 

Requires JDK8

This will generate a shadow jar in `./build/libs/dinv-monitor-shadow.jar`
```bash
./gradlew shadowJar
```

## Instrument systems
### Cassandra
The following env variable will add our runtime jar to the Cassandra's classpath.
```bash
cp /path/to/dinv-monitor-shadow.jar $CASSANDRA_DIR/build/lib/jars
export EXTRA_CLASSPATH=/path/to/dinv-monitor-shadow.jar
```

### HDFS
When compile HDFS, we modify the `$HADOOP_ROOT_PATH/hadoop-hdfs-project/hadoop-hdfs/pom.xml`, 
put the jar file at 
```xml
<dependency>
    <groupId>org.zlab</groupId>
    <artifactId>dinv-monitor</artifactId>
    <version>1.0</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/lib/dinv-monitor-shadow.jar</systemPath>
</dependency>
```

Test the dist version of HDSF, we need to put the runtime jar to the HDFS's classpath.
```bash
$HADOOP_ROOT_PATH/share/hadoop/hdfs/lib/dinv-monitor-shadow.jar
```

## Status
Tested the instrumentation on Cassandra-3.11.14 source code, the instrumented
invariants can be compiled correctly.


## TODOs
Publish jar file so that it can be added by modifying the pom.xml directly.

Handle invariants
- comparison between pre and post state


## Post Process inv

Remove `$assertionsDisabled` related invariants
```bash
# Mac
sed -i '' '/\$assertionsDisabled/d' input/cassandra_inv
# Linux
sed -i '/\$assertionsDisabled/d' filename.txt

```

