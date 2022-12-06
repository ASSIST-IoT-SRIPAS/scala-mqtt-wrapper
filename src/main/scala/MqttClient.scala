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
    mqttSettings: MqttSettings,
    mqttSessionSettings: MqttSessionSettings = MqttSessionSettings(),
    name: String = ""
)(implicit system: ActorSystem[_])
    extends LazyLogging {
  val session: ActorMqttClientSession = ActorMqttClientSession(mqttSessionSettings)
  val tcpConnection: Flow[ByteString, ByteString, Future[Tcp.OutgoingConnection]] =
    Tcp(system).outgoingConnection(mqttSettings.host, mqttSettings.port)
  val sessionFlow: Flow[Command[Nothing], Either[MqttCodec.DecodeError, Event[Nothing]], NotUsed] =
    Mqtt
      .clientSessionFlow(session, mqttSettings.sessionId)
      .join(tcpConnection)
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
  val restartingSourceSettings: RestartSettings = RestartSettings(
    minBackoff = mqttSettings.restartMinBackoff,
    maxBackoff = mqttSettings.restartMaxBackoff,
    randomFactor = mqttSettings.restartRandomFactor
  )
    .withMaxRestarts(mqttSettings.maxRestarts, mqttSettings.restartMinBackoff)
    .withLogSettings(RestartSettings.createLogSettings(logLevel = mqttSettings.restartLogLevel))
  val source: Source[Either[MqttCodec.DecodeError, Event[Nothing]], NotUsed] =
    RestartSource.withBackoff(restartingSourceSettings) { () =>
      Source(initialCommands)
        .concatMat(Source.never)(Keep.left)
        .via(sessionFlow)
        .wireTap(event => logger.debug(s"[$name] Received event $event"))
    }
  val (sourceBroadcastKillSwitch, sourceBroadcast) = {
    source
      .viaMat(KillSwitches.single)(Keep.right)
      .toMat(BroadcastHub.sink(bufferSize = mqttSettings.broadcastHubBufferSize))(Keep.both)
      .run()
  }
  val sourceBroadcastFuture: Future[Done] = sourceBroadcast.runWith(Sink.ignore)
  sourceBroadcastFuture.onComplete(_ => logger.debug(s"[$name] Source broadcast hub shutdown"))(
    system.executionContext
  )
  def shutdown(): Future[Done] = {
    sourceBroadcastKillSwitch.shutdown()
    sourceBroadcastFuture
  }
}
