package pl.waw.ibspan.scala_mqtt_wrapper.akka

import akka.NotUsed
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink

object MqttSink {

  /** Create MQTT sink
    *
    * Sink publishes consumed messages to the MQTT broker
    *
    * @param mqttClient
    *   MQTT client
    * @param loggingSettings
    *   optional logging settings
    */
  def sink(
      mqttClient: MqttClient,
      loggingSettings: Option[MqttLoggingSettings] = None,
  ): Sink[MqttPublishMessage, NotUsed] = {
    loggingSettings.fold(mqttClient.publishMergeSink) { settings =>
      val name = s"${mqttClient.name} : ${settings.name}"
      Flow[MqttPublishMessage]
        .log(
          name,
          data =>
            s"payload [${data.payload.utf8String}] to topic [${data.topic}] with flags [${data.flags}]",
        )
        .addAttributes(settings.attributes)
        .to(mqttClient.publishMergeSink)
    }
  }
}
