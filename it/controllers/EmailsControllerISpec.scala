package controllers

import model._
import org.jsoup.Jsoup
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.play.ServerProvider
import play.api.http.HeaderNames.{CONTENT_TYPE, LOCATION}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.test.Helpers.{FORBIDDEN, OK, SEE_OTHER}
import support._
import utils.UserFunctionsWrapper
import views.emails.EmailsPagesHelper



class EmailsControllerISpec extends ServerBaseISpec with BeforeAndAfterEach with UserFunctionsWrapper
  with ApplicationServiceStub with AuthServiceStub with DeveloperServiceStub with APIDefinitionServiceStub with EmailsPagesHelper {
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

  val verifiedUser1 = RegisteredUser("user1@hmrc.com", UserId.random, "userA", "1", verified = true)
  val unverifiedUser1 = RegisteredUser("user2@hmrc.com", UserId.random, "userB", "2", verified = false)
  val verifiedUser2 = RegisteredUser("user3@hmrc.com", UserId.random, "userC", "3", verified = true)

  val verifiedUsers = Seq(verifiedUser1, verifiedUser2)
  val allUsers = Seq(verifiedUser1, verifiedUser2, unverifiedUser1)

  val api1 = simpleAPIDefinition("api-1", "API 1", "api1", None, "1")
  val api2 = simpleAPIDefinition("api-2", "API 2", "api2", Some(List("CATEGORY1", "VAT")), "1")
  val api3 = simpleAPIDefinition("api-3", "API 3", "api3", Some(List("TAX", "VAT")), "1")
  val api4 = simpleAPIDefinition("api-4", "API 4", "api4", None, "1")
  val api5 = simpleAPIDefinition("api-5", "API 5", "api5", None, "1")
  val api6 = simpleAPIDefinition("api-6", "API 6", "api6", None, "1")
  val apis = List(api1, api2, api3)

  def callGetEndpoint(url: String, headers: List[(String, String)]): WSResponse =
    wsClient
      .url(url)
      .withHttpHeaders(headers: _*)
      .withFollowRedirects(false)
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

        validateLandingPage(Jsoup.parse(result.body))
      }

       "respond with 403 and when not authorised" in {
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

      "respond with 403 and when not authorised" in {
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

        validateApiSubcriptionInformationPage(Jsoup.parse(result.body))
      }

      "respond with 200 and render all-users information page correctly when authorised" in {
        primeAuthServiceSuccess()
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/all-users/information", validHeaders)
        result.status mustBe OK

        validateAllUsersInformationPage(Jsoup.parse(result.body))
      }

      "respond with 403 and for all-users when not authorised" in {
        primeAuthServiceFail()
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/all-users/information", validHeaders)
        result.status mustBe FORBIDDEN
      }

      "respond with 403 and for api-subscription when not authorised" in {
        primeAuthServiceFail()
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/api-subscription/information", validHeaders)
        result.status mustBe FORBIDDEN
      }

     }

     "GET  /emails/all-users" should {

         "respond with 200 and render the all users page correctly on initial load when authorised" in {
            primeAuthServiceSuccess()
            primeDeveloperServiceAllSuccessWithUsers(Seq.empty)
            val result = callGetEndpoint(s"$url/api-gatekeeper/emails/all-users", validHeaders)
            result.status mustBe OK

            validateEmailAllUsersPage(Jsoup.parse(result.body), Seq.empty)  
          }


         "respond with 200 and render the all users page correctly when authorised and users returned from developer connector" in {
            primeAuthServiceSuccess()
            primeDeveloperServiceAllSuccessWithUsers(allUsers)
            val result = callGetEndpoint(s"$url/api-gatekeeper/emails/all-users", validHeaders)
            result.status mustBe OK

            validateEmailAllUsersPage(Jsoup.parse(result.body), verifiedUsers)
          }

          "respond with 403 when not authorised" in {
            primeAuthServiceFail()
           
            val result = callGetEndpoint(s"$url/api-gatekeeper/emails/all-users", validHeaders)
            result.status mustBe FORBIDDEN   
          }
     }

     "GET /emails/api-subscribers " should {
    
 
        
         "respond  with 200 and render the page correctly on initial load when authorised" in {
            primeAuthServiceSuccess()
            primeDefinitionServiceSuccessWithPublicAPIs(apis)
            primeDefinitionServiceSuccessWithPrivateAPIs(Seq.empty)
            val result = callGetEndpoint(s"$url/api-gatekeeper/emails/api-subscribers", validHeaders)
            result.status mustBe OK
            
            validateEmailAPISubscriptionsPage(Jsoup.parse(result.body), apis)

         }

         "respond  with 200 and render the page with users when selected api sent" in {
           primeAuthServiceSuccess()
           primeDefinitionServiceSuccessWithPublicAPIs(apis)
           primeDefinitionServiceSuccessWithPrivateAPIs(Seq.empty)
           primeApplicationServiceSuccessWithUsers(allUsers)
           primeDeveloperServiceGetByEmails(allUsers++Seq(unverifiedUser1))
           val dropdownvalues: Seq[DropDownValue] = getApiVersionsDropDownValues(apis)
           val result = callGetEndpoint(s"$url/api-gatekeeper/emails/api-subscribers?apiVersionFilter=${dropdownvalues.head.value}", validHeaders)
           result.status mustBe OK

           validateEmailAPISubscriptionsPage(Jsoup.parse(result.body), apis, dropdownvalues.head.value, verifiedUsers)
           
         }
        //test when application service fails? api definition service fails?
         "respond with 403 when not authorised" in {
            primeAuthServiceFail()
            val result = callGetEndpoint(s"$url/api-gatekeeper/emails/api-subscribers", validHeaders)
            result.status mustBe FORBIDDEN
            
          }
     }

    "GET /emails/email-preferences " should {

      "respond  with 200 and render the page correctly on initial load when authorised" in {
        primeAuthServiceSuccess()

        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/email-preferences", validHeaders)
        result.status mustBe OK

        validateEmailPreferencesChoicePage(Jsoup.parse(result.body))
      }

      "respond with 403 when not authorised" in {
        primeAuthServiceFail()
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/email-preferences", validHeaders)
        result.status mustBe FORBIDDEN

      }
    }

    "POST /emails/email-preferences " should {
      "redirect to select page when SPECIFIC_API passed in the form" in {
        primeAuthServiceSuccess()
        val result = callPostEndpoint(s"$url/api-gatekeeper/emails/email-preferences", validHeaders, "sendEmailPreferences=SPECIFIC_API")
        validateRedirect(result, "/api-gatekeeper/emails/email-preferences/select-api")
      }

      "redirect to select page when TAX_REGIME passed in the form" in {
        primeAuthServiceSuccess()
        val result = callPostEndpoint(s"$url/api-gatekeeper/emails/email-preferences", validHeaders, "sendEmailPreferences=TAX_REGIME")
        validateRedirect(result, "/api-gatekeeper/emails/email-preferences/by-api-category")
      }

      "redirect to select page when TOPIC passed in the form" in {
        primeAuthServiceSuccess()
        val result = callPostEndpoint(s"$url/api-gatekeeper/emails/email-preferences", validHeaders, "sendEmailPreferences=TOPIC")
        validateRedirect(result, "/api-gatekeeper/emails/email-preferences/by-topic")
      }

      "respond with 403 when not authorised" in {
        primeAuthServiceFail()
        val result = callPostEndpoint(s"$url/api-gatekeeper/emails/email-preferences", validHeaders, "sendEmailPreferences=TOPIC")
        result.status mustBe FORBIDDEN
      }

    }

    "GET /emails/email-preferences/by-topic" should {

      "respond with 200 and render the page correctly on initial load when authorised" in {
        primeAuthServiceSuccess()
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/email-preferences/by-topic", validHeaders)
        result.status mustBe OK

        validateEmailPreferencesTopicPage(Jsoup.parse(result.body))
      } 

      "respond with 200 and render the page correctly when selected topic provided" in {
        primeAuthServiceSuccess()
        primeDeveloperServiceEmailPreferencesByTopic(allUsers, TopicOptionChoice.BUSINESS_AND_POLICY)
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/email-preferences/by-topic?selectedTopic=${TopicOptionChoice.BUSINESS_AND_POLICY}", validHeaders)
        result.status mustBe OK

        validateEmailPreferencesTopicResultsPage(Jsoup.parse(result.body),  TopicOptionChoice.BUSINESS_AND_POLICY, verifiedUsers)  
      } 

      "respond with 200 and render the page correctly when selected topic provided but no users returned" in {
        primeAuthServiceSuccess()
        primeDeveloperServiceEmailPreferencesByTopic(Seq.empty, TopicOptionChoice.BUSINESS_AND_POLICY)
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/email-preferences/by-topic?selectedTopic=${TopicOptionChoice.BUSINESS_AND_POLICY}", validHeaders)
        result.status mustBe OK

        validateEmailPreferencesTopicResultsPage(Jsoup.parse(result.body),  TopicOptionChoice.BUSINESS_AND_POLICY, Seq.empty) 
      } 

      "respond with 403 when not authorised" in {
        primeAuthServiceFail()
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/email-preferences/by-topic", validHeaders)
        result.status mustBe FORBIDDEN
      }
    } 
    
    "GET /emails/email-preferences/by-api-category" should {
      val categories = List(APICategoryDetails("category1", "name1"), APICategoryDetails("category2", "name2"),  APICategoryDetails("category3", "name3"))
      
      "respond with 200 and render the page correctly on initial load when authorised" in {
        primeAuthServiceSuccess()
        primeGetAllCategories(categories)
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/email-preferences/by-api-category", validHeaders)
        result.status mustBe OK

        validateEmailPreferencesAPICategoryPage(Jsoup.parse(result.body), categories)
      } 

      "respond with 200 and render the page correctly when only category filter is provided" in {
        primeAuthServiceSuccess()
        primeGetAllCategories(categories)
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/email-preferences/by-api-category?selectedCategory=${categories.head.category}", validHeaders)
        result.status mustBe OK

        validateEmailPreferencesAPICategoryPageWithCategoryFilter(Jsoup.parse(result.body), categories, categories.head)
      } 

      "respond with 200 and render the page correctly when category and topic filter provided but no users returned" in {
        primeAuthServiceSuccess()
        primeGetAllCategories(categories)
        primeDeveloperServiceEmailPreferencesByTopicAndCategory(Seq.empty, TopicOptionChoice.BUSINESS_AND_POLICY, categories.head)
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/email-preferences/by-api-category?selectedTopic=${TopicOptionChoice.BUSINESS_AND_POLICY}&selectedCategory=${categories.head.category}", validHeaders)
        result.status mustBe OK

        validateEmailPreferencesAPICategoryResultsPage(Jsoup.parse(result.body), categories, Some(categories.head), TopicOptionChoice.BUSINESS_AND_POLICY, Seq.empty)
      } 

     "respond with 200 and render the page correctly when category and topic filter provided and some users returned" in {
        primeAuthServiceSuccess()
        primeGetAllCategories(categories)
        primeDeveloperServiceEmailPreferencesByTopicAndCategory(allUsers, TopicOptionChoice.BUSINESS_AND_POLICY, categories.head)
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/email-preferences/by-api-category?selectedTopic=${TopicOptionChoice.BUSINESS_AND_POLICY}&selectedCategory=${categories.head.category}", validHeaders)
        result.status mustBe OK

        validateEmailPreferencesAPICategoryResultsPage(Jsoup.parse(result.body), categories, Some(categories.head), TopicOptionChoice.BUSINESS_AND_POLICY, verifiedUsers)
      } 

      "respond with 403 when not authorised" in {
        primeAuthServiceFail()
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/email-preferences/by-api-category", validHeaders)
        result.status mustBe FORBIDDEN
      }
    }

    "GET /emails/email-preferences/select-api" should {

       "respond with 200 and render the page correctly on initial load when authorised" in {
        primeAuthServiceSuccess()
        primeDefinitionServiceSuccessWithPublicAPIs(Seq.empty)
        primeDefinitionServiceSuccessWithPrivateAPIs(apis)
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/email-preferences/select-api", validHeaders)
        result.status mustBe OK

        validateSelectAPIPageWithNonePreviouslySelected(Jsoup.parse(result.body), apis)
      } 


      "respond with 200 and render the page correctly when selectedAPis provided" in {
        val selectedApis = Seq(api4, api5, api6)

        primeAuthServiceSuccess()
        primeDefinitionServiceSuccessWithPublicAPIs(Seq.empty)
        primeDefinitionServiceSuccessWithPrivateAPIs(apis++selectedApis)
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/email-preferences/select-api?${selectedApis.map("selectedAPIs="+_.serviceName).mkString("&")}", validHeaders)
        result.status mustBe OK

        validateSelectAPIPageWithPreviouslySelectedAPIs(Jsoup.parse(result.body), apis, selectedApis)
      } 

      "respond with 403 when not authorised" in {
        primeAuthServiceFail()
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/email-preferences/select-api", validHeaders)
        result.status mustBe FORBIDDEN
      }
    }

    "GET /emails/email-preferences/by-specific-api" should {
        val selectedApis = Seq(api4, api5, api6)

     "respond with 200 and render the page correctly on initial load with selectedApis" in {
        primeAuthServiceSuccess()
        primeDefinitionServiceSuccessWithPublicAPIs(Seq.empty)
        primeDefinitionServiceSuccessWithPrivateAPIs(apis++selectedApis)
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/email-preferences/by-specific-api?${selectedApis.map("selectedAPIs="+_.serviceName).mkString("&")}", validHeaders)
        result.status mustBe OK

        validateEmailPreferencesSpecificAPIPage(Jsoup.parse(result.body), selectedApis)
      } 


     "redirect to select api page when no selectedApis in query params" in {
        primeAuthServiceSuccess()
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/email-preferences/by-specific-api", validHeaders)
        validateRedirect(result, "/api-gatekeeper/emails/email-preferences/select-api")

      } 

      "respond with 200 and render the page with users table with selectedApis" in {
        primeAuthServiceSuccess()
        primeDefinitionServiceSuccessWithPublicAPIs(Seq.empty)
        primeDefinitionServiceSuccessWithPrivateAPIs(apis++selectedApis)
        primeDeveloperServiceEmailPreferencesBySelectedAPisTopicAndCategory(allUsers, apis, TopicOptionChoice.BUSINESS_AND_POLICY)

        val result =
          callGetEndpoint(s"$url/api-gatekeeper/emails/email-preferences/by-specific-api?selectedTopic=${TopicOptionChoice.BUSINESS_AND_POLICY.toString}${apis.map("&selectedAPIs="+_.serviceName).mkString}", validHeaders)

        validateEmailPreferencesSpecificAPIResults(Jsoup.parse(result.body), TopicOptionChoice.BUSINESS_AND_POLICY, apis, verifiedUsers, usersToEmailCopyText(verifiedUsers))
      } 


      "respond with 200 and render the page with selectedApis but no users" in {
        primeAuthServiceSuccess()
        primeDefinitionServiceSuccessWithPublicAPIs(Seq.empty)
        primeDefinitionServiceSuccessWithPrivateAPIs(apis++selectedApis)
        primeDeveloperServiceEmailPreferencesBySelectedAPisTopicAndCategory(Seq.empty, apis, TopicOptionChoice.BUSINESS_AND_POLICY)

        val result =
          callGetEndpoint(s"$url/api-gatekeeper/emails/email-preferences/by-specific-api?selectedTopic=${TopicOptionChoice.BUSINESS_AND_POLICY.toString}${apis.map("&selectedAPIs="+_.serviceName).mkString}", validHeaders)

        validateEmailPreferencesSpecificAPIResults(Jsoup.parse(result.body), TopicOptionChoice.BUSINESS_AND_POLICY, apis, Seq.empty, "")
      }

       "respond with 403 when not authorised" in {
        primeAuthServiceFail()
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/email-preferences/by-specific-api?${selectedApis.map("&selectedAPIs="+_.serviceName).mkString}", validHeaders)
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
