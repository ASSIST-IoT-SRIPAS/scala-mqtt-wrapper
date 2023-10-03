package pl.waw.ibspan.scala_mqtt_wrapper.pekko

import org.apache.pekko.stream.connectors.mqtt.streaming.ControlPacketFlags
import org.apache.pekko.stream.connectors.mqtt.streaming.PacketId
import org.apache.pekko.util.ByteString

/** data received from the MQTT broker
  * @param payload
  *   received payload
  * @param topic
  *   topic from which the payload was received
  * @param flags
  *   MQTT flags
  * @param packetId
  *   MQTT packet id
  */
final case class MqttReceivedMessage(
    payload: ByteString,
    topic: String,
    flags: ControlPacketFlags,
    packetId: Option[PacketId],
)
