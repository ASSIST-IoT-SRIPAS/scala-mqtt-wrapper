ThisBuild / scalaVersion := "2.13.10"
ThisBuild / organization := "io.github.assist-iot-sripas"
ThisBuild / homepage := Some(url("https://github.com/ASSIST-IoT-SRIPAS/scala-mqtt-wrapper/"))
ThisBuild / licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
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

sonatypeCredentialHost := "s01.oss.sonatype.org"
sonatypeRepository := "https://s01.oss.sonatype.org/service/local"

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
lazy val logbackVersion = "1.4.5"
lazy val scalaTestVersion = "3.2.14"
lazy val scalaTestPlusScalaCheck = "3.2.14.0"

lazy val root = (project in file("."))
  .settings(
    name := "scala-mqtt-wrapper",
    idePackagePrefix := Some("pl.waw.ibspan.scala_mqtt_wrapper"),
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.lightbend.akka" %% "akka-stream-alpakka-mqtt-streaming" % akkaMqttVersion,
      "ch.qos.logback" % "logback-classic" % logbackVersion,
      "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
      "org.scalatestplus" %% "scalacheck-1-16" % scalaTestPlusScalaCheck % Test,
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
      "-Ywarn-unused",
    ),
  )
