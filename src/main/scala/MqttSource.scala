package pl.waw.ibspan.scala_mqtt_wrapper

import akka.actor.typed.ActorSystem
import akka.stream.alpakka.mqtt.streaming.Event
import akka.stream.alpakka.mqtt.streaming.Publish
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.SourceQueueWithComplete
import akka.util.ByteString

class MqttSource(mqttSettings: MqttSettings)(implicit system: ActorSystem[_]) {
  val mqttClient: MqttClient = new MqttClient(mqttSettings)
  val flow: Source[(ByteString, String), SourceQueueWithComplete[Nothing]] = mqttClient.flow
    .collect { case Right(Event(p: Publish, _)) =>
      (p.payload, p.topicName)
    }
}
