#!/bin/bash

OLD_SYSTEM_PATH="/home/khan/project/system/hadoop/hadoop1/hadoop-hdfs-project/hadoop-hdfs-client/src/main/java/org/apache/hadoop/hdfs/"
NEW_SYSTEM_PATH="/home/khan/project/system/hadoop/hadoop2/hadoop-hdfs-project/hadoop-hdfs-client/src/main/java/org/apache/hadoop/hdfs/"

INFO_PATH="output" # Store the output config info
TARGET_CONFIG_CLASSES="org.apache.hadoop.hdfs.client.HdfsClientConfigKeys"

#./gradlew modifiedConfigs --args="-infoPath PATH_TO_INFO -targetOldSystemPath PATH_TO_OLD_SYSTEM -targetNewSystemPath PATH_TO_NEW_SYSTEM -tp CONFIX_CLASSES" > /dev/null
# echo ./gradlew modifiedConfigs --args="-infoPath ${INFO_PATH} -targetOldSystemPath ${OLD_SYSTEM_PATH} -targetNewSystemPath ${NEW_SYSTEM_PATH} -tc ${TARGET_CONFIG_CLASSES}" 
./gradlew modifiedConfigs --args="-infoPath ${INFO_PATH} -targetOldSystemPath ${OLD_SYSTEM_PATH} -targetNewSystemPath ${NEW_SYSTEM_PATH} -tc ${TARGET_CONFIG_CLASSES}" > /dev/null
