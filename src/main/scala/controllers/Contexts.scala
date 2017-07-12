package controllers

import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import com.google.inject.ImplementedBy

import scala.concurrent.ExecutionContext

@ImplementedBy(classOf[AkkaContexts])
trait Contexts {
  def addressCacheContext: ExecutionContext
}

@Singleton
class AkkaContexts @Inject()(akkaSystem: ActorSystem) extends Contexts {
  val addressCacheContext: ExecutionContext = akkaSystem.dispatchers.lookup("contexts.address-cache")
}
