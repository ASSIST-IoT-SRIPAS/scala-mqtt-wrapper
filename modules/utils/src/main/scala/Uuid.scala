package pl.waw.ibspan.scala_mqtt_wrapper.utils

import java.util.UUID

private[scala_mqtt_wrapper] object Uuid {
  def generateUuid: UUID = java.util.UUID.randomUUID
}
