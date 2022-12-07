package pl.waw.ibspan.scala_mqtt_wrapper

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.alpakka.mqtt.streaming.Command
import akka.stream.alpakka.mqtt.streaming.ControlPacketFlags
import akka.stream.alpakka.mqtt.streaming.Subscribe
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Source
import akka.util.ByteString

object Main {
  def main(args: Array[String]): Unit = {
    // MQTT clients require a running actor system
    implicit val system: ActorSystem[Nothing] = ActorSystem[Nothing](
      Behaviors.setup[Nothing] { context =>
        Behaviors.empty
      },
      name = "scalaMqttWrapper",
    )

    // create a client connected to an MQTT broker
    // and subscribe to one topic ("input")
    val sourceClient = new MqttClient(
      MqttSettings(
        host = "mosquitto",
        port = 1883,
        topics = Seq(MqttTopic("input")),
      ),
      name = "sourceClient",
    )
    // create a source emitting messages from subscribed topics
    val source = MqttSource.source(sourceClient)

    // create a client connected to the same MQTT broker
    val sinkClient = new MqttClient(
      MqttSettings(
        host = "mosquitto",
        port = 1883,
      ),
      name = "sinkClient",
    )
    // create a sink to publish messages
    val sink = MqttSink.sink(sinkClient)

    // create a flow that converts the incoming messages to uppercase
    // and publishes them to the "output" topic
    val uppercaseFlow = Flow[MqttReceivedMessage].map { case MqttReceivedMessage(payload, topic) =>
      val outputPayload = ByteString(payload.utf8String.toUpperCase)
      val outputTopic = "output"
      val publishFlags = ControlPacketFlags.QoSAtLeastOnceDelivery | ControlPacketFlags.RETAIN
      println(
        s"source [$topic] ${payload.utf8String} --> sink [$outputTopic] ${outputPayload.utf8String}"
      )
      MqttPublishMessage(outputPayload, outputTopic, publishFlags)
    }

    // run the stream
    source
      .via(uppercaseFlow)
      .runWith(sink)

    // send a command to the client to subscribe to the "test" topic
    Source
      .single(Command[Nothing](Subscribe("test")))
      .runWith(sourceClient.commandMergeSink)

    // after some time, shutdown the clients
    Thread.sleep(60000)
    sourceClient.shutdown()
    sinkClient.shutdown()
    println("done")
  }
}
