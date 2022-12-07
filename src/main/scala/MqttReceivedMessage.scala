package pl.waw.ibspan.scala_mqtt_wrapper

import akka.util.ByteString

/** data received from the MQTT broker
  * @param payload
  *   received payload
  * @param topic
  *   topic from which the payload was received
  */
final case class MqttReceivedMessage(
    payload: ByteString,
    topic: String,
)
