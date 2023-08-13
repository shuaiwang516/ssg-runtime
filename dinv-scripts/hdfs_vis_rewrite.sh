#!/bin/bash

SYSTEM_PATH1="/Users/hanke/Desktop/Project/hadoop/hadoop1/hadoop-common-project"
SYSTEM_PATH2="/Users/hanke/Desktop/Project/hadoop/hadoop1/hadoop-hdfs-project"

INFO_PATH="/Users/hanke/Desktop/Project/vasco/system/hdfs/hadoop-3.3.0"
UPGRADE_VERSION="hadoop-3.3.0_15624"

./gradlew run --args="-infoPath ${INFO_PATH} -targetSystemPath ${SYSTEM_PATH1} -targetSystemPath ${SYSTEM_PATH2} -upgradeVersion ${UPGRADE_VERSION}"