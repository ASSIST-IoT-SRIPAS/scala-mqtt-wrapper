package pl.waw.ibspan.scala_mqtt_wrapper

import akka.event.Logging
import akka.stream.alpakka.mqtt.streaming.ConnectFlags
import akka.util.ByteString

import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

import utils.Uuid.generateUuid

final case class MqttSettings(
    host: String,
    port: Int,
    username: String = "",
    password: String = "",
    topics: Seq[MqttTopic] = Seq.empty[MqttTopic],
    clientId: String = generateUuid.toString,
    sessionId: ByteString = ByteString(generateUuid.toString),
    connectFlags: ConnectFlags = ConnectFlags.CleanSession,
    restartMinBackoff: FiniteDuration = 1.seconds,
    restartMaxBackoff: FiniteDuration = 30.seconds,
    restartRandomFactor: Double = 0.2,
    maxRestarts: Int = -1,
    restartLogLevel: Logging.LogLevel = Logging.WarningLevel
)
