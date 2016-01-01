package info.chinnews

import akka.actor.ActorSystem
import com.google.inject.Guice
import com.typesafe.config.Config
import com.typesafe.scalalogging.Logger
import info.chinnews.instagram._
import info.chinnews.system.DB
import info.chinnews.system.akkaguice.{ConfigModule, SystemModule}
import org.apache.log4j.{LogManager, Level}
import org.mongodb.scala._
import org.slf4j.LoggerFactory
import scala.collection.JavaConversions._
import net.codingwell.scalaguice.InjectorExtensions._

object Main {

  val logger = Logger(LoggerFactory.getLogger(this.getClass))

  def main(args: Array[String]): Unit = {

    val injector = Guice.createInjector(
      new ConfigModule(),
      new SystemModule()
    )

    val actorSystem = injector.instance[ActorSystem]
    val conf = injector.instance[Config]
    val db = injector.instance[DB]
    val frontServer = injector.instance[FrontServer]

    LogManager.getRootLogger.setLevel(Level.toLevel(conf.getString("chin_news.log.level")))

    subscribe(conf, actorSystem, db, frontServer)
  }

  def subscribe(conf: Config, actorSystem: ActorSystem, db: DB, frontServer: FrontServer): Unit = {
    val client_id = conf.getString("chin_news.instagram.client_id")
    val client_secret = conf.getString("chin_news.instagram.client_secret")
    val callback_url = conf.getString("chin_news.public.host")

    Subscriber.removeOldConnections(client_id, client_secret)
    db.removeAllCities(() => {
      CitiesHolder.addCities(db)
      frontServer.subscribe()
      db.forAllCities((city: Document) => {
        val name = city.get("name").get.asString().getValue
        logger.info(s"Subscribing to the city $name")
        city.get("tags").get.asArray().foreach(value => {
          val tag = value.asString().getValue
          logger.info(s"Subscribing to the tag $tag")
          Subscriber.subscribeByTag(tag, client_id, client_secret, callback_url, name)
        })

        //      val lat = city.get("lat").get.asString().getValue
        //      val lng = city.get("lng").get.asString().getValue
        //      Subscriber.subscribeByLocation(lat, lng, client_id, client_secret, callback_url, name)
        //      Subscriber.subscribeByTag(name, client_id, client_secret, callback_url, name)
      })
    })

  }
}

