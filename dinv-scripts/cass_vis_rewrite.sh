#!/bin/bash

SYSTEM_PATH="/Users/hanke/Desktop/Project/cassandra/cassandra1/src/java"
INFO_PATH="/Users/hanke/Desktop/Project/vasco/system/cassandra/apache-cassandra-3.11.15"
UPGRADE_VERSION="apache-cassandra-4.1.3"

./gradlew run --args="-infoPath ${INFO_PATH} -targetSystemPath ${SYSTEM_PATH} -upgradeVersion ${UPGRADE_VERSION}"
