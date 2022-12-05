package pl.waw.ibspan.scala_mqtt_wrapper

import utils.Uuid.generateUuid

import akka.util.ByteString
import akka.stream.alpakka.mqtt.streaming.ConnectFlags

final case class MqttSettings(
    username: String,
    password: String,
    host: String,
    port: Int,
    topics: Seq[MqttTopic],
    clientId: String = generateUuid.toString,
    sessionId: ByteString = ByteString(generateUuid.toString),
    connectFlags: ConnectFlags = ConnectFlags.CleanSession
)
