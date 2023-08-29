package pl.waw.ibspan.scala_mqtt_wrapper

import org.apache.pekko.Done
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.stream.KillSwitches
import org.apache.pekko.stream.OverflowStrategy
import org.apache.pekko.stream.RestartSettings
import org.apache.pekko.stream.UniqueKillSwitch
import org.apache.pekko.stream.connectors.mqtt.streaming.Command
import org.apache.pekko.stream.connectors.mqtt.streaming.Connect
import org.apache.pekko.stream.connectors.mqtt.streaming.Event
import org.apache.pekko.stream.connectors.mqtt.streaming.MqttCodec
import org.apache.pekko.stream.connectors.mqtt.streaming.MqttSessionSettings
import org.apache.pekko.stream.connectors.mqtt.streaming.PubAck
import org.apache.pekko.stream.connectors.mqtt.streaming.Publish
import org.apache.pekko.stream.connectors.mqtt.streaming.Subscribe
import org.apache.pekko.stream.connectors.mqtt.streaming.scaladsl.ActorMqttClientSession
import org.apache.pekko.stream.connectors.mqtt.streaming.scaladsl.Mqtt
import org.apache.pekko.stream.scaladsl.BroadcastHub
import org.apache.pekko.stream.scaladsl.Flow
import org.apache.pekko.stream.scaladsl.Keep
import org.apache.pekko.stream.scaladsl.MergeHub
import org.apache.pekko.stream.scaladsl.RestartSource
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.stream.scaladsl.SourceQueueWithComplete
import org.apache.pekko.stream.scaladsl.Tcp
import org.apache.pekko.util.ByteString

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
)(implicit system: ActorSystem[_]) {
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
    mqttSettings.subscriptions
      .map(topic => Command[Nothing](Subscribe(Seq((topic.name, topic.flags)))))
      .toList
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

  // active command queue that accepts client commands
  val commandQueueSource: Source[Command[Nothing], SourceQueueWithComplete[Command[Nothing]]] =
    Source
      .queue[Command[Nothing]](mqttSettings.commandQueueBufferSize, OverflowStrategy.backpressure)
  val commandQueue: SourceQueueWithComplete[Command[Nothing]] = loggingSettings
    .fold(commandQueueSource)(settings =>
      commandQueueSource
        .log(settings.name + " : (internal) commandQueue", event => s"event [$event]")
        .addAttributes(settings.attributes)
    )
    .to(commandMergeSink)
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
        source
          .log(settings.name + " : (internal) restartingEventSource", event => s"event [$event]")
          .addAttributes(settings.attributes)
      )
    }

  // create event broadcast source that broadcasts the MQTT events
  // the kill switch is used to stop the MQTT session flow
  // the buffer size is not that important in case a consumer (with `Sink.ignore`) is created
  // it handles QoS 1
  // it does not handle QoS 2
  val (eventBroadcastSourceKillSwitch, eventBroadcastSource) = {
    restartingEventSource
      .map {
        case event @ Right(Event(Publish(_, _, Some(packetId), _), _)) =>
          commandQueue.offer(Command[Nothing](PubAck(packetId)))
          event
        case event => event
      }
      .viaMat(KillSwitches.single)(Keep.right)
      .toMat(BroadcastHub.sink(bufferSize = mqttSettings.eventBroadcastSourceBufferSize))(Keep.both)
      .run()
  }
  val eventBroadcastConsumerFuture: Option[Future[Done]] =
    if (!mqttSettings.withEventBroadcastSourceBackpressure) {
      // this consumer ensures that the event broadcast does not apply backpressure
      // on the session flow after reaching the event broadcast buffer size
      val source = loggingSettings.fold(eventBroadcastSource)(settings =>
        eventBroadcastSource
          .log(settings.name + " : (internal) eventBroadcastConsumer", event => s"event [$event]")
          .addAttributes(settings.attributes)
      )
      Some(source.runWith(Sink.ignore))
    } else {
      None
    }

  // helper broadcast source that collects only MQTT publish events
  val publishEventBroadcastSource: Source[MqttReceivedMessage, NotUsed] = eventBroadcastSource
    .collect { case Right(Event(p: Publish, _)) =>
      MqttReceivedMessage(p.payload, p.topicName, p.flags, p.packetId)
    }

  // create a publish sink (a merge hub) that publishes messages to the MQTT broker
  // a kill switch is used to kill the merge hub
  // as it is not stopped when the MQTT session flow is stopped
  val publishMergeSinkSource
      : Source[MqttPublishMessage, (Sink[MqttPublishMessage, NotUsed], UniqueKillSwitch)] = MergeHub
    .source[MqttPublishMessage](perProducerBufferSize =
      mqttSettings.publishMergeSinkPerProducerBufferSize
    )
    .viaMat(KillSwitches.single)(Keep.both)
  val publishMergeSinkSourceWithOptionalLogger
      : Source[MqttPublishMessage, (Sink[MqttPublishMessage, NotUsed], UniqueKillSwitch)] =
    loggingSettings
      .fold(publishMergeSinkSource)(settings =>
        publishMergeSinkSource
          .log(settings.name + " : (internal) publishMergeSink", event => s"event [$event]")
          .addAttributes(settings.attributes)
      )
  val ((publishMergeSink, publishMergeSinkKillSwitch), publishMergeSinkFuture) =
    publishMergeSinkSourceWithOptionalLogger
      .map { case MqttPublishMessage(payload, topic, publishFlags) =>
        session ! Command[Nothing](Publish(publishFlags, topic, payload))
      }
      .toMat(Sink.ignore)(Keep.both)
      .run()

  /** Shutdown the client
    *
    * Shutdowns command queue, command merge sink / command broadcast source, publish merge sink,
    * and event broadcast source (along with the MQTT session)
    *
    * @return
    *   a future that completes after closing command queue, publish merge sink, and the (optional)
    *   event broadcast consumer (if option withEventBroadcastSourceBackpressure is false)
    */
  def shutdown(): Future[Done] = {
    commandQueue.complete()
    commandMergeSinkKillSwitch.shutdown()
    publishMergeSinkKillSwitch.shutdown()
    eventBroadcastSourceKillSwitch.shutdown()
    val eventBroadcastConsumerFutureDone: Future[Done] = eventBroadcastConsumerFuture match {
      case Some(future) => future
      case None         => Future.successful(Done)
    }
    Future.reduceLeft(
      Seq(commandQueue.watchCompletion(), publishMergeSinkFuture, eventBroadcastConsumerFutureDone)
    )((_, _) => Done)(
      system.executionContext
    )
  }
}
