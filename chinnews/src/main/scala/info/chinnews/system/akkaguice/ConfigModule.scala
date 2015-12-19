package info.chinnews.system.akkaguice

import java.io.File

import com.google.inject.{AbstractModule, Provider}
import com.typesafe.config.{Config, ConfigFactory}
import info.chinnews.system.akkaguice.ConfigModule.ConfigProvider
import net.codingwell.scalaguice.ScalaModule

object ConfigModule {

  class ConfigProvider extends Provider[Config] {
    override def get() = {
      val applicationFile = new File("application.conf")
      val conf = if (applicationFile.exists()) {
        ConfigFactory.parseFile(applicationFile)
      } else {
        ConfigFactory.load()
      }
      conf
    }
  }

}

/**
  * Binds the application configuration to the [[Config]] interface.
  *
  * The config is bound as an eager singleton so that errors in the config are detected
  * as early as possible.
  */
class ConfigModule extends AbstractModule with ScalaModule {

  override def configure() {
    bind[Config].toProvider[ConfigProvider].asEagerSingleton()
  }

}