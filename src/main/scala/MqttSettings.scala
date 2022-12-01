package pl.waw.ibspan.scala_mqtt_wrapper

final case class MqttSettings(
    username: String,
    password: String,
    host: String,
    port: Int,
    topics: Seq[MqttTopic]
)
