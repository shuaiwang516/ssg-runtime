# dinv-monitor
* Performs source code instrumentation to embed the invariants. Use `Runtime` 
to collect violations and send this information back to the user. 
* When the jvm exits, it dumps the violations to violations.txt using a
hook

Requirement: JDK8

This will generate a shadow jar in `./build/libs/dinv-monitor-shadow.jar`
```bash
./gradlew shadowJar
```

## Usage

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


## Visibility Rewrite
Our program analysis can track to some interesting variables. We want
daikon to monitor them. However, those variables could be local variables
and daikon won't monitor them. Therefore, we add a local dummy field to
represent their values.

E.g.

Before
```java
pubilc Class Example {
    public void f(int a) {
        int b = 0;
        if (a > b) {
        }

    }
}
```
After
```java
pubilc Class Example {
    public int left_;
    
    public void f(int a) {
        int b = 0;
        left_ = a;
        if (a > b) {
        }
        // Specify invariants over a and a_daikon_dummy_field
    }
}
```

## TODOs

Handle invariants
- comparison between pre and post state

**Avoid the side effect**: the current implementation cannot handle the side effect related stmts.
This will cause problems.

We also need to overwrite the if branches, use the field in the if branches.
```bash

left_ = a++;
right = b;
if (a++ > b) {

}}
```

Test configurations
* ENUM: extract the constants