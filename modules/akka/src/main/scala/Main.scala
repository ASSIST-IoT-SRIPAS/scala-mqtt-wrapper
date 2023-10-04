package pl.waw.ibspan.scala_mqtt_wrapper.akka

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.Attributes
import akka.stream.alpakka.mqtt.streaming.Command
import akka.stream.alpakka.mqtt.streaming.ControlPacketFlags
import akka.stream.alpakka.mqtt.streaming.Subscribe
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Source
import akka.util.ByteString

private[scala_mqtt_wrapper] object Main {
  def main(args: Array[String]): Unit = {
    // MQTT clients require a running actor system
    implicit val system: ActorSystem[Nothing] = ActorSystem[Nothing](
      Behaviors.setup[Nothing] { _ =>
        Behaviors.empty
      },
      name = "scalaMqttWrapper",
    )

    // create shared logging attributes
    val loggingAttributes = Attributes.logLevels(
      onElement = Attributes.LogLevels.Info,
      onFinish = Attributes.LogLevels.Info,
      onFailure = Attributes.LogLevels.Error,
    )

    // create a client connected to an MQTT broker
    // and subscribe to one topic ("input")
    val sourceClient = new MqttClient(
      MqttSettings(
        host = "mosquitto",
        port = 1883,
        subscriptions =
          Seq(MqttTopic(name = "input", flags = SubscribeQoSFlags.QoSAtLeastOnceDelivery)),
      ),
      loggingSettings = Some(
        MqttLoggingSettings(name = "sourceClient", attributes = loggingAttributes)
      ),
    )

    // create a source emitting messages from subscribed topics
    val source = MqttSource.source(
      sourceClient,
      loggingSettings = Some(MqttLoggingSettings(name = "source", attributes = loggingAttributes)),
    )

    // create a client connected to the same MQTT broker
    val sinkClient = new MqttClient(
      MqttSettings(
        host = "mosquitto",
        port = 1883,
      ),
      loggingSettings = Some(
        MqttLoggingSettings(name = "sinkClient", attributes = loggingAttributes)
      ),
    )

    // create a sink to publish messages
    val sink = MqttSink.sink(
      sinkClient,
      loggingSettings = Some(MqttLoggingSettings(name = "sink", attributes = loggingAttributes)),
    )

    // create a flow that converts the incoming messages to uppercase
    // and publishes them to the "output" topic
    val uppercaseFlow = Flow[MqttReceivedMessage].map {
      case MqttReceivedMessage(payload, topic, flags, packetId) =>
        val outputPayload = ByteString(payload.utf8String.toUpperCase)
        val outputTopic = "output"
        val publishFlags = PublishQoSFlags.QoSAtLeastOnceDelivery | ControlPacketFlags.RETAIN
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
    // or alternatively
    // sourceClient.commandQueue.offer(Command[Nothing](Subscribe("test")))

    // after some time, shutdown the clients
    Thread.sleep(60000)
    sourceClient.shutdown()
    sinkClient.shutdown()
    println("done")
  }
}