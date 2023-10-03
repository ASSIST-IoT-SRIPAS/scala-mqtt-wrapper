package pl.waw.ibspan.scala_mqtt_wrapper.akka

import akka.stream.alpakka.mqtt.streaming.ControlPacketFlags

object PublishQoSFlags {
  val QoSAtMostOnceDelivery: ControlPacketFlags = ControlPacketFlags(0)
  val QoSAtLeastOnceDelivery: ControlPacketFlags = ControlPacketFlags(2)
  val QoSExactlyOnceDelivery: ControlPacketFlags = ControlPacketFlags(4)
}
