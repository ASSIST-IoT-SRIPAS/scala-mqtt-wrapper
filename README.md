# Scala MQTT wrapper

## About <a name = "about" />
Scala wrapper for [the Alpakka MQTT Streaming library](https://doc.akka.io/docs/alpakka/current/mqtt-streaming.html).

## Installation <a name = "installation" />
Add the following to your `build.sbt` file:
```scala
libraryDependencies += "io.github.assist-iot-sripas" %% "scala-mqtt-wrapper" % "(version)"
```
Currently, the supported Scala version is `2.13.10`.

## Examples <a name = "examples" />

[Example code](example/Main.scala)

## Documentation <a name = "documentation" />
- [MqttClient](src/main/scala/MqttClient.scala)
- [MqttSettings](src/main/scala/MqttSettings.scala)
- [MqttSink](src/main/scala/MqttSink.scala)
- [MqttSource](src/main/scala/MqttSource.scala)
- [MqttTopic](src/main/scala/MqttTopic.scala)

## Contributing <a name = "contributing" />
Please follow the [contributing guide](CONTRIBUTING.md) if you wish to contribute to the project.
The guide contains information about the project structure, development environment, code style, etc.
