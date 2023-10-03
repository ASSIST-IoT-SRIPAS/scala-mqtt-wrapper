#!/bin/sh
docker rm --force --volumes smw-app-dev swm-mqtt-explorer-dev
sbt clean
find modules -name "*.semanticdb" -type f -delete
sudo rm -r modules/utils/target modules/pekko/target modules/akka/target project/target target
