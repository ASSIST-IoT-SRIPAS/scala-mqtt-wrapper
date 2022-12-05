package pl.waw.ibspan.scala_mqtt_wrapper

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.OverflowStrategy
import akka.stream.alpakka.mqtt.streaming.Command
import akka.stream.alpakka.mqtt.streaming.Connect
import akka.stream.alpakka.mqtt.streaming.Event
import akka.stream.alpakka.mqtt.streaming.MqttCodec
import akka.stream.alpakka.mqtt.streaming.MqttSessionSettings
import akka.stream.alpakka.mqtt.streaming.Subscribe
import akka.stream.alpakka.mqtt.streaming.scaladsl.ActorMqttClientSession
import akka.stream.alpakka.mqtt.streaming.scaladsl.Mqtt
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Keep
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.SinkQueueWithCancel
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.SourceQueueWithComplete
import akka.stream.scaladsl.Tcp
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Future

class MqttClient(mqttSettings: MqttSettings)(implicit system: ActorSystem[_]) extends LazyLogging {
  val settings: MqttSessionSettings = MqttSessionSettings()
  val session: ActorMqttClientSession = ActorMqttClientSession(settings)
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
  val commandQueueBufferSize: Int = 100
  val (commandQueue, eventQueue) = Source
    .queue(commandQueueBufferSize, OverflowStrategy.backpressure)
    .via(sessionFlow)
    .wireTap(event => logger.debug(s"Received output event $event"))
    .toMat(Sink.queue())(Keep.both)
    .run()
  for (command <- initialCommands) {
    commandQueue.offer(command)
  }

//  import akka.stream.alpakka.mqtt.streaming.Publish
//  import scala.concurrent.ExecutionContext
//  import ExecutionContext.Implicits.global
//  val f = commandQueue.offer(Command[Nothing](Publish("input", ByteString("a"))))
//  f.onComplete(_ => println("DONE"))
//  session ! Command[Nothing](Publish("input", ByteString("a")))
}
