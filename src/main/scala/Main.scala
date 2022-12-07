package pl.waw.ibspan.scala_mqtt_wrapper

object Main {
  def main(args: Array[String]): Unit = {
    println("Scala MQTT wrapper")

    // TODO: remove me
    // example code
    import akka.actor.typed.ActorSystem
    import akka.actor.typed.scaladsl.Behaviors
    import akka.stream.scaladsl.Flow
    import akka.util.ByteString
    import akka.stream.alpakka.mqtt.streaming.ControlPacketFlags
    import akka.stream.scaladsl.Sink
    import akka.stream.scaladsl.Source
    import akka.stream.alpakka.mqtt.streaming.Command
    import akka.stream.alpakka.mqtt.streaming.Publish
    import akka.stream.KillSwitches
    import akka.stream.scaladsl.Keep
    import scala.concurrent.ExecutionContextExecutor
    import akka.stream.alpakka.mqtt.streaming.Subscribe

    implicit val system: ActorSystem[Nothing] = ActorSystem[Nothing](
      Behaviors.setup[Nothing] { context =>
        context.log.info("system started")
        Behaviors.empty
      },
      name = "ScalaMqttWrapper"
    )
    implicit val ec: ExecutionContextExecutor = system.executionContext

    val sourceClient = new MqttClient(
      MqttSettings(
        host = "mosquitto",
        port = 1883,
        topics = Seq(MqttTopic("input"))
      ),
      name = "sourceClient"
    )
    val source = MqttSource.source(sourceClient)

    val sinkClient = new MqttClient(
      MqttSettings(
        host = "mosquitto",
        port = 1883
      ),
      name = "sinkClient"
    )
    val sink = MqttSink.sink(sinkClient)

    val uppercaseFlow = Flow[(ByteString, String)].map { case (msg, topic) =>
      val outputMessage = ByteString(msg.utf8String.toUpperCase)
      val outputTopic = "output"
      val publishFlags = ControlPacketFlags.QoSAtLeastOnceDelivery | ControlPacketFlags.RETAIN
      println(
        s"source [$topic] ${msg.utf8String} --> sink [$outputTopic] ${outputMessage.utf8String}"
      )
      (outputMessage, outputTopic, publishFlags)
    }

    source
      .via(uppercaseFlow)
      .runWith(sink)

    sourceClient.commandQueue.offer(Command[Nothing](Subscribe("test")))

    Thread.sleep(60000)
    sourceClient.shutdown()
    sinkClient.shutdown()
    println("done")
  }
}
