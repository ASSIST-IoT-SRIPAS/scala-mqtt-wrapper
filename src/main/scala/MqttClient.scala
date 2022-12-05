package pl.waw.ibspan.scala_mqtt_wrapper

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.alpakka.mqtt.streaming.Command
import akka.stream.alpakka.mqtt.streaming.Connect
import akka.stream.alpakka.mqtt.streaming.ConnectFlags
import akka.stream.alpakka.mqtt.streaming.Event
import akka.stream.alpakka.mqtt.streaming.MqttCodec
import akka.stream.alpakka.mqtt.streaming.MqttSessionSettings
import akka.stream.alpakka.mqtt.streaming.scaladsl.ActorMqttClientSession
import akka.stream.alpakka.mqtt.streaming.scaladsl.Mqtt
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Tcp
import akka.util.ByteString


import scala.concurrent.Future

class MqttClient(mqttSettings: MqttSettings)(implicit system: ActorSystem[_]) {
  val settings: MqttSessionSettings = MqttSessionSettings()
  val session: ActorMqttClientSession = ActorMqttClientSession(settings)
  val tcpConnection: Flow[ByteString, ByteString, Future[Tcp.OutgoingConnection]] =
    Tcp(system).outgoingConnection(mqttSettings.host, mqttSettings.port)
  val flow: Flow[Command[Nothing], Either[MqttCodec.DecodeError, Event[Nothing]], NotUsed] =
    Mqtt
      .clientSessionFlow(session, mqttSettings.sessionId)
      .join(tcpConnection)
  val connection: Command[Nothing] =
    Command[Nothing](
      Connect(
        mqttSettings.clientId,
        extraConnectFlags = ConnectFlags.CleanSession,
        username = mqttSettings.username.getOrElse(""),
        password = mqttSettings.password.getOrElse("")
      )
    )
}
