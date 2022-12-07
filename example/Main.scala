import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.alpakka.mqtt.streaming.Command
import akka.stream.alpakka.mqtt.streaming.ControlPacketFlags
import akka.stream.alpakka.mqtt.streaming.Subscribe
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Source
import akka.util.ByteString

import pl.waw.ibspan.scala_mqtt_wrapper.MqttClient
import pl.waw.ibspan.scala_mqtt_wrapper.MqttSettings
import pl.waw.ibspan.scala_mqtt_wrapper.MqttSink
import pl.waw.ibspan.scala_mqtt_wrapper.MqttSource
import pl.waw.ibspan.scala_mqtt_wrapper.MqttTopic

object Main {
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem[Nothing] = ActorSystem[Nothing](
      Behaviors.setup[Nothing] { context =>
        Behaviors.empty
      },
      name = "scalaMqttWrapper",
    )

    val sourceClient = new MqttClient(
      MqttSettings(
        host = "mosquitto",
        port = 1883,
        topics = Seq(MqttTopic("input")),
      ),
      name = "sourceClient",
    )
    val source = MqttSource.source(sourceClient)

    val sinkClient = new MqttClient(
      MqttSettings(
        host = "mosquitto",
        port = 1883,
      ),
      name = "sinkClient",
    )
    val sink = MqttSink.sink(sinkClient)

    val uppercaseFlow = Flow[(ByteString, String)].map { case (msg, topic) =>
      val outputMessage = ByteString(msg.utf8String.toUpperCase)
      val outputTopic = "output"
      val publishFlags = ControlPacketFlags.QoSAtLeastOnceDelivery | ControlPacketFlags.RETAIN
      println(
        s"source [$topic] ${msg.utf8String} --> sink [$outputTopic] ${outputMessage.utf8String}"
      )
      (outputMessage, outputTopic, publishFlags)
    }

    source
      .via(uppercaseFlow)
      .runWith(sink)

    Source
      .single(Command[Nothing](Subscribe("test")))
      .runWith(sourceClient.commandMergeSink)

    Thread.sleep(60000)
    sourceClient.shutdown()
    sinkClient.shutdown()
    println("done")
  }
}
