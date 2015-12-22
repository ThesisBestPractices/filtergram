import sbt.Keys._
import sbt._
import sbtassembly.AssemblyPlugin

object Settings {

  import net.virtualvoid.sbt.graph.DependencyGraphSettings._

  val resolutionRepos = Seq(
    resolvers ++= Seq("Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/", Resolver.mavenLocal)
  )

  val javaVer = "1.7"
  val scalaVer = "2.11.7"

  lazy val common = Seq(
    ivyScala := ivyScala.value map {
      _.copy(overrideScalaVersion = true)
    },
    scalaVersion := scalaVer,
    version := "0.1-SNAPSHOT",
    javacOptions := Seq(
      "-Xlint:-options",
      "-source", javaVer, "-target", javaVer),
    scalacOptions := Seq(
      "-encoding",
      "utf8",
      "-g:vars",
      "-feature",
      "-unchecked",
      "-optimise",
      "-deprecation",
      "-target:jvm-1.7",
      "-language:postfixOps",
      "-language:implicitConversions"
    )) ++ graphSettings ++ resolutionRepos

}

object Dependencies {

  val scalajHttp = "org.scalaj" % "scalaj-http_2.11" % "1.1.6"
  val argonaut = "io.argonaut" % "argonaut_2.11" % "6.0.4"
  val mongoScalaDriver = "org.mongodb.scala" % "mongo-scala-driver_2.11" % "1.0.0"
  val sprayJson = "io.spray" % "spray-json_2.11" % "1.3.2"

  val scalaLogging = "com.typesafe.scala-logging" % "scala-logging_2.11" % "3.1.0"
  val slf4j = "org.slf4j" % "slf4j-log4j12" % "1.7.13"

  val akka = "com.typesafe.akka" % "akka-actor_2.11" % "2.4.1"
  val shttpparser = "com.daxzel" % "shttpparser" % "0.4"
  val commonsIo = "commons-io" % "commons-io" % "2.4"

  val protobuf = "com.googlecode.protobuf-java-format" % "protobuf-java-format" % "1.4"

  val guice = "com.google.inject" % "guice" % "4.0"
  val scala_guice = "net.codingwell" % "scala-guice_2.10" % "4.0.1"

  val http4s = Seq(
    "org.http4s" % "http4s-blaze-server_2.11" % "0.10.0",
    "org.http4s" % "http4s-dsl_2.11" % "0.10.0",
    "org.http4s" % "http4s-argonaut_2.11" % "0.10.0",
    "org.http4s" % "http4s-jetty_2.11" % "0.10.0"
  )
}

object Tasks {

  import AssemblyPlugin.autoImport._

  val packageChinNews = TaskKey[Unit]("packageAll")

  val packageChinNewsTask = packageChinNews := {
    val assembled = assembly.toTask.value
    val targetPath = baseDirectory.value.getAbsolutePath + "/target/"
    val files = Map(
      new File(baseDirectory.value.getAbsolutePath.concat(
        "/src/main/resources/application.conf")) -> "application.conf",
      assembly.toTask.value -> "chin_news.jar"
    )
    val chinNewsZip = new File(targetPath + "/chin_news.zip")
    IO.zip(files, chinNewsZip)
    println("Packaged zip is created " + chinNewsZip.absolutePath)
  }

  assemblyMergeStrategy in assembly := {
    case PathList("application.conf") => sbtassembly.MergeStrategy.discard
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  }

}