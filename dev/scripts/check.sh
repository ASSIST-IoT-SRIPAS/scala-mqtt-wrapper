#!/bin/sh
sbt "scalastyle" "clean" "compile" "test:compile" "scalafixAll --check"
