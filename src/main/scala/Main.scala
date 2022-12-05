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

    implicit val system: ActorSystem[Nothing] = ActorSystem[Nothing](
      Behaviors.setup[Nothing] { context =>
        context.log.info("system started")
        Behaviors.empty
      },
      name = "ScalaMqttWrapper"
    )
    val source = new MqttSource(
      MqttSettings(
        host = "mosquitto",
        port = 1883,
        topics = Seq(MqttTopic("input"))
      )
    )
    val sink = new MqttSink(
      MqttSettings(
        host = "mosquitto",
        port = 1883
      )
    )
    source.flow
      .via(Flow[(ByteString, String)].map { case (msg, topic) =>
        val outputMsg = ByteString(msg.utf8String.toUpperCase)
        val outputTopic = "output"
        val publishFlags = ControlPacketFlags.QoSAtLeastOnceDelivery | ControlPacketFlags.RETAIN
        println(
          s"source [$topic] ${msg.utf8String} --> sink [$outputTopic] ${outputMsg.utf8String}"
        )
        (outputMsg, outputTopic, publishFlags)
      })
      .to(sink.flow)
      .run()
  }
}
