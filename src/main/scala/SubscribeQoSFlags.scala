package pl.waw.ibspan.scala_mqtt_wrapper

import org.apache.pekko.stream.connectors.mqtt.streaming.ControlPacketFlags

object SubscribeQoSFlags {
  val QoSAtMostOnceDelivery: ControlPacketFlags = ControlPacketFlags(0)
  val QoSAtLeastOnceDelivery: ControlPacketFlags = ControlPacketFlags(1)
}
