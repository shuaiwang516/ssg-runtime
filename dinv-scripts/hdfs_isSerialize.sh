#!/bin/bash

OLD_SYSTEM_PATH="/Users/hanke/Desktop/Project/hadoop/hadoop1/"
NEW_SYSTEM_PATH="/Users/hanke/Desktop/Project/hadoop/hadoop2/"

# store the modified fields
INFO_PATH="/Users/hanke/Desktop/Project/vasco/system/hdfs/hadoop-3.3.0/modified/hadoop-3.3.0_15624"
TARGET_PREFIX="org.apache.hadoop.hdfs."

./gradlew modifiedFields --args="-infoPath ${INFO_PATH} -targetOldSystemPath ${OLD_SYSTEM_PATH} -targetNewSystemPath ${NEW_SYSTEM_PATH} -tp ${TARGET_PREFIX}"
