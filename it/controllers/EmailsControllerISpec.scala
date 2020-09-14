package controllers

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.play.ServerProvider
import play.api.http.HeaderNames.{CONTENT_TYPE, LOCATION}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.test.Helpers.{FORBIDDEN, OK, SEE_OTHER}
import model.User
import support.{AuthService, DeveloperService, ServerBaseISpec}
import views.emails.{EmailLandingViewHelper, EmailInformationViewHelper}


class EmailsControllerISpec extends ServerBaseISpec with BeforeAndAfterEach with AuthService with DeveloperService
with EmailLandingViewHelper with EmailInformationViewHelper {
  this: Suite with ServerProvider =>

  protected override def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.port" -> wireMockPort,
        "metrics.enabled" -> true,
        "auditing.enabled" -> false,
        "auditing.consumer.baseUri.host" -> wireMockHost,
        "auditing.consumer.baseUri.port" -> wireMockPort,
        "microservice.services.third-party-developer.host" ->  wireMockHost,
        "microservice.services.third-party-developer.port" -> wireMockPort
      )

  val url = s"http://localhost:$port"

  val wsClient: WSClient = app.injector.instanceOf[WSClient]
  val validHeaders = List(CONTENT_TYPE -> "application/x-www-form-urlencoded")


  def callGetEndpoint(url: String, headers: List[(String, String)]): WSResponse =
    wsClient
      .url(url)
      .withHttpHeaders(headers: _*)
      .get()
      .futureValue

    def callPostEndpoint(url: String, headers: List[(String, String)], body: String): WSResponse =
    wsClient
      .url(url)
      .withHttpHeaders(headers: _*)
      .withFollowRedirects(false)
      .post(body)
      .futureValue  

  "EmailsController" when {

    "GET /api-gatekeeper/emails" should {
      "respond with 200 and render landingPage correctly when authorised" in {
        primeAuthServiceSuccess()
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails", validHeaders)
        result.status mustBe OK
        val document: Document = Jsoup.parse(result.body)
        validateLandingPage(document)
      }

       "respond with 401 and when not authorised" in {
        primeAuthServiceFail()
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails", validHeaders)
        result.status mustBe FORBIDDEN
      }

    }

     "POST /api-gatekeeper/emails" should {
      "redirect to subscription information page when EMAIL_PREFERENCES passed in the form" in {
        primeAuthServiceSuccess()
        val result = callPostEndpoint(s"$url/api-gatekeeper/emails", validHeaders, "sendEmailChoice=EMAIL_PREFERENCES")
        validateRedirect(result, "/api-gatekeeper/emails/email-preferences")
      }

      "redirect to subscription information page when API_SUBSCRIPTION passed in the form" in {
        primeAuthServiceSuccess()
        val result = callPostEndpoint(s"$url/api-gatekeeper/emails", validHeaders, "sendEmailChoice=API_SUBSCRIPTION")
       validateRedirect(result, "/api-gatekeeper/emails/api-subscription/information")
      }

      "redirect to email all users information page when EMAIL_ALL_USERS passed in the form" in {
        primeAuthServiceSuccess()
        val result = callPostEndpoint(s"$url/api-gatekeeper/emails", validHeaders, "sendEmailChoice=EMAIL_ALL_USERS")
        validateRedirect(result, "/api-gatekeeper/emails/all-users/information")
      }

      "respond with 401 and when not authorised" in {
        primeAuthServiceFail()
        val result = callPostEndpoint(s"$url/api-gatekeeper/emails", validHeaders, "")
        result.status mustBe FORBIDDEN
      }
     }

     "GET  /emails/:emailChoice/information " should {
      "respond with 200 and render api-subscription information page correctly when authorised" in {
        primeAuthServiceSuccess()
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/api-subscription/information", validHeaders)
        result.status mustBe OK
        val document: Document = Jsoup.parse(result.body)
        validateApiSubcriptionInformationPage(document)
      }

      "respond with 200 and render all-users information page correctly when authorised" in {
        primeAuthServiceSuccess()
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/all-users/information", validHeaders)
        result.status mustBe OK
        val document: Document = Jsoup.parse(result.body)
        validateAllUsersInformationPage(document)
      }

      "respond with 401 and for all-users when not authorised" in {
        primeAuthServiceFail()
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/all-users/information", validHeaders)
        result.status mustBe FORBIDDEN
      }

      "respond with 401 and for api-subscription when not authorised" in {
        primeAuthServiceFail()
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/api-subscription/information", validHeaders)
        result.status mustBe FORBIDDEN
      }

     }

     "GET  /emails/all-users" should {
        val user1 = User("user1@hmrc.com", "userA", "1", verified = Some(true))
        
         "respond with 200 and render the all users page correctly when authorised" in {
            primeAuthServiceSuccess()
            primeDeveloperServiceAllSuccessWithUsers(Seq(user1))
            val result = callGetEndpoint(s"$url/api-gatekeeper/emails/all-users", validHeaders)
            result.status mustBe OK
            val document: Document = Jsoup.parse(result.body)
            validateApiSubcriptionInformationPage(document)
          }
     }

     def validateRedirect(response: WSResponse, expectedLocation: String){
        response.status mustBe SEE_OTHER
        val mayBeLocationHeader: Option[Seq[String]] = response.headers.get(LOCATION)
        mayBeLocationHeader.fold(fail("redirect Location header missing")){ locationHeader => 
          locationHeader.head mustBe expectedLocation}
     }
  }

}
