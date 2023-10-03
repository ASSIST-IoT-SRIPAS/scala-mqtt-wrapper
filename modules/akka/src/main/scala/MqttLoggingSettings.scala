package pl.waw.ibspan.scala_mqtt_wrapper.akka


import akka.event.Logging
import akka.stream.Attributes

/** MQTT logging settings
  *
  * @param name
  *   name used in log messages
  * @param attributes
  *   attributes with log levels
  */
final case class MqttLoggingSettings(
    name: String = "",
    attributes: Attributes = Attributes.logLevels(
      onElement = Logging.InfoLevel,
      onFinish = Logging.InfoLevel,
      onFailure = Logging.ErrorLevel,
    ),
)
