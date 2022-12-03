package pl.waw.ibspan.scala_mqtt_wrapper

import akka.actor.typed.ActorSystem
import akka.stream.OverflowStrategy
import akka.stream.alpakka.mqtt.streaming.Command
import akka.stream.alpakka.mqtt.streaming.Event
import akka.stream.alpakka.mqtt.streaming.Publish
import akka.stream.alpakka.mqtt.streaming.Subscribe
import akka.stream.scaladsl.Keep
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.SourceQueueWithComplete

import scala.util.Right

class MqttSource(mqttSettings: MqttSettings, bufferSize: Int)(implicit system: ActorSystem[_]) {
  val mqttClient: MqttClient = MqttClient(mqttSettings)
  val subscriptions: Option[List[Command[Nothing]]] =
    mqttSettings.topics.map(_.map(topic => Command[Nothing](Subscribe(topic.name))).toList)
  val initialCommands: List[Command[Nothing]] =
    mqttClient.connection :: subscriptions.getOrElse(Nil)
  val source: Source[String, SourceQueueWithComplete[Nothing]] =
    Source(initialCommands)
      .concatMat(Source.queue(bufferSize, OverflowStrategy.backpressure))(Keep.right)
      .via(mqttClient.flow)
      .collect { case Right(Event(p: Publish, _)) =>
        p.payload.utf8String
      }
}
