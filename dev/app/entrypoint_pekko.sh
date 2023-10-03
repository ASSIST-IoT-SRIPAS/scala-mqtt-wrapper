#!/bin/sh
sbt "pekko/clean" "~pekko/scalastyle; pekko/compile; pekko/Test/compile; pekko/scalafixAll --check; pekko/testQuick; pekko/reStart"
