package pl.waw.ibspan.scala_mqtt_wrapper

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.alpakka.mqtt.streaming.Command
import akka.stream.alpakka.mqtt.streaming.ControlPacketFlags
import akka.stream.alpakka.mqtt.streaming.Publish
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Keep
import akka.stream.scaladsl.Sink
import akka.util.ByteString

class MqttSink(mqttSettings: MqttSettings)(implicit system: ActorSystem[_]) {
  val mqttClient: MqttClient = new MqttClient(mqttSettings)
  val flow: Sink[(ByteString, String, ControlPacketFlags), NotUsed] =
    Flow[(ByteString, String, ControlPacketFlags)]
      .collect { case (msg, topic, publishFlags) =>
        publish(msg, topic, publishFlags)
      }
      .concatMat(mqttClient.flow)(Keep.none)
      .to(Sink.ignore)

  def publish(msg: ByteString, topic: String, publishFlags: ControlPacketFlags): Unit = {
    val command = Command[Nothing](Publish(publishFlags, topic, msg))
    mqttClient.session ! command
  }
}
