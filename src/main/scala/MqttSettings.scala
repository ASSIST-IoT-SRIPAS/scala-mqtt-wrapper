package pl.waw.ibspan.scala_mqtt_wrapper

import utils.Uuid.generateUuid

import akka.util.ByteString

final case class MqttSettings(
    username: String,
    password: String,
    host: String,
    port: Int,
    topics: Seq[MqttTopic],
    sessionId: ByteString = ByteString(generateUuid.toString)
)
