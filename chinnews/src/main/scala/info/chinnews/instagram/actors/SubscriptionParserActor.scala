package info.chinnews.instagram.actors

import java.io.ByteArrayInputStream

import akka.actor.Actor
import com.chinnews.Instagram
import info.chinnews.system.akkaguice.{GuiceAkkaExtension, NamedActor}
import org.apache.http.impl.DefaultHttpClientConnection

import com.google.protobuf.ExtensionRegistry
import com.googlecode.protobuf.format.JsonFormat
import com.typesafe.scalalogging.Logger
import org.apache.http.impl.io.{DefaultHttpRequestParser, HttpTransportMetricsImpl, SessionInputBufferImpl}
import org.apache.http._
import org.slf4j.LoggerFactory

/**
  * Created by Tsarevskiy
  */

object SubscriptionParserActor extends NamedActor {
  override final val name = "SubscriptionParserActor"
}

class SubscriptionParserActor extends Actor {

  val logger = Logger(LoggerFactory.getLogger(this.getClass))

  val photoUpdateActor = context.actorOf(GuiceAkkaExtension(context.system).props(SubscriptionParserActor.name))

  def receive() = {
    case message: String =>
      logger.info(s"Received a request: $message")

      val client = new DefaultHttpClientConnection()
      val sessionInputBuffer = new SessionInputBufferImpl(new HttpTransportMetricsImpl(), 2048)
      sessionInputBuffer.bind(new ByteArrayInputStream(message.getBytes(Consts.ASCII)))
      val requestParser = new DefaultHttpRequestParser(sessionInputBuffer)
      requestParser.parse() match {
        case request: HttpEntityEnclosingRequest =>
          client.sendRequestEntity(request)
          if (request.getEntity != null) {
            val content = request.getEntity.getContent

            val builder = Instagram.SubscriptionUpdate.newBuilder()
            val jsonFormat = new JsonFormat
            jsonFormat.merge(content, ExtensionRegistry.getEmptyRegistry, builder)
            val subscriptionUpdate = builder.build()

            logger.info(s"Subscription id: " + subscriptionUpdate.getSubscriptionId)
            photoUpdateActor ! subscriptionUpdate
          } else {
            logger.warn(s"Can't get a request entity ")
            if (request.expectContinue()) {
              logger.warn(s"Expected continue")
            }
          }
        case default => logger.info("Unrecognized request " + default.toString)
      }
  }
}
