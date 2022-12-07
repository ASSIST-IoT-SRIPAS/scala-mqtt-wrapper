package pl.waw.ibspan.scala_mqtt_wrapper

import akka.NotUsed
import akka.stream.alpakka.mqtt.streaming.ControlPacketFlags
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
    */
  def sink(mqttClient: MqttClient): Sink[(ByteString, String, ControlPacketFlags), NotUsed] =
    Flow[(ByteString, String, ControlPacketFlags)]
      // TODO: use .log() instead
      .wireTap(data =>
        logger.debug(
          "[%s] Sending message [%s] to topic [%s]"
            .format(mqttClient.name, data._1.utf8String, data._2)
        )
      )
      .to(mqttClient.publishMergeSink)
}
