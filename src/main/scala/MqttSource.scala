package pl.waw.ibspan.scala_mqtt_wrapper

import akka.actor.typed.ActorSystem
import akka.stream.KillSwitches
import akka.stream.UniqueKillSwitch
import akka.stream.alpakka.mqtt.streaming.Event
import akka.stream.alpakka.mqtt.streaming.Publish
import akka.stream.scaladsl.Keep
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging

class MqttSource(mqttSettings: MqttSettings)(implicit system: ActorSystem[_]) extends LazyLogging {
  val mqttClient: MqttClient = new MqttClient(mqttSettings)
  val flow: Source[(ByteString, String), UniqueKillSwitch] = mqttClient.source
    .viaMat(KillSwitches.single)(Keep.right)
    .collect { case Right(Event(p: Publish, _)) =>
      (p.payload, p.topicName)
    }
}
