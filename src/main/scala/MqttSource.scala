package pl.waw.ibspan.scala_mqtt_wrapper

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.alpakka.mqtt.streaming.Event
import akka.stream.alpakka.mqtt.streaming.Publish
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging

object MqttSource extends LazyLogging {
  def source(mqttClient: MqttClient)(implicit
      system: ActorSystem[_]
  ): Source[(ByteString, String), NotUsed] = mqttClient.sourceBroadcast
    .collect { case Right(Event(p: Publish, _)) =>
      (p.payload, p.topicName)
    }
}
