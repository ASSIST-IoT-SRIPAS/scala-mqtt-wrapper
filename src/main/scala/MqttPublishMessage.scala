package pl.waw.ibspan.scala_mqtt_wrapper

import akka.stream.alpakka.mqtt.streaming.ControlPacketFlags
import akka.util.ByteString

/** MQTT data to be published
  *
  * @param message
  *   message to be published
  * @param topic
  *   topic to which the message will be published
  * @param publishFlags
  *   MQTT publish flags
  */
final case class MqttPublishMessage(
    message: ByteString,
    topic: String,
    publishFlags: ControlPacketFlags = ControlPacketFlags.None,
)
