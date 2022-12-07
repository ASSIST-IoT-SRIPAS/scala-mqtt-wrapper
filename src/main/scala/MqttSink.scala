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

object MqttSink extends LazyLogging {

  /** Create MQTT sink
    *
    * Sink publishes consumed messages to the MQTT broker
    *
    * @param mqttClient
    *   MQTT client
    * @param system
    *   actor system
    */
  def sink(
      mqttClient: MqttClient
  )(implicit system: ActorSystem[_]): Sink[(ByteString, String, ControlPacketFlags), NotUsed] =
    Flow[(ByteString, String, ControlPacketFlags)]
      .wireTap(data =>
        logger.debug(
          "[%s] Sending message [%s] to topic [%s]"
            .format(mqttClient.name, data._1.utf8String, data._2)
        )
      )
      .map { case (msg, topic, publishFlags) =>
        mqttClient.session ! Command[Nothing](Publish(publishFlags, topic, msg))
      }
      .to(Sink.ignore)
}
