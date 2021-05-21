package com.github.ojacquemart.realtime.db.debezium

import io.vertx.core.eventbus.EventBus
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.jboss.logging.Logger
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class DebeziumChangeEventConsumer(
  val eventBus: EventBus
) {

  private val logger = Logger.getLogger(DebeziumChangeEventConsumer::class.java)

  @Incoming("rtdb")
  fun onMessage(message: DebeziumChangeEvent?) {
    try {
      logger.info("Message received: $message")

      val data = DebeziumData.from(message)
      data?.let {
        eventBus.publish("dbz-data", it)
      }
    } catch (e: Exception) {
      logger.error("Unable to process message", e)
    }
  }

}