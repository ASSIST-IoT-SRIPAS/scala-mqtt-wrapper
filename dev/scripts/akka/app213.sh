#!/bin/sh
LIBRARY_VERSION=akka SCALA_VERSION=213 docker compose -f dev/compose/docker-compose.yml up app --build --no-log-prefix
