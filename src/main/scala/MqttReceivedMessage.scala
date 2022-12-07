package pl.waw.ibspan.scala_mqtt_wrapper

import akka.util.ByteString

/** data received from the MQTT broker
  * @param message
  *   received message
  * @param topic
  *   topic from which the message was received
  */
final case class MqttReceivedMessage(
    message: ByteString,
    topic: String,
)
