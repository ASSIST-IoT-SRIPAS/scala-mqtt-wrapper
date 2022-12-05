ThisBuild / version := "1.0.0-rc0"
ThisBuild / scalaVersion := "2.13.10"
ThisBuild / organization := "pl.waw.ibspan"

ThisBuild / scalafmtConfig := file("dev/configs/scalafmt.conf")

ThisBuild / semanticdbEnabled := true

ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"
ThisBuild / scalafixConfig := Option(file("dev/configs/scalafix.conf"))

scalastyleFailOnError := false
scalastyleConfig := file("dev/configs/scalastyle-config.xml")

ThisBuild / wartremoverWarnings ++= Warts.allBut(Wart.ImplicitParameter, Wart.Equals)

ThisBuild / fork := true

Global / excludeLintKeys += idePackagePrefix

lazy val akkaVersion = "2.6.19" // 2.7.x changes license to BSL v1.1
lazy val akkaMqttVersion = "4.0.0" // 5.x changes license to BSL v1.1
lazy val scalaLoggingVersion = "3.9.5"
lazy val scalaTestVersion = "3.2.14"
lazy val scalaTestPlusScalaCheck = "3.2.14.0"

lazy val root = (project in file("."))
  .settings(
    name := "scala-mqtt-wrapper",
    idePackagePrefix := Some("pl.waw.ibspan.scala_mqtt_wrapper"),
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.lightbend.akka" %% "akka-stream-alpakka-mqtt-streaming" % akkaMqttVersion,
      "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion,
      "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
      "org.scalatestplus" %% "scalacheck-1-16" % scalaTestPlusScalaCheck % Test
    ),
    scalacOptions ++= Seq(
      "-encoding",
      "utf8",
      "-feature",
      "-unchecked",
      "-deprecation",
      "-Xlint",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Ywarn-value-discard",
      "-Ywarn-unused"
    )
  )
