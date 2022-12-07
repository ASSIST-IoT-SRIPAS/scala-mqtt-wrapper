package pl.waw.ibspan.scala_mqtt_wrapper

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging

object MqttSource extends LazyLogging {

  /** Create MQTT source
    *
    * Source consumes messages from the MQTT broker from subscribed topics
    *
    * @param mqttClient
    *   MQTT client
    * @param system
    *   actor system
    */
  def source(mqttClient: MqttClient)(implicit
      system: ActorSystem[_]
  ): Source[(ByteString, String), NotUsed] = mqttClient.publishEventBroadcast
    // TODO: use .log() instead
    .wireTap(data =>
      logger.debug(
        "[%s] Received message [%s] from topic [%s]"
          .format(mqttClient.name, data._1.utf8String, data._2)
      )
    )
}
