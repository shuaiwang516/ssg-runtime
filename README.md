# ssg-runtime

This repo is split from dinv-monitor for only JDK8 usage.

* Performs source code instrumentation to embed the invariants. Use `Runtime` 
to collect violations and send this information back to the user. 
* When the jvm exits, it dumps the violations to violations.txt using a
hook

Requirement: JDK8

## Usage

### Logger
```bash
./gradlew loggerShadowJar

# Cassandra
cp build/libs/logger-shadow.jar PATH_TO_CASSANDSRA/lib/
ant artifacts
````

### Scripts
```bash
# isSerialize likely invariants
dinv-scripts/cass_isSerialize.sh
# Visibility Rewrite
dinv-scripts/cass_vis_rewrite.sh
```

### Extract modified configs
```bash
./gradlew modifiedConfigs --args="-infoPath PATH_TO_INFO -targetOldSystemPath PATH_TO_OLD_SYSTEM -targetNewSystemPath PATH_TO_NEW_SYSTEM -tp CONFIX_CLASSES" > /dev/null

./gradlew modifiedConfigs --args="-infoPath output -targetOldSystemPath /Users/hanke/Desktop/Project/cassandra/cassandra1 -targetNewSystemPath /Users/hanke/Desktop/Project/cassandra/cassandra2 -tp org.apache.cassandra.config.Config" > /dev/null
```

### Invariant Monitor
Execute `org.zlab.dinv.runtimechecker.Main` to embed the invariants, recompile
system with our runtime jar.

This will generate a shadow jar in `./build/libs/dinv-monitor-shadow.jar`
```bash
# Invariant Monitor
./gradlew shadowJar
```

#### Cassandra
The following env variable will add our runtime jar to the Cassandra's classpath.
```bash
cp /path/to/dinv-monitor-shadow.jar $CASSANDRA_DIR/lib
# Build the dist version, 
ant artifacts
# $CASSANDRA_DIR/build/apache-cassandra-X.X.X-SNAPSHOT-bin.tar.gz
```

#### HDFS
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

