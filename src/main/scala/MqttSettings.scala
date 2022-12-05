package pl.waw.ibspan.scala_mqtt_wrapper

import akka.stream.alpakka.mqtt.streaming.ConnectFlags
import akka.util.ByteString

import utils.Uuid.generateUuid

final case class MqttSettings(
    host: String,
    port: Int,
    username: String = "",
    password: String = "",
    topics: Seq[MqttTopic] = Seq.empty[MqttTopic],
    clientId: String = generateUuid.toString,
    sessionId: ByteString = ByteString(generateUuid.toString),
    connectFlags: ConnectFlags = ConnectFlags.CleanSession
)
