package info.chinnews.instagram.actors

import akka.actor.Actor
import com.chinnews.App.SubscriptionUpdateCity
import com.chinnews.Instagram
import com.chinnews.Instagram.SubscriptionUpdate
import com.google.inject.Inject
import com.google.protobuf.ExtensionRegistry
import com.googlecode.protobuf.format.JsonFormat
import info.chinnews.instagram.InstagramAuth
import info.chinnews.system.DB
import info.chinnews.system.akkaguice.NamedActor
import scala.collection.JavaConversions._

import scalaj.http.Http

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

/**
  * Created by Tsarevskiy
  */
object PhotoUpdateActor extends NamedActor {
  override final val name = "PhotoUpdateActor"
}

class PhotoUpdateActor @Inject()(auth: InstagramAuth, db: DB) extends Actor {

  val logger = Logger(LoggerFactory.getLogger(this.getClass))

  def receive() = {
    case subscriptionUpdateCity: SubscriptionUpdateCity =>
      val subscriptionUpdate = subscriptionUpdateCity.getSubscriptionUpdate
      logger.info("Received message for the photo update: " + subscriptionUpdate.getSubscriptionId)
      val accessToken = auth.acquireToken()
      updatePhotos(accessToken, subscriptionUpdate, subscriptionUpdateCity.getCityId)
  }

  def updatePhotos(accessToken: String, subscriptionUpdate: SubscriptionUpdate, city: String): Unit = {

    val searchBody = getRecentMediaByTag(subscriptionUpdate.getObjectId, accessToken)

    val builder = Instagram.MediaRecentResponse.newBuilder()
    new JsonFormat().merge(searchBody, ExtensionRegistry.getEmptyRegistry, builder)
    builder.getDataBuilderList.foreach(dataBuilder => dataBuilder.setId15(dataBuilder.getId13))
    logger.debug("Saving a media in city info: " + city)
    db.storeMediaResponse(builder.build())
  }

  def getRecentMediaByTag(tag: String, accessToken: String) = {
    val request = s"https://api.instagram.com/v1/tags/$tag/media/recent"
    logger.info("Running http query: " + request + " access token: " + accessToken)
    val searchBody = Http(request).param("access_token", accessToken).asString.body

    //some kind of bug fix
    val searchBodyFixed = searchBody.replace("\\/", "/")

    logger.trace("Received news photos. Query:\n" + searchBodyFixed)
    searchBodyFixed
  }

}
