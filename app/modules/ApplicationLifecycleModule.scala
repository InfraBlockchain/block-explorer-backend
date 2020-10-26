package modules

import blockchain.InfraBlockchainStatus
import com.google.inject.AbstractModule

/**
  * Created by bezalel on 13/09/2018.
  */
class ApplicationLifecycleModule extends AbstractModule {

  def configure() = {
    bind(classOf[InfraBlockchainStatus]).asEagerSingleton()
  }
}
