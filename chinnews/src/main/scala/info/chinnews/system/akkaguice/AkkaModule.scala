package info.chinnews.system.akkaguice

import javax.inject.Inject

import akka.actor.ActorSystem
import com.google.inject.{AbstractModule, Injector, Provider}
import com.typesafe.config.Config
import info.chinnews.system.akkaguice.AkkaModule.ActorSystemProvider
import net.codingwell.scalaguice.ScalaModule

/**
  * Created by Tsarevskiy
  */

object AkkaModule {
  class ActorSystemProvider @Inject() (val config: Config, val injector: Injector) extends Provider[ActorSystem] {
    override def get() = {
      val system = ActorSystem("main-actor-system", config)
      // add the GuiceAkkaExtension to the system, and initialize it with the Guice injector
      GuiceAkkaExtension(system).initialize(injector)
      system
    }
  }
}

/**
  * A module providing an Akka ActorSystem.
  */
class AkkaModule extends AbstractModule with ScalaModule {

  override def configure() {
    bind[ActorSystem].toProvider[ActorSystemProvider].asEagerSingleton()
  }
}