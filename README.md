# Scala MQTT wrapper
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

## About <a name = "about" />
Scala wrapper for the [Pekko Connectors MQTT Streaming](https://pekko.apache.org/docs/pekko-connectors/current/mqtt-streaming.html) library and the [Alpakka MQTT Streaming](https://doc.akka.io/docs/alpakka/current/mqtt-streaming.html) library.

## Installation <a name = "installation" />
Add the following to your `build.sbt` file.

### Pekko
```scala
libraryDependencies += "io.github.assist-iot-sripas" %% "scala-mqtt-wrapper-pekko" % "(version)"
```
Currently, the supported Scala versions are `2.13.12` and `3.3.1`.

### Akka
```scala
libraryDependencies += "io.github.assist-iot-sripas" %% "scala-mqtt-wrapper-akka" % "(version)"
```
Currently, the supported Scala version is `2.13.12`.

## Examples <a name = "examples" />
* [Pekko](https://github.com/ASSIST-IoT-SRIPAS/scala-mqtt-wrapper/blob/main/examples/PekkoMain.scala)
* [Akka](https://github.com/ASSIST-IoT-SRIPAS/scala-mqtt-wrapper/blob/main/examples/AkkaMain.scala)

## Documentation <a name = "documentation" />
See [Javadoc](https://www.javadoc.io/doc/io.github.assist-iot-sripas/scala-mqtt-wrapper_2.13/latest/pl/waw/ibspan/scala_mqtt_wrapper/index.html) for the latest version.

## Contributing <a name = "contributing" />
Please follow the [contributing guide](CONTRIBUTING.md) if you wish to contribute to the project.
The guide contains information about the project structure, development environment, code style, etc.
