package info.chinnews.instagram.actors

import java.io.ByteArrayInputStream

import akka.actor.Actor
import com.chinnews.Instagram
import com.daxzel.shttpparser.DefaultHttpRequestParser
import com.daxzel.shttpparser.message.{HttpTransportMetricsImpl, SessionInputBufferImpl, Consts, HttpEntityEnclosingRequest}
import info.chinnews.system.akkaguice.{GuiceAkkaExtension, NamedActor}
import com.google.protobuf.ExtensionRegistry
import com.googlecode.protobuf.format.JsonFormat
import com.typesafe.scalalogging.Logger
import org.apache.commons.io.IOUtils
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

      val sessionInputBuffer = new SessionInputBufferImpl(new HttpTransportMetricsImpl(), 2048)
      sessionInputBuffer.bind(new ByteArrayInputStream(message.getBytes(Consts.ASCII)))
      val requestParser = new DefaultHttpRequestParser(sessionInputBuffer)
      requestParser.parse() match {
        case request: HttpEntityEnclosingRequest =>
          if (request.getEntity != null) {
            val content = request.getEntity.getContent
            val stringContent = IOUtils.toString(content)
            if (stringContent.charAt(0).equals('[')) {
              forAllJsonArrayElements(stringContent, handleOneJsonElement)
            } else {
              handleOneJsonElement(stringContent)
            }
          } else {
            logger.warn(s"Can't get a request entity ")
            if (request.expectContinue()) {
              logger.warn(s"Expected continue")
            }
          }
        case default => logger.info("Unrecognized request " + default.toString)
      }
  }

  def handleOneJsonElement(el: String): Unit = {
    val builder = Instagram.SubscriptionUpdate.newBuilder()
    val jsonFormat = new JsonFormat
    jsonFormat.merge(el, ExtensionRegistry.getEmptyRegistry, builder)
    val subscriptionUpdate = builder.build()
    logger.info(s"Subscription id: " + subscriptionUpdate.getSubscriptionId)
    photoUpdateActor ! subscriptionUpdate
  }

  def forAllJsonArrayElements(array: String, func: String => Unit): Unit = {
    array.substring(1, array.length - 1).split(",").foreach(func)
  }
}
