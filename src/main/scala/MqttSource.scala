package pl.waw.ibspan.scala_mqtt_wrapper

import akka.NotUsed
import akka.stream.scaladsl.Source

object MqttSource {

  /** Create MQTT source
    *
    * Source consumes messages from the MQTT broker from subscribed topics
    *
    * @param mqttClient
    *   MQTT client
    * @param loggingSettings
    *   optional logging settings
    */
  def source(
      mqttClient: MqttClient,
      loggingSettings: Option[MqttLoggingSettings] = None,
  ): Source[MqttReceivedMessage, NotUsed] = {
    loggingSettings.fold(mqttClient.publishEventBroadcastSource) { settings =>
      val name = s"${mqttClient.name} : ${settings.name}"
      mqttClient.publishEventBroadcastSource
        .log(
          name,
          data =>
            s"payload [${data.payload.utf8String}] from topic [${data.topic}] with flags [${data.flags}] and packet id [${data.packetId}]",
        )
        .addAttributes(settings.attributes)
    }
  }
}
