package pl.waw.ibspan.scala_mqtt_wrapper

import akka.Done
import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.KillSwitches
import akka.stream.OverflowStrategy
import akka.stream.RestartSettings
import akka.stream.alpakka.mqtt.streaming.Command
import akka.stream.alpakka.mqtt.streaming.Connect
import akka.stream.alpakka.mqtt.streaming.ControlPacketFlags
import akka.stream.alpakka.mqtt.streaming.Event
import akka.stream.alpakka.mqtt.streaming.MqttCodec
import akka.stream.alpakka.mqtt.streaming.MqttSessionSettings
import akka.stream.alpakka.mqtt.streaming.Publish
import akka.stream.alpakka.mqtt.streaming.Subscribe
import akka.stream.alpakka.mqtt.streaming.scaladsl.ActorMqttClientSession
import akka.stream.alpakka.mqtt.streaming.scaladsl.Mqtt
import akka.stream.scaladsl.BroadcastHub
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Keep
import akka.stream.scaladsl.MergeHub
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

  // create a queue that accepts client commands
  // note that the commands sent to the queue are not persisted between restarts
  // `commandQueueBroadcast` broadcasts the queue commands
  val (commandQueue, commandQueueBroadcast) = Source
    .queue[Command[Nothing]](
      bufferSize = mqttSettings.commandBroadcastBufferSize,
      overflowStrategy = OverflowStrategy.backpressure
    )
    .toMat(BroadcastHub.sink(bufferSize = mqttSettings.commandBroadcastBufferSize))(Keep.both)
    .run()
  commandQueue
    .watchCompletion()
    .onComplete(_ => logger.debug(s"[$name] Command queue completed"))(system.executionContext)

  // create settings for restarting the MQTT event source
  val restartingEventSourceSettings: RestartSettings = RestartSettings(
    minBackoff = mqttSettings.restartMinBackoff,
    maxBackoff = mqttSettings.restartMaxBackoff,
    randomFactor = mqttSettings.restartRandomFactor
  )
    .withMaxRestarts(mqttSettings.maxRestarts, mqttSettings.restartMinBackoff)
    .withLogSettings(RestartSettings.createLogSettings(logLevel = mqttSettings.restartLogLevel))
  // create the MQTT source that restarts on failure
  // first, the initial commands are sent to the broker
  // then the command broadcast is connected to the MQTT session flow
  // the MQTT session flow produces MQTT event
  val restartingEventSource: Source[Either[MqttCodec.DecodeError, Event[Nothing]], NotUsed] =
    RestartSource.withBackoff(restartingEventSourceSettings) { () =>
      Source(initialCommands)
        .concatMat(commandQueueBroadcast)(Keep.right)
        .via(sessionFlow)
        .wireTap(event => logger.debug(s"[$name] Received event $event"))
    }
  // create `eventBroadcast` that broadcasts the MQTT events
  // and a kill switch that can be used to stop the MQTT session flow
  // `bufferSize` is not that important as a consumer (with `Sink.ignore`) is created immediately
  val (eventBroadcastKillSwitch, eventBroadcast) = {
    restartingEventSource
      .viaMat(KillSwitches.single)(Keep.right)
      .toMat(BroadcastHub.sink(bufferSize = mqttSettings.eventBroadcastBufferSize))(Keep.both)
      .run()
  }
  // this consumer ensures that the event broadcast does not apply backpressure
  val eventBroadcastFuture: Future[Done] = eventBroadcast.runWith(Sink.ignore)
  eventBroadcastFuture.onComplete(_ => logger.debug(s"[$name] Event broadcast shutdown"))(
    system.executionContext
  )
  // helper broadcast that collects only MQTT publish events
  val publishEventBroadcast: Source[(ByteString, String), NotUsed] = eventBroadcast
    .collect { case Right(Event(p: Publish, _)) =>
      (p.payload, p.topicName)
    }

  // create a publish sink (a merge hub) that publishes messages to the MQTT broker
  // a kill switch is used to kill the merge hub
  // as it is not stopped when the MQTT session flow is stopped
  val ((publishSink, publishSinkKillSwitch), publishSinkFuture) = MergeHub
    .source[(ByteString, String, ControlPacketFlags)](perProducerBufferSize =
      mqttSettings.publishSinkPerProducerBufferSize
    )
    .viaMat(KillSwitches.single)(Keep.both)
    .map { case (msg, topic, publishFlags) =>
      session ! Command[Nothing](Publish(publishFlags, topic, msg))
    }
    .toMat(Sink.ignore)(Keep.both)
    .run()
  publishSinkFuture.onComplete(_ => logger.debug(s"[$name] Publish sink shutdown"))(
    system.executionContext
  )

  /** Shutdown the client
    *
    * @return
    *   a future that completes after closing the command queue, closing the publish sink, and
    *   stopping the MQTT session
    */
  def shutdown(): Future[Done] = {
    commandQueue.complete()
    publishSinkKillSwitch.shutdown()
    eventBroadcastKillSwitch.shutdown()
    Future.reduceLeft(Seq(commandQueue.watchCompletion(), publishSinkFuture, eventBroadcastFuture))(
      (_, _) => Done
    )(
      system.executionContext
    )
  }
}
