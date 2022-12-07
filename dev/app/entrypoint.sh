#!/bin/sh
sbt "clean" "~scalastyle; compile; Test/compile; scalafixAll --check; testQuick; reStart"
