package info.chinnews.system.akkaguice

import akka.actor.{Actor, IndirectActorProducer}
import com.google.inject.name.Names
import com.google.inject.{Injector, Key}

class GuiceActorProducer(val injector: Injector, val actorName: String) extends IndirectActorProducer {

  override def actorClass = classOf[Actor]

  override def produce() =
    injector.getBinding(Key.get(classOf[Actor], Names.named(actorName))).getProvider.get()

}