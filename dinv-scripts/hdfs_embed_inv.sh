#!/bin/bash

SYSTEM_PATH="/Users/hanke/Desktop/Project/hadoop/hadoop1/"
INFO_PATH="/Users/hanke/Desktop/Project/vasco/system/hdfs/hadoop-3.3.0"
UPGRADE_VERSION="hadoop-3.3.0_15624"

./gradlew embedInv --args="-infoPath ${INFO_PATH} -targetSystemPath ${SYSTEM_PATH}"

