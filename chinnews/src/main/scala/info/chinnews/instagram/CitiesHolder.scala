package info.chinnews.instagram

import info.chinnews.system.DB
;

/**
  * Created by Tsarevskiy
  */
object CitiesHolder {

  def addCities(db: DB) {
    db.addCity("warsaw", "52.215361", "17.018681", Seq("warsaw", "warsawa"))
    db.addCity("wroclaw", "51.105643", "17.018681", Seq("wroclaw", "wrocław"))
    db.addCity("gdansk", "54.351128", "18.646977", Seq("gdansk", "gdańsk"))
    db.addCity("moscow", "55.753567", "37.621077", Seq("moscow", "москва"))
  }
}
