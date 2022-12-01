package pl.waw.ibspan.scala_mqtt_wrapper

import akka.stream.alpakka.mqtt.streaming.ControlPacketFlags

final case class MqttTopic(
    name: String,
    controlPacketFlags: Seq[ControlPacketFlags]
)
