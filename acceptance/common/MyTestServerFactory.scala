package common

import play.api.Mode
import play.api.Application
import play.api.test.DefaultTestServerFactory
import play.core.server.ServerConfig

object MyTestServerFactory extends MyTestServer

class MyTestServer extends DefaultTestServerFactory {
  override protected def serverConfig(app: Application): ServerConfig = {
    val sc = ServerConfig(port = Some(6001), sslPort = Some(6002), mode = Mode.Test, rootDir = app.path)
    sc.copy(configuration = sc.configuration withFallback overrideServerConfiguration(app))
  }
}
