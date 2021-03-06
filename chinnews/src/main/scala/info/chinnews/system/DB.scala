package info.chinnews.system

import com.chinnews.Instagram.MediaRecentResponse
import com.googlecode.protobuf.format.JsonFormat
import com.typesafe.scalalogging.Logger
import org.mongodb.scala._
import org.slf4j.LoggerFactory
import scala.collection.JavaConversions._

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

  val instagramMedia = database.getCollection("instagram_media")

  def storeUserLocation(city_id: String, username: String): Unit = {
    val id = city_id + username
    if (userLocations.find(equal("_id", id)).first() == null) {
      userLocations.insertOne(Document(
        "_id" -> id,
        "city_id" -> city_id,
        "username" -> username)).subscribe(observer)
    }
  }

  def storeMediaResponse(mediaRecentResponse: MediaRecentResponse): Unit = {
    mediaRecentResponse.getDataList.foreach(response => {
      val json = new JsonFormat().printToString(response)
      logger.info("Storing media with the id" + response.getId13)
      instagramMedia.insertOne(Document(json)).subscribe(observer)
    })
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

  def removeAllCities(f: () => _): Unit = {

    cities.drop().subscribe(new Observer[Completed] {
      override def onNext(result: Completed): Unit = {}

      override def onError(e: Throwable): Unit = logger.error("Can't drop a collection")

      override def onComplete(): Unit = f()
    })
  }

}
