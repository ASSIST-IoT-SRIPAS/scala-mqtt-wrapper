package pl.waw.ibspan.scala_mqtt_wrapper

import akka.Done
import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.KillSwitches
import akka.stream.RestartSettings
import akka.stream.alpakka.mqtt.streaming.Command
import akka.stream.alpakka.mqtt.streaming.Connect
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
  * @param loggingSettings
  *   optional logging settings
  * @param system
  *   actor system
  */
class MqttClient(
    val mqttSettings: MqttSettings,
    val mqttSessionSettings: MqttSessionSettings = MqttSessionSettings(),
    val loggingSettings: Option[MqttLoggingSettings] = None,
)(implicit system: ActorSystem[_])
    extends LazyLogging {
  // name used for logging
  val name: String = loggingSettings.fold("")(_.name)

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
        password = mqttSettings.password,
      )
    )
  val subscribeCommands: List[Command[Nothing]] =
    mqttSettings.topics.map(topic => Command[Nothing](Subscribe(topic.name))).toList
  val initialCommands: List[Command[Nothing]] = connectCommand :: subscribeCommands

  // create a command merge sink that accepts client commands
  // note that the commands sent to the sink are not persisted between restarts
  // received commands are broadcast via command broadcast hub
  // the kill switch is used to stop the merge-broadcast stream
  // while deciding on the buffer sizes note that there is at least one broadcast consumer
  // (while the MQTT session flow is running; i.e. it is not restarting)
  val ((commandMergeSink, commandMergeSinkKillSwitch), commandBroadcastSource) = MergeHub
    .source[Command[Nothing]](perProducerBufferSize =
      mqttSettings.commandMergeSinkPerProducerBufferSize
    )
    .viaMat(KillSwitches.single)(Keep.both)
    .toMat(BroadcastHub.sink(bufferSize = mqttSettings.commandBroadcastSourceBufferSize))(Keep.both)
    .run()

  // create settings for restarting the MQTT event source
  val restartingEventSourceSettings: RestartSettings = RestartSettings(
    minBackoff = mqttSettings.restartMinBackoff,
    maxBackoff = mqttSettings.restartMaxBackoff,
    randomFactor = mqttSettings.restartRandomFactor,
  )
    .withMaxRestarts(mqttSettings.maxRestarts, mqttSettings.restartMinBackoff)
    .withLogSettings(RestartSettings.createLogSettings(logLevel = mqttSettings.restartLogLevel))

  // create the MQTT source that restarts on failure
  // first, the initial commands are sent to the broker
  // then the command broadcast is connected to the MQTT session flow
  // the MQTT session flow produces MQTT event
  val restartingEventSource: Source[Either[MqttCodec.DecodeError, Event[Nothing]], NotUsed] =
    RestartSource.withBackoff(restartingEventSourceSettings) { () =>
      val source = Source(initialCommands)
        .concatMat(commandBroadcastSource)(Keep.right)
        .via(sessionFlow)
      loggingSettings.fold(source)(settings =>
        source.log(settings.name, event => s"event [$event]").addAttributes(settings.attributes)
      )
    }

  // create event broadcast source that broadcasts the MQTT events
  // the kill switch is used to stop the MQTT session flow
  // the buffer size is not that important in case a consumer (with `Sink.ignore`) is created
  val (eventBroadcastSourceKillSwitch, eventBroadcastSource) = {
    restartingEventSource
      .viaMat(KillSwitches.single)(Keep.right)
      .toMat(BroadcastHub.sink(bufferSize = mqttSettings.eventBroadcastSourceBufferSize))(Keep.both)
      .run()
  }
  val eventBroadcastConsumerFuture: Option[Future[Done]] =
    if (!mqttSettings.withEventBroadcastSourceBackpressure) {
      // this consumer ensures that the event broadcast does not apply backpressure
      // on the session flow after reaching the event broadcast buffer size
      val future: Future[Done] = eventBroadcastSource.runWith(Sink.ignore)
      future.onComplete(_ => logger.debug(s"[$name] Event broadcast consumer shutdown"))(
        system.executionContext
      )
      Some(future)
    } else {
      None
    }

  // helper broadcast source that collects only MQTT publish events
  val publishEventBroadcastSource: Source[MqttReceivedMessage, NotUsed] = eventBroadcastSource
    .collect { case Right(Event(p: Publish, _)) =>
      MqttReceivedMessage(p.payload, p.topicName)
    }

  // create a publish sink (a merge hub) that publishes messages to the MQTT broker
  // a kill switch is used to kill the merge hub
  // as it is not stopped when the MQTT session flow is stopped
  val ((publishMergeSink, publishMergeSinkKillSwitch), publishMergeSinkFuture) = MergeHub
    .source[MqttPublishMessage](perProducerBufferSize =
      mqttSettings.publishMergeSinkPerProducerBufferSize
    )
    .viaMat(KillSwitches.single)(Keep.both)
    .map { case MqttPublishMessage(payload, topic, publishFlags) =>
      session ! Command[Nothing](Publish(publishFlags, topic, payload))
    }
    .toMat(Sink.ignore)(Keep.both)
    .run()
  publishMergeSinkFuture.onComplete(_ => logger.debug(s"[$name] Publish merge sink shutdown"))(
    system.executionContext
  )

  /** Shutdown the client
    *
    * Shutdowns command merge sink / command broadcast source, publish merge sink, and event
    * broadcast source (along with the MQTT session)
    *
    * @return
    *   a future that completes after closing publish merge sink, and the (optional) event broadcast
    *   consumer (if option withEventBroadcastSourceBackpressure is false)
    */
  def shutdown(): Future[Done] = {
    commandMergeSinkKillSwitch.shutdown()
    publishMergeSinkKillSwitch.shutdown()
    eventBroadcastSourceKillSwitch.shutdown()
    val eventBroadcastConsumerFutureDone: Future[Done] = eventBroadcastConsumerFuture match {
      case Some(future) => future
      case None         => Future.successful(Done)
    }
    Future.reduceLeft(Seq(publishMergeSinkFuture, eventBroadcastConsumerFutureDone))((_, _) =>
      Done
    )(
      system.executionContext
    )
  }
}
