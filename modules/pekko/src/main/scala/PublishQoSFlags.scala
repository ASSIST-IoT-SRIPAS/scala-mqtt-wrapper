package pl.waw.ibspan.scala_mqtt_wrapper.pekko

import org.apache.pekko.stream.connectors.mqtt.streaming.ControlPacketFlags

object PublishQoSFlags {
  val QoSAtMostOnceDelivery: ControlPacketFlags = ControlPacketFlags(0)
  val QoSAtLeastOnceDelivery: ControlPacketFlags = ControlPacketFlags(2)
  val QoSExactlyOnceDelivery: ControlPacketFlags = ControlPacketFlags(4)
}
