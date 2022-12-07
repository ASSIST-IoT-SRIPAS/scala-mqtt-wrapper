package pl.waw.ibspan.scala_mqtt_wrapper

import akka.event.Logging
import akka.stream.alpakka.mqtt.streaming.ConnectFlags
import akka.util.ByteString

import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

import utils.Uuid.generateUuid

/** MQTT client settings
  *
  * @param host
  *   MQTT host
  * @param port
  *   MQTT port
  * @param username
  *   MQTT username
  * @param password
  *   MQTT password
  * @param topics
  *   MQTT topics to subscribe to
  * @param clientId
  *   MQTT client ID
  * @param sessionId
  *   MQTT session ID
  * @param connectFlags
  *   MQTT connect flags
  * @param restartMinBackoff
  *   MQTT restart minimum backoff
  * @param restartMaxBackoff
  *   MQTT restart maximum backoff
  * @param restartRandomFactor
  *   MQTT restart random factor, random delay is added based on this factor
  * @param maxRestarts
  *   MQTT maximum restarts, set to -1 for infinite restarts
  * @param restartLogLevel
  *   MQTT restart log level
  * @param commandsBroadcastBufferSize
  *   MQTT commands broadcast buffer size, must be a power of 2
  * @param eventsBroadcastBufferSize
  *   MQTT events broadcast buffer size, must be a power of 2
  * @param publishSinkPerProducerBufferSize
  *   buffer space used per producer for publish sink
  */
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
    restartLogLevel: Logging.LogLevel = Logging.WarningLevel,
    commandsBroadcastBufferSize: Int = 128,
    eventsBroadcastBufferSize: Int = 128,
    publishSinkPerProducerBufferSize: Int = 32
)
