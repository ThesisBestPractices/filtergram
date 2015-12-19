package info.chinnews.system.akkaguice

import javax.inject.Inject

import akka.actor.{Actor, ActorSystem}
import com.chinnews.Instagram
import com.google.inject.name.Names
import com.google.inject.{AbstractModule, Injector, Provider}
import com.typesafe.config.Config
import info.chinnews.instagram.InstagramAuth
import info.chinnews.instagram.actors.{PhotoUpdateActor, SubscriptionParserActor}
import info.chinnews.system.DB
import info.chinnews.system.akkaguice.SystemModule.{DbSystemProvider, ActorSystemProvider}
import net.codingwell.scalaguice.ScalaModule

/**
  * Created by Tsarevskiy
  */

object SystemModule {
  class ActorSystemProvider @Inject() (val config: Config, val injector: Injector) extends Provider[ActorSystem] {
    override def get() = {
      val system = ActorSystem("main-actor-system", config)
      // add the GuiceAkkaExtension to the system, and initialize it with the Guice injector
      GuiceAkkaExtension(system).initialize(injector)
      system
    }
  }

  class DbSystemProvider @Inject() (val conf: Config, val injector: Injector) extends Provider[DB] {
    override def get() = {
      val db = DB(conf.getString("chin_news.db.name"), conf.getString("chin_news.db.host"),
        conf.getInt("chin_news.db.port"))
      db
    }
  }
}

/**
  * A module providing an Akka ActorSystem.
  */
class SystemModule extends AbstractModule with ScalaModule {

  override def configure() {
    install(new ConfigModule)
    bind[ActorSystem].toProvider[ActorSystemProvider].asEagerSingleton()
    bind[DB].toProvider[DbSystemProvider]
    bind[InstagramAuth].to[InstagramAuth].asEagerSingleton()
    bind[Actor].annotatedWith(Names.named(SubscriptionParserActor.name)).to[SubscriptionParserActor]
    bind[Actor].annotatedWith(Names.named(PhotoUpdateActor.name)).to[PhotoUpdateActor]
  }
}