package info.chinnews

import akka.actor.ActorSystem
import com.google.inject.{Injector, Guice}
import com.typesafe.config.Config
import com.typesafe.scalalogging.Logger
import info.chinnews.instagram._
import info.chinnews.system.DB
import info.chinnews.system.akkaguice.{ConfigModule, SystemModule}
import org.mongodb.scala._
import org.slf4j.LoggerFactory
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

    subscribe(conf, actorSystem, injector)

    //    InstragramAuth(conf.getString("chin_news.instagram.client_id"), conf.getString("chin_news.instagram.client_secret"))
    //      .auth(conf.getString("chin_news.instagram.login"), conf.getString("chin_news.instagram.password"), conf, (accessToken, failureListener) => {
    //
    //        val db = DB(conf.getString("chin_news.db.name"), conf.getString("chin_news.db.host"),
    //          conf.getInt("chin_news.db.port"))
    //        scheduler.schedule(
    //          initialDelay = Duration(5, TimeUnit.SECONDS),
    //          interval = Duration(60, TimeUnit.SECONDS),
    //          runnable = new LocationCrawler(accessToken, failureListener, db))
    //
    //        scheduler.schedule(
    //          initialDelay = Duration(5, TimeUnit.SECONDS),
    //          interval = Duration(20, TimeUnit.SECONDS),
    //          runnable = new TagCrawler(accessToken, failureListener, db))
    //      }
    //      )
  }

  def subscribe(conf: Config, actorSystem: ActorSystem, injector: Injector): Unit = {
    val client_id = conf.getString("chin_news.instagram.client_id")
    val client_secret = conf.getString("chin_news.instagram.client_secret")
    val callback_url = conf.getString("chin_news.public.host")

    val db = injector.instance[DB]
    Subscriber.removeOldConnections(client_id, client_secret)
    CitiesHolder.addCities(db)
    FrontServer.subscribe(actorSystem)
    db.forAllCities((city: Document) => {
      val name = city.get("name").get.asString().getValue
      val lat = city.get("lat").get.asString().getValue
      val lng = city.get("lng").get.asString().getValue
      logger.info(s"Subscribing to the city $name")

      Subscriber.subscribeByLocation(lat, lng, client_id, client_secret, callback_url, name)
      Subscriber.subscribeByTag(name, client_id, client_secret, callback_url, name)
    })
  }
}

