#!/bin/sh
sbt "akka/clean" "~akka/scalastyle; akka/compile; akka/Test/compile; akka/scalafixAll --check; akka/testQuick; akka/reStart"
