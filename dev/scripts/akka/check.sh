#!/bin/sh
sbt "akka/scalastyle" "akka/clean" "akka/compile" "akka/Test/compile" "akka/scalafixAll --check"
