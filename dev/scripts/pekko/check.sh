#!/bin/sh
sbt "pekko/scalastyle" "pekko/clean" "pekko/compile" "pekko/Test/compile" "pekko/scalafixAll --check"
