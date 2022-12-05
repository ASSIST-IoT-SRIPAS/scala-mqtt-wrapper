package pl.waw.ibspan.scala_mqtt_wrapper

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.alpakka.mqtt.streaming.Command
import akka.stream.alpakka.mqtt.streaming.ControlPacketFlags
import akka.stream.alpakka.mqtt.streaming.Publish
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging

class MqttSink(mqttSettings: MqttSettings)(implicit system: ActorSystem[_]) extends LazyLogging {
  val mqttClient: MqttClient = new MqttClient(mqttSettings)
  mqttClient.source.runWith(Sink.ignore)
  val flow: Sink[(ByteString, String, ControlPacketFlags), NotUsed] =
    Flow[(ByteString, String, ControlPacketFlags)]
      .wireTap(data =>
        logger.debug(s"Sending message [${data._1.utf8String}] to topic [${data._2}]")
      )
      .map { case (msg, topic, publishFlags) =>
        mqttClient.session ! Command[Nothing](Publish(publishFlags, topic, msg))
      }
      .to(Sink.ignore)
}
