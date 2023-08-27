#!/bin/bash

OLD_SYSTEM_PATH="/Users/hanke/Desktop/Project/cassandra/cassandra1/src/java"
NEW_SYSTEM_PATH="/Users/hanke/Desktop/Project/cassandra/cassandra2/src/java"

INFO_PATH="output" # Store the output config info
TARGET_CONFIG_CLASSES="org.apache.cassandra"

#./gradlew modifiedConfigs --args="-infoPath PATH_TO_INFO -targetOldSystemPath PATH_TO_OLD_SYSTEM -targetNewSystemPath PATH_TO_NEW_SYSTEM -tp CONFIX_CLASSES" > /dev/null

./gradlew modifiedConfigs --args="-infoPath ${INFO_PATH} -targetOldSystemPath ${OLD_SYSTEM_PATH} -targetNewSystemPath ${NEW_SYSTEM_PATH} -tc ${TARGET_CONFIG_CLASSES}" > /dev/null