# Scala MQTT wrapper

## About <a name = "about" />
Scala wrapper for [the Alpakka MQTT Streaming library](https://doc.akka.io/docs/alpakka/current/mqtt-streaming.html).

## Installation <a name = "installation" />
Add the following to your `build.sbt` file:
```scala
libraryDependencies += TODO
```
Currently, the supported Scala version is `2.13.10`.

## Example <a name = "example" />
```scala
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import akka.stream.alpakka.mqtt.streaming.ControlPacketFlags
import akka.stream.alpakka.mqtt.streaming.Command
import akka.stream.alpakka.mqtt.streaming.Subscribe
import pl.waw.ibspan.scala_mqtt_wrapper.MqttClient
import pl.waw.ibspan.scala_mqtt_wrapper.MqttSettings
import pl.waw.ibspan.scala_mqtt_wrapper.MqttSink
import pl.waw.ibspan.scala_mqtt_wrapper.MqttSource
import pl.waw.ibspan.scala_mqtt_wrapper.MqttTopic

// MQTT clients require a running actor system
implicit val system: ActorSystem[Nothing] = ActorSystem[Nothing](
  Behaviors.setup[Nothing] { context =>
    Behaviors.empty
  },
  name = "scalaMqttWrapper"
)

// create a client connected to an MQTT broker 
// and subscribe to one topic ("input")
val sourceClient = new MqttClient(
  MqttSettings(
    host = "mosquitto",
    port = 1883,
    topics = Seq(MqttTopic(name = "input"))
  ),
  name = "sourceClient"
)
// create a source emitting messages from subscribed topics
val source = MqttSource.source(sourceClient)

// create a client connected to the same MQTT broker
val sinkClient = new MqttClient(
  MqttSettings(
    host = "mosquitto",
    port = 1883
  ),
  name = "sinkClient"
)
// create a sink to publish messages
val sink = MqttSink.sink(sinkClient)

// create a flow that converts the incoming messages to uppercase 
// and publishes them to the "output" topic
val uppercaseFlow = Flow[(ByteString, String)].map { case (msg, topic) =>
  val outputMessage = ByteString(msg.utf8String.toUpperCase)
  val outputTopic = "output"
  val publishFlags = ControlPacketFlags.QoSAtLeastOnceDelivery | ControlPacketFlags.RETAIN
  println(
    s"source [$topic] ${msg.utf8String} --> sink [$outputTopic] ${outputMessage.utf8String}"
  )
  (outputMessage, outputTopic, publishFlags)
}

// run the stream
source
  .via(uppercaseFlow)
  .runWith(sink)

// send a command to the client to subscribe to "testTopic"
sourceClient.commands.offer(Command[Nothing](Subscribe("testTopic")))

// after some time, shutdown the clients
Thread.sleep(60000)
sourceClient.shutdown()
sinkClient.shutdown()
```

## Documentation <a name = "documentation" />
- [MqttClient](src/main/scala/MqttClient.scala)
- [MqttSettings](src/main/scala/MqttSettings.scala)
- [MqttSink](src/main/scala/MqttSink.scala)
- [MqttSource](src/main/scala/MqttSource.scala)
- [MqttTopic](src/main/scala/MqttTopic.scala)

## Contributing <a name = "contributing" />
Please follow the [contributing guide](CONTRIBUTING.md) if you wish to contribute to the project.
The guide contains information about the project structure, development environment, code style, etc.
