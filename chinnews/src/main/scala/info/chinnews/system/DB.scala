package info.chinnews.system

import com.typesafe.scalalogging.Logger
import org.mongodb.scala._
import org.slf4j.LoggerFactory

import org.mongodb.scala.model.Filters._

/**
  * Created by tsarevskiy on 12/11/15.
  */
case class DB(dbname: String, host: String, port: Int) {

  val logger = Logger(LoggerFactory.getLogger(this.getClass))

  val observer = new Observer[Completed] {
    override def onNext(result: Completed): Unit = {}

    override def onError(e: Throwable): Unit =
      logger.error("Can't process a database query:\n" + e.toString)

    override def onComplete(): Unit = {}
  }

  val mongoClient: MongoClient = MongoClient(s"mongodb://$host:$port")
  val database: MongoDatabase = mongoClient.getDatabase(dbname)

  val userLocations = database.getCollection("user_locations")

  val cities = database.getCollection("cities")


  def storeUserLocation(city_id: String, username: String): Unit = {
    val id = city_id + username
    if (userLocations.find(equal("_id", id)).first() == null) {
      userLocations.insertOne(Document(
        "_id" -> id,
        "city_id" -> city_id,
        "username" -> username)).subscribe(observer)
    }
  }

  def forAllCities(f: (Document) => _) {
    cities.find().foreach(document => f(document))
  }

  def addCity(name: String, lat: String, lng: String, tags: Seq[String]): Unit = {
    cities.insertOne(Document(
      "_id" -> name,
      "name" -> name,
      "lat" -> lat,
      "lng" -> lng,
      "tags" -> tags)).subscribe(observer)
  }

}
