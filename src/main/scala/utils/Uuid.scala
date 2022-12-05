package pl.waw.ibspan.scala_mqtt_wrapper
package utils

import java.util.UUID

object Uuid {
  def generateUuid: UUID = java.util.UUID.randomUUID
}
