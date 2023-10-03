#!/bin/sh
LIBRARY_VERSION=pekko SCALA_VERSION=213 docker compose -f dev/compose/docker-compose.yml up app --build --no-log-prefix
