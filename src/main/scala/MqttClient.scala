package pl.waw.ibspan.scala_mqtt_wrapper

import akka.Done
import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.KillSwitches
import akka.stream.OverflowStrategy
import akka.stream.QueueOfferResult
import akka.stream.RestartSettings
import akka.stream.alpakka.mqtt.streaming.Command
import akka.stream.alpakka.mqtt.streaming.Connect
import akka.stream.alpakka.mqtt.streaming.Event
import akka.stream.alpakka.mqtt.streaming.MqttCodec
import akka.stream.alpakka.mqtt.streaming.MqttSessionSettings
import akka.stream.alpakka.mqtt.streaming.Subscribe
import akka.stream.alpakka.mqtt.streaming.scaladsl.ActorMqttClientSession
import akka.stream.alpakka.mqtt.streaming.scaladsl.Mqtt
import akka.stream.scaladsl.BroadcastHub
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Keep
import akka.stream.scaladsl.RestartSource
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Tcp
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Future

/** MQTT client
  *
  * @constructor
  *   create a new MQTT client
  * @param mqttSettings
  *   MQTT client settings
  * @param mqttSessionSettings
  *   MQTT session settings
  * @param name
  *   MQTT client name used for logging
  * @param system
  *   actor system
  */
class MqttClient(
    val mqttSettings: MqttSettings,
    val mqttSessionSettings: MqttSessionSettings = MqttSessionSettings(),
    val name: String = ""
)(implicit system: ActorSystem[_])
    extends LazyLogging {
  // prepare MQTT client session
  val session: ActorMqttClientSession = ActorMqttClientSession(mqttSessionSettings)
  val tcpConnection: Flow[ByteString, ByteString, Future[Tcp.OutgoingConnection]] =
    Tcp(system).outgoingConnection(mqttSettings.host, mqttSettings.port)
  val sessionFlow: Flow[Command[Nothing], Either[MqttCodec.DecodeError, Event[Nothing]], NotUsed] =
    Mqtt
      .clientSessionFlow(session, mqttSettings.sessionId)
      .join(tcpConnection)

  // prepare initial commands
  // the first one connects the client to the broker
  // the consecutive commands subscribe to topics
  val connectCommand: Command[Nothing] =
    Command[Nothing](
      Connect(
        clientId = mqttSettings.clientId,
        extraConnectFlags = mqttSettings.connectFlags,
        username = mqttSettings.username,
        password = mqttSettings.password
      )
    )
  val subscribeCommands: List[Command[Nothing]] =
    mqttSettings.topics.map(topic => Command[Nothing](Subscribe(topic.name))).toList
  val initialCommands: List[Command[Nothing]] = connectCommand :: subscribeCommands

  // create a queue `commands` that accepts client commands
  // the commands sent to the queue are not persisted between restarts
  // `commandsBroadcast` broadcasts the queue commands
  val (commands, commandsBroadcast) = Source
    .queue[Command[Nothing]](
      bufferSize = mqttSettings.commandsBroadcastBufferSize,
      overflowStrategy = OverflowStrategy.backpressure
    )
    .toMat(BroadcastHub.sink(bufferSize = mqttSettings.commandsBroadcastBufferSize))(Keep.both)
    .run()
  commands
    .watchCompletion()
    .onComplete(_ => logger.debug(s"[$name] Commands queue completed"))(system.executionContext)

  // create settings for restarting the MQTT events source
  val restartingEventsSourceSettings: RestartSettings = RestartSettings(
    minBackoff = mqttSettings.restartMinBackoff,
    maxBackoff = mqttSettings.restartMaxBackoff,
    randomFactor = mqttSettings.restartRandomFactor
  )
    .withMaxRestarts(mqttSettings.maxRestarts, mqttSettings.restartMinBackoff)
    .withLogSettings(RestartSettings.createLogSettings(logLevel = mqttSettings.restartLogLevel))
  // create the MQTT source that restarts on failure
  // first, the initial commands are sent to the broker
  // then the command broadcast is connected to the MQTT session flow
  // the MQTT session flow produces MQTT events
  val restartingEventsSource: Source[Either[MqttCodec.DecodeError, Event[Nothing]], NotUsed] =
    RestartSource.withBackoff(restartingEventsSourceSettings) { () =>
      Source(initialCommands)
        .concatMat(commandsBroadcast)(Keep.right)
        .via(sessionFlow)
        .wireTap(event => logger.debug(s"[$name] Received event $event"))
    }
  // create `eventsBroadcast` that broadcasts the MQTT events
  // and a kill switch that can be used to stop the MQTT session flow
  // `bufferSize` is not that important as a consumer (with `Sink.ignore`) is created immediately
  val (eventsBroadcastKillSwitch, eventsBroadcast) = {
    restartingEventsSource
      .viaMat(KillSwitches.single)(Keep.right)
      .toMat(BroadcastHub.sink(bufferSize = mqttSettings.eventsBroadcastBufferSize))(Keep.both)
      .run()
  }
  val eventsBroadcastFuture: Future[Done] = eventsBroadcast.runWith(Sink.ignore)
  eventsBroadcastFuture.onComplete(_ => logger.debug(s"[$name] Events broadcast shutdown"))(
    system.executionContext
  )

  /** Shutdown the client
    *
    * @return
    *   a future that completes after stopping the MQTT session and closing the command queue
    */
  def shutdown(): Future[Done] = {
    commands.complete()
    eventsBroadcastKillSwitch.shutdown()
    Future.reduceLeft(Seq(commands.watchCompletion(), eventsBroadcastFuture))((_, _) => Done)(
      system.executionContext
    )
  }
}
