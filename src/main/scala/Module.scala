import java.util.concurrent.TimeUnit

import com.google.inject.AbstractModule
import model.{AddressCache, AddressCacheExpiring}
import play.api.{Configuration, Environment}

class Module(
  environment: Environment,
  configuration: Configuration) extends AbstractModule {

  override def configure() = {

    val cache: AddressCache = new AddressCacheExpiring(configuration.get[Int]("address-cache.timeout"), TimeUnit.MINUTES)

    bind(classOf[AddressCache])
      .toInstance(cache)
  }
}
