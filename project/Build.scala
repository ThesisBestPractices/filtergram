import Dependencies._
import sbt._
import Keys._

object Chinnews {

   import sbtprotobuf.ProtobufPlugin

   val build = Project("fg-chinnews", file("chinnews"),
     settings = Defaults.coreDefaultSettings ++
     Settings.common ++
     ProtobufPlugin.protobufSettings ++
     Seq(
        javaSource in ProtobufPlugin.protobufConfig <<= (sourceDirectory in Compile)(_ / "generated"),
        libraryDependencies ++= Seq(scalajHttp, argonaut, mongoScalaDriver, sprayJson,
             scalaLogging, slf4j, akka, httpcore, commonsIo, protobuf, guice, scala_guice) ++  http4s)
   )

}

object Core {
  val build = Project("fg-core", file("core"))
    .settings(Settings.common: _*)
}

object Filter {
  val build = Project("fg-filter", file("filter"))
    .dependsOn(Core.build)
    .settings(Settings.common: _*)
}

object Web {
  val build = Project("fg-web", file("web"))
    .dependsOn(Core.build)
    .settings(Settings.common: _*)
}

object Filtergram extends Build {

  lazy val chinnews = Chinnews.build
  lazy val core = Core.build
  lazy val filter = Filter.build
  lazy val web = Web.build

  lazy val filtergram = Project("fg-filtergram", file("."))
    .aggregate(chinnews, core, filter, web)
    .settings(Settings.common: _*)
}