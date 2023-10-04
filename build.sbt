lazy val scala213 = "2.13.12"
lazy val scala3 = "3.3.1"

ThisBuild / scalaVersion := scala213
ThisBuild / organization := "io.github.assist-iot-sripas"
ThisBuild / homepage := Some(url("https://github.com/ASSIST-IoT-SRIPAS/scala-mqtt-wrapper/"))
ThisBuild / licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild / versionScheme := Some("semver-spec")
ThisBuild / developers := List(
  Developer(
    "madpeh",
    "Przemysław Hołda",
    "pholda@ibspan.waw.pl",
    url("https://github.com/madpeh"),
  ),
  Developer(
    "Ostrzyciel",
    "Piotr Sowiński",
    "psowinski@ibspan.waw.pl",
    url("https://github.com/Ostrzyciel"),
  ),
)

ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
sonatypeRepository := "https://s01.oss.sonatype.org/service/local"

ThisBuild / scalafmtConfig := file("dev/configs/scalafmt.conf")

ThisBuild / semanticdbEnabled := true

ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"
ThisBuild / scalafixConfig := Option(file("dev/configs/scalafix.conf"))

ThisBuild / wartremoverWarnings ++= Warts.allBut(Wart.ImplicitParameter, Wart.Equals)

ThisBuild / fork := true

Global / excludeLintKeys += idePackagePrefix

lazy val commonSettings = Seq(
  scalastyleFailOnError := false,
  scalastyleConfig := file("dev/configs/scalastyle-config.xml"),
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
    "-Ywarn-unused",
  ),
)

lazy val logbackVersion = "1.4.7"
lazy val scalaTestVersion = "3.2.15"
lazy val scalaTestPlusScalaCheck = "3.2.15.0"

lazy val commonDependencies = Seq(
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
  "org.scalatestplus" %% "scalacheck-1-17" % scalaTestPlusScalaCheck % Test,
)

lazy val utils = (project in file("modules/utils"))
  .settings(
    commonSettings,
    name := "scala-mqtt-wrapper-utils",
    idePackagePrefix := Some("pl.waw.ibspan.scala_mqtt_wrapper.utils"),
    crossScalaVersions := Seq(scala213, scala3),
    libraryDependencies ++= commonDependencies,
  )

lazy val pekkoVersion = "1.0.1"
lazy val pekkoMqttVersion = "1.0.0"

lazy val pekko = (project in file("modules/pekko"))
  .settings(
    commonSettings,
    name := "scala-mqtt-wrapper-pekko",
    idePackagePrefix := Some("pl.waw.ibspan.scala_mqtt_wrapper.pekko"),
    crossScalaVersions := Seq(scala213, scala3),
    libraryDependencies ++= commonDependencies ++ Seq(
      "org.apache.pekko" %% "pekko-actor-typed" % pekkoVersion,
      "org.apache.pekko" %% "pekko-connectors-mqtt-streaming" % pekkoMqttVersion,
      "org.apache.pekko" %% "pekko-actor-testkit-typed" % pekkoVersion % Test,
    ),
  )
  .dependsOn(utils)
  .aggregate(utils)

lazy val akkaVersion = "2.6.20" // 2.7.x changes license to BSL v1.1
lazy val akkaMqttVersion = "4.0.0" // 5.x changes license to BSL v1.1

lazy val akka = (project in file("modules/akka"))
  .settings(
    commonSettings,
    name := "scala-mqtt-wrapper-akka",
    idePackagePrefix := Some("pl.waw.ibspan.scala_mqtt_wrapper.akka"),
    crossScalaVersions := Seq(scala213),
    libraryDependencies ++= commonDependencies ++ Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.lightbend.akka" %% "akka-stream-alpakka-mqtt-streaming" % akkaMqttVersion,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
      // the following three dependencies are added explicitly to fix the issue
      // with mixed versioning of Akka between "akka-actor-typed" and "akka-stream-alpakka-mqtt-streaming"
      "com.typesafe.akka" %% "akka-protobuf-v3" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream-typed" % akkaVersion,
    ),
  )
  .dependsOn(utils)
  .aggregate(utils)

lazy val root = (project in file("."))
  .aggregate(akka, pekko, utils)
  .settings(
    crossScalaVersions := Nil,
    publish / skip := true,
  )
