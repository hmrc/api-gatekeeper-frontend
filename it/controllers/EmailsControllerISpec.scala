package controllers

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.play.ServerProvider
import play.api.http.HeaderNames.USER_AGENT
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.test.Helpers.OK
import support.{AuthService, ServerBaseISpec}
import views.emails.EmailLandingViewHelper


class EmailsControllerISpec extends ServerBaseISpec with BeforeAndAfterEach with AuthService with EmailLandingViewHelper {
  this: Suite with ServerProvider =>

  protected override def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.port" -> wireMockPort,
        "metrics.enabled" -> true,
        "auditing.enabled" -> false,
        "auditing.consumer.baseUri.host" -> wireMockHost,
        "auditing.consumer.baseUri.port" -> wireMockPort
      )

  val url = s"http://localhost:$port"

  val wsClient: WSClient = app.injector.instanceOf[WSClient]
  val validHeaders = List(USER_AGENT -> "api-subscription-fields")


  def callGetEndpoint(url: String, headers: List[(String, String)]): WSResponse =
    wsClient
      .url(url)
      .withHttpHeaders(headers: _*)
      .get()
      .futureValue

  "EmailsController" when {

    "GET /api-gatekeeper/emails" should {
      "respond with 200 and render landingPage correctly when authorised" in {
        primeAuthServiceSuccess()
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails", validHeaders)
        result.status mustBe OK
        val document: Document = Jsoup.parse(result.body)
        validateLandingPage(document)

        //verify auth called???
      }

    }
  }

}
