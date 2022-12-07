package uk.gov.hmrc.gatekeeper.support

import java.net.URL

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

case class WireMockBaseUrl(value: URL)

object WireMockSupport {
  // We have to make the wireMockPort constant per-JVM instead of constant
  // per-WireMockSupport-instance because config values containing it are
  // cached in the GGConfig object
  private lazy val wireMockPort = 22222
}

trait WireMockSupport extends BeforeAndAfterAll with BeforeAndAfterEach {
  me: Suite =>

  def commonStubs(): Unit

  val wireMockPort: Int                                           = WireMockSupport.wireMockPort
  val wireMockHost                                                = "localhost"
  val wireMockBaseUrlAsString                                     = s"http://$wireMockHost:$wireMockPort"
  val wireMockBaseUrl                                             = new URL(wireMockBaseUrlAsString)
  protected implicit val implicitWireMockBaseUrl: WireMockBaseUrl = WireMockBaseUrl(wireMockBaseUrl)

  protected def basicWireMockConfig(): WireMockConfiguration = wireMockConfig()

  private val wireMockServer = new WireMockServer(basicWireMockConfig().port(wireMockPort))

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    WireMock.configureFor(wireMockHost, wireMockPort)
    wireMockServer.start()
  }

  override protected def afterAll(): Unit = {
    wireMockServer.stop()
    super.afterAll()
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    WireMock.reset()
    commonStubs()
  }

  protected def stopWireMockServer(): Unit = wireMockServer.stop()

  protected def startWireMockServer(): Unit = wireMockServer.start()
}
