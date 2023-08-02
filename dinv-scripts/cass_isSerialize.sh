#!/bin/bash

OLD_SYSTEM_PATH="/Users/hanke/Desktop/Project/cassandra/cassandra1/src/java"
NEW_SYSTEM_PATH="/Users/hanke/Desktop/Project/cassandra/cassandra2/src/java"

# store the modified fields
INFO_PATH="/Users/hanke/Desktop/Project/vasco/system/cassandra/apache-cassandra-3.11.15"
TARGET_PREFIX="org.apache.cassandra"

./gradlew modifiedFields --args="-infoPath ${INFO_PATH} -targetOldSystemPath ${OLD_SYSTEM_PATH} -targetNewSystemPath ${NEW_SYSTEM_PATH} -tp ${TARGET_PREFIX}"