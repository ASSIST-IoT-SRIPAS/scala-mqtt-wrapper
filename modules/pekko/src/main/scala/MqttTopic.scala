package pl.waw.ibspan.scala_mqtt_wrapper.pekko

import org.apache.pekko.stream.connectors.mqtt.streaming.ControlPacketFlags

/** MQTT topic
  *
  * @param name
  *   topic name
  * @param flags
  *   MQTT flags
  */
final case class MqttTopic(
    name: String,
    flags: ControlPacketFlags = SubscribeQoSFlags.QoSAtMostOnceDelivery,
)
