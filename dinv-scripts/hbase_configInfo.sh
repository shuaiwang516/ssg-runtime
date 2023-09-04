#!/bin/bash

OLD_SYSTEM_PATH="/Users/hanke/Desktop/Project/hbase/hbase1/hbase-common/src/main/java/org/apache/hadoop/hbase/"
NEW_SYSTEM_PATH="/Users/hanke/Desktop/Project/hbase/hbase2/hbase-common/src/main/java/org/apache/hadoop/hbase/"

INFO_PATH="output" # Store the output config info
TARGET_CONFIG_CLASSES="org.apache.hadoop.hbase.HConstants"

#./gradlew modifiedConfigs --args="-infoPath PATH_TO_INFO -targetOldSystemPath PATH_TO_OLD_SYSTEM -targetNewSystemPath PATH_TO_NEW_SYSTEM -tp CONFIG_CLASS1, CONFIG_CLASS2, ..., CONFIG_CLASSN" > /dev/null

./gradlew modifiedConfigs --args="-infoPath ${INFO_PATH} -targetOldSystemPath ${OLD_SYSTEM_PATH} -targetNewSystemPath ${NEW_SYSTEM_PATH} -tc ${TARGET_CONFIG_CLASSES}"
#  > /dev/null
