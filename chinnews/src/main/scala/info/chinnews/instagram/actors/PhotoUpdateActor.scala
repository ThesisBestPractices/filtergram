package info.chinnews.instagram.actors

import akka.actor.Actor
import com.chinnews.Instagram.SubscriptionUpdate
import info.chinnews.system.akkaguice.NamedActor

//import com.chinnews.Instagram.SubscriptionUpdate
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

/**
  * Created by Tsarevskiy
  */
object PhotoUpdateActor extends NamedActor {
  override final val name = "PhotoUpdateActor"
}

class PhotoUpdateActor extends Actor {

  val logger = Logger(LoggerFactory.getLogger(this.getClass))

  def receive() = {
    case subscriptionUpdate: SubscriptionUpdate =>
      logger.info("Received message for the photo update: " + subscriptionUpdate.getSubscriptionId)
  }

}
