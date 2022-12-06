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

class MqttClient(
    val mqttSettings: MqttSettings,
    val mqttSessionSettings: MqttSessionSettings = MqttSessionSettings(),
    val name: String = ""
)(implicit system: ActorSystem[_])
    extends LazyLogging {
  // mqtt session
  val session: ActorMqttClientSession = ActorMqttClientSession(mqttSessionSettings)
  val tcpConnection: Flow[ByteString, ByteString, Future[Tcp.OutgoingConnection]] =
    Tcp(system).outgoingConnection(mqttSettings.host, mqttSettings.port)
  val sessionFlow: Flow[Command[Nothing], Either[MqttCodec.DecodeError, Event[Nothing]], NotUsed] =
    Mqtt
      .clientSessionFlow(session, mqttSettings.sessionId)
      .join(tcpConnection)

  // initial commands
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

  // commands queue
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

  // events source
  val restartingEventsSourceSettings: RestartSettings = RestartSettings(
    minBackoff = mqttSettings.restartMinBackoff,
    maxBackoff = mqttSettings.restartMaxBackoff,
    randomFactor = mqttSettings.restartRandomFactor
  )
    .withMaxRestarts(mqttSettings.maxRestarts, mqttSettings.restartMinBackoff)
    .withLogSettings(RestartSettings.createLogSettings(logLevel = mqttSettings.restartLogLevel))
  val restartingEventsSource: Source[Either[MqttCodec.DecodeError, Event[Nothing]], NotUsed] =
    RestartSource.withBackoff(restartingEventsSourceSettings) { () =>
      for (command <- initialCommands) {
        val queueResult = commands.offer(command)
        queueResult.map {
          case QueueOfferResult.Enqueued =>
            logger.debug(s"[$name] Command [$command] enqueued")
          case QueueOfferResult.Dropped =>
            logger.warn(s"[$name] Command [$command] dropped")
          case QueueOfferResult.Failure(exception) =>
            logger.error(s"[$name] Command [$command] failed [$exception]")
          case QueueOfferResult.QueueClosed =>
            logger.warn(s"[$name] Command [$command] queue closed")
        }(system.executionContext)
      }
      commandsBroadcast
        .via(sessionFlow)
        .wireTap(event => logger.debug(s"[$name] Received event $event"))
    }
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

  def shutdown(): Future[Done] = {
    commands.complete()
    eventsBroadcastKillSwitch.shutdown()
    Future.reduceLeft(Seq(commands.watchCompletion(), eventsBroadcastFuture))((_, _) => Done)(
      system.executionContext
    )
  }
}
