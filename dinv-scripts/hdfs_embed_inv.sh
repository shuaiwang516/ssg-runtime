#!/bin/bash

SYSTEM_PATH1="/Users/hanke/Desktop/Project/hadoop/hadoop1/hadoop-common-project"
SYSTEM_PATH2="/Users/hanke/Desktop/Project/hadoop/hadoop1/hadoop-hdfs-project"
INFO_PATH="/Users/hanke/Desktop/Project/vasco/system/hdfs/hadoop-3.3.0"

./gradlew embedInv --args="-infoPath ${INFO_PATH} -targetSystemPath ${SYSTEM_PATH1} -targetSystemPath ${SYSTEM_PATH2}"

