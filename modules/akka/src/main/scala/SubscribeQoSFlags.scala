package pl.waw.ibspan.scala_mqtt_wrapper.akka

import akka.stream.alpakka.mqtt.streaming.ControlPacketFlags

object SubscribeQoSFlags {
  val QoSAtMostOnceDelivery: ControlPacketFlags = ControlPacketFlags(0)
  val QoSAtLeastOnceDelivery: ControlPacketFlags = ControlPacketFlags(1)
}
