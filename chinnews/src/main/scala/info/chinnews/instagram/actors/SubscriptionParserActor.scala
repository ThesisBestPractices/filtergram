package info.chinnews.instagram.actors

import java.io.ByteArrayInputStream

import akka.actor.Actor
import com.chinnews.{App, Instagram}
import com.chinnews.Instagram.SubscriptionUpdate
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

  def forAllJsonArrayElements(array: String, func: String => Unit): Unit = {
    //    array.substring(1, array.length - 1).split(",").foreach(func)
    func(array.substring(1, array.length - 1))
  }

  def handleOneJsonElement(el: String, func: SubscriptionUpdate => Unit): Unit = {
    //bug https://github.com/bivas/protobuf-java-format/issues/20
    val message = el.replace("{}", "\" \"")

    val builder = Instagram.SubscriptionUpdate.newBuilder()
    val jsonFormat = new JsonFormat
    jsonFormat.merge(message, ExtensionRegistry.getEmptyRegistry, builder)
    val subscriptionUpdate = builder.build()
    func(subscriptionUpdate)
  }

  def receiveCityId(uri: String) = {
    val uriParts = uri.split("/")
    uriParts(1)
  }
}

class SubscriptionParserActor extends Actor {

  val logger = Logger(LoggerFactory.getLogger(this.getClass))

  val photoUpdateActor = context.actorOf(GuiceAkkaExtension(context.system).props(PhotoUpdateActor.name))

  def receive() = {
    case message: String =>
      logger.trace(s"Received a request: $message")

      val sessionInputBuffer = new SessionInputBufferImpl(new HttpTransportMetricsImpl(), 2048)
      sessionInputBuffer.bind(new ByteArrayInputStream(message.getBytes(Consts.ASCII)))
      val requestParser = new DefaultHttpRequestParser(sessionInputBuffer)
      requestParser.parse() match {
        case request: HttpEntityEnclosingRequest =>
          if (request.getEntity != null) {
            val cityId = SubscriptionParserActor.receiveCityId(request.getRequestLine.getUri)
            logger.trace(s"Received a request. City id : $cityId")
            val content = request.getEntity.getContent
            val stringContent = IOUtils.toString(content)
            if (stringContent.charAt(0).equals('[')) {
              SubscriptionParserActor.forAllJsonArrayElements(stringContent,
                (el: String) => SubscriptionParserActor.handleOneJsonElement(el,
                  subscriptionUpdate => sendPhotoUpdate(subscriptionUpdate, cityId)))
            } else {
              SubscriptionParserActor.handleOneJsonElement(stringContent,
                subscriptionUpdate => sendPhotoUpdate(subscriptionUpdate, cityId))
            }
          } else {
            logger.warn(s"Can't get a request entity ")
            if (request.expectContinue()) {
              logger.warn(s"Expected continue")
            }
          }
        case default => logger.debug("Unrecognized request " + default.toString)
      }
  }

  def sendPhotoUpdate(subscriptionUpdate: SubscriptionUpdate, cityId: String) = {
    logger.info("Sending photo update to another actor. Tag: " +
      subscriptionUpdate.getObjectId + " city: " + cityId)

    val subscriptionUpdateCity = App.SubscriptionUpdateCity.newBuilder()
      .setCityId(cityId)
      .setSubscriptionUpdate(subscriptionUpdate).build()
    photoUpdateActor ! subscriptionUpdateCity
  }

}
