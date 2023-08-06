#!/bin/bash

SYSTEM_PATH="/Users/hanke/Desktop/Project/cassandra/cassandra1/src/java"
INFO_PATH="/Users/hanke/Desktop/Project/vasco/system/cassandra/apache-cassandra-3.11.15"

echo ./gradlew embedInv --args="-infoPath ${INFO_PATH} -targetSystemPath ${SYSTEM_PATH}"
./gradlew embedInv --args="-infoPath ${INFO_PATH} -targetSystemPath ${SYSTEM_PATH}"
