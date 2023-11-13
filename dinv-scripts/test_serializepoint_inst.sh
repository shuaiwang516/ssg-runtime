#!/bin/bash

SYSTEM_PATH="/Users/hanke/Desktop/Project/vasco/src/test/java/"
serializePointsPath="/Users/hanke/Desktop/Project/vasco/output/serializePoints_alg1.json"
echo ./gradlew instSerializePoints --args="-targetSystemPath ${SYSTEM_PATH} -serializePointsPath ${serializePointsPath}"
#./gradlew instSerializePoints --args="-targetSystemPath ${SYSTEM_PATH} -serializePointsPath ${serializePointsPath}"
