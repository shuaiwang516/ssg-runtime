#!/bin/bash

SYSTEM_PATH="/Users/hanke/Desktop/Project/cassandra/cassandra1/src/java"
serializePointsPath="/Users/hanke/Desktop/Project/vasco/system/cassandra/apache-cassandra-2.2.8/serializePoints_alg1.json"
./gradlew instSerializePoints --args="-targetSystemPath ${SYSTEM_PATH} -serializePointsPath ${serializePointsPath}"
