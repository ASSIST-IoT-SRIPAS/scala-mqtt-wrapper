package pl.waw.ibspan.scala_mqtt_wrapper

import akka.stream.alpakka.mqtt.streaming.ControlPacketFlags
import akka.util.ByteString

/** MQTT data to be published
  *
  * @param payload
  *   payload to be published
  * @param topic
  *   topic to which the payload will be published
  * @param publishFlags
  *   MQTT publish flags
  */
final case class MqttPublishMessage(
    payload: ByteString,
    topic: String,
    publishFlags: ControlPacketFlags = ControlPacketFlags.None,
)
