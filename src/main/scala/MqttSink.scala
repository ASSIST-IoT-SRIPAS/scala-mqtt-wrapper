package pl.waw.ibspan.scala_mqtt_wrapper

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.OverflowStrategy
import akka.stream.alpakka.mqtt.streaming.Command
import akka.stream.alpakka.mqtt.streaming.ControlPacketFlags
import akka.stream.alpakka.mqtt.streaming.Publish
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Keep
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging

import scala.util.Failure
import scala.util.Success
import scala.util.Try

class MqttSink(
    mqttSettings: MqttSettings,
    bufferSize: Int,
    topics: Seq[Topic],
    parsedTopicNames: List[Parsed]
)(implicit
    system: ActorSystem[_]
) extends LazyLogging {
  import MqttSink._

  val zippedTopics: Seq[(Topic, Parsed)] = topics.zip(parsedTopicNames)
  val mqttClient: MqttClient = MqttClient(mqttSettings)
  val initialCommands: List[Command[Nothing]] = List(mqttClient.connection)
  val sink: Sink[(Try[AnyJson], String, Try[AnyJson], Either[String, String], Boolean), NotUsed] =
    Flow[(Try[AnyJson], String, Try[AnyJson], Either[String, String], Boolean)]
      .collect { case Success((topics, parametrizedTopicNames, msg)) =>
        topics.zip(parametrizedTopicNames).foreach { (topic, parametrizedTopicName) =>
          publish(topic, parametrizedTopicName, msg)
        }
      }
      .concatMat(
        Source(initialCommands)
          .concatMat(Source.queue(bufferSize, OverflowStrategy.backpressure))(Keep.right)
          .via(mqttClient.flow)
      )(Keep.none)
      .to(Sink.ignore)

  def publish(topic: Topic, parametrizedTopicName: String, message: ByteString): Unit = {
    val publishFlags =
      topic.publishFlags.getOrElse(Seq.empty[ControlPacketFlags]).reduceOption(_ | _)
    val command = publishFlags match {
      case Some(flags) => Command[Nothing](Publish(flags, parametrizedTopicName, message))
      case None        => Command[Nothing](Publish(parametrizedTopicName, message))
    }
    mqttClient.session ! command
  }
}
