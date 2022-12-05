package pl.waw.ibspan.scala_mqtt_wrapper

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.alpakka.mqtt.streaming.Event
import akka.stream.alpakka.mqtt.streaming.Publish
import akka.stream.scaladsl.Keep
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.SourceQueueWithComplete
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging

class MqttSource(mqttSettings: MqttSettings)(implicit system: ActorSystem[_]) extends LazyLogging {
  val mqttClient: MqttClient = new MqttClient(mqttSettings)
  val flow: Source[(ByteString, String), NotUsed] = Source
    .repeat(NotUsed)
    .map(_ => mqttClient.eventQueue.pull())
    .mapAsync(1)(identity)
    .wireTap(event => logger.debug(s"Received input event $event"))
    .collect { case Some(Right(Event(p: Publish, _))) =>
      (p.payload, p.topicName)
    }
}
