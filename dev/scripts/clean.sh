#!/bin/sh
docker rm --force --volumes smw-app-dev swm-mqtt-explorer-dev
sbt clean
find src -name "*.semanticdb" -type f -delete
