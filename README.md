# dinv-monitor
Performs source code instrumentation to embed the invariants. Use `Runtime` 
to collect violations and send this information back to the user. 

Requires JDK8

This will generate a shadow jar in `./build/libs/dinv-monitor-shadow.jar`
```bash
./gradlew shadowJar
```

When instrumenting Cassandra, put this jar file in `$CASSANDRA_DIR/build/lib/jars/`.

## Status
Tested the instrumentation on Cassandra-3.11.14 source code, the instrumented
invariants can be compiled correctly.


## TODO
publish jar file so that it can be added by modifying the pom.xml directly.

