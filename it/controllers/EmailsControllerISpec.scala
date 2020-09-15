package controllers

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.play.ServerProvider
import play.api.http.HeaderNames.{CONTENT_TYPE, LOCATION}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.test.Helpers.{FORBIDDEN, OK, SEE_OTHER}
import model.{APIDefinition, APIStatus, APIVersion, DropDownValue, User}
import support.{ApiDefinitionService, ApplicationService, AuthService, DeveloperService, ServerBaseISpec}
import utils.UserFunctionsWrapper
import views.emails.{EmailAllUsersViewHelper, EmailApiSubscriptionsViewHelper, EmailInformationViewHelper, EmailLandingViewHelper}
import views.html.emails.EmailAllUsersView



class EmailsControllerISpec extends ServerBaseISpec with BeforeAndAfterEach with UserFunctionsWrapper
  with ApplicationService with AuthService with DeveloperService with ApiDefinitionService with EmailLandingViewHelper with EmailInformationViewHelper
  with EmailAllUsersViewHelper with EmailApiSubscriptionsViewHelper{
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
        "microservice.services.third-party-developer.port" -> wireMockPort,
        "microservice.services.api-definition-production.host" ->  wireMockHost,
        "microservice.services.api-definition-production.port" -> wireMockPort,
        "microservice.services.api-definition-sandbox.host" ->  wireMockHost,
        "microservice.services.api-definition-sandbox.port" -> wireMockPort,
        "microservice.services.third-party-application-production.host" ->  wireMockHost,
        "microservice.services.third-party-application-production.port" -> wireMockPort,
        "microservice.services.third-party-application-sandbox.host" ->  wireMockHost,
        "microservice.services.third-party-application-sandbox.port" -> wireMockPort
      )

  val url = s"http://localhost:$port"

  val wsClient: WSClient = app.injector.instanceOf[WSClient]
  val validHeaders = List(CONTENT_TYPE -> "application/x-www-form-urlencoded")

  val verifiedUser1 = User("user1@hmrc.com", "userA", "1", verified = Some(true))
  val unverifiedUser1 = User("user2@hmrc.com", "userB", "2", verified = Some(false))
  val verifiedUser2 = User("user3@hmrc.com", "userC", "3", verified = Some(true))

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

        val returnedUsers = Seq(verifiedUser1, unverifiedUser1, verifiedUser2)
        val expectedUsers = Seq(verifiedUser1, verifiedUser2)
         "respond with 200 and render the all users page correctly on initial load when authorised" in {
            primeAuthServiceSuccess()
            primeDeveloperServiceAllSuccessWithUsers(Seq.empty)
            val result = callGetEndpoint(s"$url/api-gatekeeper/emails/all-users", validHeaders)
            result.status mustBe OK
            val document: Document = Jsoup.parse(result.body)
            validateEmailAllUsersPage(document)
            validateResultsTable(document, Seq.empty)
          }


         "respond with 200 and render the all users page correctly when authorised and users returned from developer connector" in {
            primeAuthServiceSuccess()
            primeDeveloperServiceAllSuccessWithUsers(returnedUsers)
            val result = callGetEndpoint(s"$url/api-gatekeeper/emails/all-users", validHeaders)
            result.status mustBe OK
            val document: Document = Jsoup.parse(result.body)
            validateEmailAllUsersPage(document)
            validateResultsTable(document, expectedUsers)
          }

          "respond with 401 when not authorised" in {
            primeAuthServiceFail()
           
            val result = callGetEndpoint(s"$url/api-gatekeeper/emails/all-users", validHeaders)
            result.status mustBe FORBIDDEN
            
          }
     }

     "GET /emails/api-subscribers " should {
        def simpleAPIDefinition(serviceName: String, name: String, context: String): APIDefinition =
          APIDefinition(serviceName, "url1", name, "desc", context, Seq(APIVersion("1", APIStatus.BETA)), None, None)
        val api1 = simpleAPIDefinition("api-1", "API 1", "api1")
        val api2 = simpleAPIDefinition("api-2", "API 2", "api2")
        val api3 = simpleAPIDefinition("api-3", "API 3", "api3")
        val apis = Seq(api1, api2, api3)
        val users = Seq(verifiedUser1, verifiedUser2)
        
         "respond  with 200 and render the page correctly on initial load when authorised" in {
            primeAuthServiceSuccess()
            primeDefinitionServiceSuccessWithPublicApis(apis)
            primeDefinitionServiceSuccessWithPrivateApis(Seq.empty)
            val result = callGetEndpoint(s"$url/api-gatekeeper/emails/api-subscribers", validHeaders)
            result.status mustBe OK
            val document: Document = Jsoup.parse(result.body)
            validateEmailApiSubscriptionsPage(document, apis)

         }

         "respond  with 200 and render the page with users when selected api sent" in {
           primeAuthServiceSuccess()
           primeDefinitionServiceSuccessWithPublicApis(apis)
           primeDefinitionServiceSuccessWithPrivateApis(Seq.empty)
           primeApplicationServiceSuccessWithUsers(users)
           primeDeveloperServiceGetByEmails(users++Seq(unverifiedUser1))
           val dropdownvalues: Seq[DropDownValue] = getApiVersionsDropDownValues(apis)
           val result = callGetEndpoint(s"$url/api-gatekeeper/emails/api-subscribers?apiVersionFilter=${dropdownvalues.head.value}", validHeaders)
           result.status mustBe OK
           val document: Document = Jsoup.parse(result.body)
           validateEmailApiSubscriptionsPage(document, apis, dropdownvalues.head.value)
           validateResultsTable(document, users)
         }
        //test when application service fails? api definition service fails?
         "respond with 401 when not authorised" in {
            primeAuthServiceFail()
            val result = callGetEndpoint(s"$url/api-gatekeeper/emails/api-subscribers", validHeaders)
            result.status mustBe FORBIDDEN
            
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
