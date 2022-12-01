#!/bin/sh
docker-compose -f dev/compose/docker-compose.yml up app --build --no-log-prefix
