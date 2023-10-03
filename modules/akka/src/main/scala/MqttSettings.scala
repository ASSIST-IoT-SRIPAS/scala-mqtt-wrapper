package pl.waw.ibspan.scala_mqtt_wrapper.akka

import akka.event.Logging
import akka.stream.alpakka.mqtt.streaming.ConnectFlags
import akka.util.ByteString
import pl.waw.ibspan.scala_mqtt_wrapper.utils.Uuid.generateUuid

import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

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
  * @param subscriptions
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
  * @param commandMergeSinkPerProducerBufferSize
  *   buffer space used per producer for command sink merge hub
  * @param commandBroadcastSourceBufferSize
  *   MQTT commands broadcast buffer size, must be a power of 2
  * @param eventBroadcastSourceBufferSize
  *   MQTT events broadcast buffer size, must be a power of 2
  * @param withEventBroadcastSourceBackpressure
  *   if true, the event broadcast will store events in a buffer until there is demand and
  *   eventually apply backpressure on the MQTT session flow; if false, the events will be always
  *   consumed even if there is no external demand
  * @param publishMergeSinkPerProducerBufferSize
  *   buffer space used per producer for publish merge sink
  * @param commandQueueBufferSize
  *   MQTT command queue buffer size
  */
final case class MqttSettings(
    host: String,
    port: Int,
    username: String = "",
    password: String = "",
    subscriptions: Seq[MqttTopic] = Seq.empty[MqttTopic],
    clientId: String = generateUuid.toString,
    sessionId: ByteString = ByteString(generateUuid.toString),
    connectFlags: ConnectFlags = ConnectFlags.CleanSession,
    restartMinBackoff: FiniteDuration = 1.seconds,
    restartMaxBackoff: FiniteDuration = 30.seconds,
    restartRandomFactor: Double = 0.2,
    maxRestarts: Int = -1,
    restartLogLevel: Logging.LogLevel = Logging.WarningLevel,
    commandMergeSinkPerProducerBufferSize: Int = 16,
    commandBroadcastSourceBufferSize: Int = 128,
    eventBroadcastSourceBufferSize: Int = 128,
    withEventBroadcastSourceBackpressure: Boolean = false,
    publishMergeSinkPerProducerBufferSize: Int = 32,
    commandQueueBufferSize: Int = 128,
)
