package info.chinnews.instagram.actors

import akka.actor.Actor
import argonaut.Parse
import com.chinnews.Instagram.SubscriptionUpdate
import com.google.inject.Inject
import info.chinnews.instagram.InstagramAuth
import info.chinnews.system.DB
import info.chinnews.system.akkaguice.NamedActor

import scalaj.http.Http

//import com.chinnews.Instagram.SubscriptionUpdate
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
    case subscriptionUpdate: SubscriptionUpdate =>
      logger.info("Received message for the photo update: " + subscriptionUpdate.getSubscriptionId)
      val accessToken = auth.acquireToken()
      updatePhotos(accessToken, subscriptionUpdate, subscriptionUpdate.getObjectId)
  }

  def updatePhotos(accessToken: String, subscriptionUpdate: SubscriptionUpdate, city: String): Unit = {
    val searchBody = Http(s"https://api.instagram.com/v1/tags/$city/media/recent")
      .param("access_token", accessToken).asString.body

    logger.trace("Received news photos. Query:\n" + searchBody)

    val warsawUsers = Parse.parseOption(searchBody).get.field("data").get.array
      .get.map(json => json.field("user").get.field("username").toString).toSet
    warsawUsers.foreach(username => {
      logger.debug("Saving a user in city info: username - " + username + " city - " + city)
      db.storeUserLocation(city, username)
    })
  }

}
