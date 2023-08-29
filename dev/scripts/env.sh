#!/bin/sh
docker compose -f dev/compose/docker-compose.yml up mosquitto mqtt-explorer vernemq --build
