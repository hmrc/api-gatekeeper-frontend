/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.gatekeeper.controllers

import org.jsoup.Jsoup
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.play.ServerProvider
import play.api.http.HeaderNames.{CONTENT_TYPE, LOCATION}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.test.Helpers.{FORBIDDEN, OK, SEE_OTHER}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.developers.domain.models.UserId
import uk.gov.hmrc.gatekeeper.models.APIAccessType.PUBLIC
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.support._
import uk.gov.hmrc.gatekeeper.utils.{MockCookies, UserFunctionsWrapper}
import uk.gov.hmrc.gatekeeper.views.emails.EmailsPagesHelper

class EmailsPreferencesControllerISpec extends ServerBaseISpec with BeforeAndAfterEach with UserFunctionsWrapper
    with ApplicationServiceStub with AuthServiceStub with DeveloperServiceStub with APIDefinitionServiceStub with EmailsPagesHelper with ApmServiceStub {
  this: Suite with ServerProvider =>

  override protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.host"                               -> wireMockHost,
        "microservice.services.auth.port"                               -> wireMockPort,
        "metrics.enabled"                                               -> true,
        "auditing.enabled"                                              -> false,
        "auditing.consumer.baseUri.host"                                -> wireMockHost,
        "auditing.consumer.baseUri.port"                                -> wireMockPort,
        "microservice.services.third-party-developer.host"              -> wireMockHost,
        "microservice.services.third-party-developer.port"              -> wireMockPort,
        "microservice.services.api-definition-production.host"          -> wireMockHost,
        "microservice.services.api-definition-production.port"          -> wireMockPort,
        "microservice.services.api-definition-sandbox.host"             -> wireMockHost,
        "microservice.services.api-definition-sandbox.port"             -> wireMockPort,
        "microservice.services.third-party-application-production.host" -> wireMockHost,
        "microservice.services.third-party-application-production.port" -> wireMockPort,
        "microservice.services.third-party-application-sandbox.host"    -> wireMockHost,
        "microservice.services.third-party-application-sandbox.port"    -> wireMockPort,
        "microservice.services.api-platform-microservice.host"          -> wireMockHost,
        "microservice.services.api-platform-microservice.port"          -> wireMockPort
      )

  val url = s"http://localhost:$port"

  val wsClient: WSClient = app.injector.instanceOf[WSClient]
  val validHeaders       = List(CONTENT_TYPE -> "application/x-www-form-urlencoded", "Csrf-Token" -> "nocheck")

  val verifiedUser1   = RegisteredUser("user1@hmrc.com".toLaxEmail, UserId.random, "userA", "1", verified = true)
  val unverifiedUser1 = RegisteredUser("user2@hmrc.com".toLaxEmail, UserId.random, "userB", "2", verified = false)
  val verifiedUser2   = RegisteredUser("user3@hmrc.com".toLaxEmail, UserId.random, "userC", "3", verified = true)

  val verifiedUsers = Seq(verifiedUser1, verifiedUser2)
  val allUsers      = Seq(verifiedUser1, verifiedUser2, unverifiedUser1)

  val api1 = simpleAPIDefinition("api-1", "API 1", "api1", None, "1")
  val api2 = simpleAPIDefinition("api-2", "API 2", "api2", Some(List("CATEGORY1", "VAT")), "1")
  val api3 = simpleAPIDefinition("api-3", "API 3", "api3", Some(List("TAX", "VAT")), "1")
  val api4 = simpleAPIDefinition("api-4", "API 4", "api4", None, "1")
  val api5 = simpleAPIDefinition("api-5", "API 5", "api5", None, "1")
  val api6 = simpleAPIDefinition("api-6", "API 6", "api6", None, "1")
  val apis = List(api1, api2, api3)

  val combinedApi1 = simpleAPI("api-1", "API 1", List.empty, ApiType.REST_API, Some(PUBLIC))
  val combinedApi2 = simpleAPI("api-2", "API 2", List("CATEGORY1", "VAT"), ApiType.REST_API, Some(PUBLIC))
  val combinedApi3 = simpleAPI("api-3", "API 3", List("TAX", "VAT"), ApiType.REST_API, Some(PUBLIC))
  val combinedApi4 = simpleAPI("api-4", "API 4", List.empty, ApiType.REST_API, Some(PUBLIC))
  val combinedApi5 = simpleAPI("api-5", "API 5", List.empty, ApiType.REST_API, Some(PUBLIC))
  val combinedApi6 = simpleAPI("api-6", "API 6", List.empty, ApiType.XML_API, Some(PUBLIC))
  val combinedApis = List(combinedApi1, combinedApi2, combinedApi3)

  def callGetEndpoint(url: String, headers: List[(String, String)]): WSResponse =
    wsClient
      .url(url)
      .withHttpHeaders(headers: _*)
      .withCookies(MockCookies.makeWsCookie(app))
      .withFollowRedirects(false)
      .get()
      .futureValue

  def callPostEndpoint(url: String, headers: List[(String, String)], body: String): WSResponse =
    wsClient
      .url(url)
      .withHttpHeaders(headers: _*)
      .withCookies(MockCookies.makeWsCookie(app))
      .withFollowRedirects(false)
      .post(body)
      .futureValue

  "EmailsPreferenceController" when {

    "GET  /emails/:emailChoice/information" should {
      "respond with 200 and render api-subscription information page correctly when authorised" in {
        primeAuthServiceSuccess()
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/api-subscription/information", validHeaders)
        result.status shouldBe OK

        validateApiSubcriptionInformationPage(Jsoup.parse(result.body))
      }

      "respond with 403 and for api-subscription when not authorised" in {
        primeAuthServiceFail()
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/api-subscription/information", validHeaders)
        result.status shouldBe FORBIDDEN
      }

      "respond with 200 and render all-users information page correctly when authorised" in {
        primeAuthServiceSuccess()
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/all-users/information", validHeaders)
        result.status shouldBe OK

        validateAllUsersInformationPage(Jsoup.parse(result.body))
      }

      "respond with 403 and for all-users when not authorised" in {
        primeAuthServiceFail()
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/all-users/information", validHeaders)
        result.status shouldBe FORBIDDEN
      }
    }

    "GET  /emails/all-users" should {

      "respond with 200 and render the all users page correctly on initial load when authorised" in {
        primeAuthServiceSuccess()
        primeDeveloperServiceAllSuccessWithUsersPaginated(0, Seq.empty)
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/all-users", validHeaders)
        result.status shouldBe OK

        validateEmailAllUsersPaginatedPage(Jsoup.parse(result.body), 0, Seq.empty)
      }

      "respond with 200 and render the all users page correctly when authorised and users returned from developer connector" in {
        primeAuthServiceSuccess()
        primeDeveloperServiceAllSuccessWithUsersPaginated(15, allUsers)
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/all-users", validHeaders)
        result.status shouldBe OK

        validateEmailAllUsersPaginatedPage(Jsoup.parse(result.body), 15, verifiedUsers)
      }

      "respond with 403 when not authorised" in {
        primeAuthServiceFail()

        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/all-users", validHeaders)
        result.status shouldBe FORBIDDEN
      }
    }

    "GET /emails" should {

      "respond  with 200 and render the page correctly on initial load when authorised" in {
        primeAuthServiceSuccess()

        val result = callGetEndpoint(s"$url/api-gatekeeper/emails", validHeaders)
        result.status shouldBe OK

        validateEmailPreferencesChoicePage(Jsoup.parse(result.body))
      }

      "respond with 403 when not authorised" in {
        primeAuthServiceFail()
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails", validHeaders)
        result.status shouldBe FORBIDDEN
      }
    }

    "POST /emails" should {
      "redirect to select page when SPECIFIC_API passed in the form" in {
        primeAuthServiceSuccess()
        val result = callPostEndpoint(s"$url/api-gatekeeper/emails", validHeaders, "sendEmailPreferences=SPECIFIC_API")
        validateRedirect(result, "/api-gatekeeper/emails/email-preferences/select-api")
      }

      "redirect to select tax regime page when TAX_REGIME passed in the form" in {
        primeAuthServiceSuccess()
        val result = callPostEndpoint(s"$url/api-gatekeeper/emails", validHeaders, "sendEmailPreferences=TAX_REGIME")
        validateRedirect(result, "/api-gatekeeper/emails/email-preferences/select-tax-regime")
      }

      "redirect to select page when TOPIC passed in the form" in {
        primeAuthServiceSuccess()
        val result = callPostEndpoint(s"$url/api-gatekeeper/emails", validHeaders, "sendEmailPreferences=TOPIC")
        validateRedirect(result, "/api-gatekeeper/emails/email-preferences/select-user-topic")
      }

      "respond with 403 when not authorised" in {
        primeAuthServiceFail()
        val result = callPostEndpoint(s"$url/api-gatekeeper/emails", validHeaders, "sendEmailPreferences=TOPIC")
        result.status shouldBe FORBIDDEN
      }
    }

    "GET /emails/email-preferences/select-user-topic" should {

      "respond with 200 and render the page correctly on initial load when authorised" in {
        primeAuthServiceSuccess()
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/email-preferences/select-user-topic", validHeaders)
        result.status shouldBe OK

        validateEmailPreferencesSelectTopicPage(Jsoup.parse(result.body))
      }

      "respond with 200 and render the page correctly when selected topic provided" in {
        primeAuthServiceSuccess()
        primeDeveloperServiceEmailPreferencesByTopic(allUsers, TopicOptionChoice.BUSINESS_AND_POLICY)
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/email-preferences/select-user-topic?selectedTopic=${TopicOptionChoice.BUSINESS_AND_POLICY}", validHeaders)
        result.status shouldBe OK

        validateEmailPreferencesSelectTopicPage(Jsoup.parse(result.body))
      }

      "respond with 200 and render the page correctly when selected topic provided but no users returned" in {
        primeAuthServiceSuccess()
        primeDeveloperServiceEmailPreferencesByTopic(Seq.empty, TopicOptionChoice.BUSINESS_AND_POLICY)
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/email-preferences/select-user-topic?selectedTopic=${TopicOptionChoice.BUSINESS_AND_POLICY}", validHeaders)
        result.status shouldBe OK

        validateEmailPreferencesSelectTopicPage(Jsoup.parse(result.body))
      }

      "respond with 403 when not authorised" in {
        primeAuthServiceFail()
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/email-preferences/select-user-topic", validHeaders)
        result.status shouldBe FORBIDDEN
      }
    }

    "GET /emails/email-preferences/select-subscribed-api " should {

      "respond with 200 and render the page correctly on initial load when authorised" in {
        primeAuthServiceSuccess()
        primeFetchAllCombinedApisSuccess(combinedApis)

        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/email-preferences/select-subscribed-api", validHeaders)
        result.status shouldBe OK

        validateEmailPreferencesSelectSubscribedApiPage(Jsoup.parse(result.body), apis)
      }

      "respond with 200 and render the page correctly when selected api provided" in {
        primeAuthServiceSuccess()
        primeFetchAllCombinedApisSuccess(combinedApis)

        val selectedApis = List(combinedApi2, combinedApi3)

        val result =
          callGetEndpoint(s"$url/api-gatekeeper/emails/email-preferences/by-subscribed-api?${selectedApis.map("selectedAPIs=" + _.serviceName).mkString("&")}", validHeaders)
        result.status shouldBe OK

        validateSubscribedAPIPageWithPreviouslySelectedAPIs(Jsoup.parse(result.body), combinedApis, selectedApis)
      }

      "respond with 403 when not authorised" in {
        primeAuthServiceFail()
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/email-preferences/select-subscribed-api", validHeaders)
        result.status shouldBe FORBIDDEN
      }
    }

    "GET /emails/email-preferences/select-tax-regime" should {
      val categories = List(APICategoryDetails("category1", "name1"), APICategoryDetails("category2", "name2"), APICategoryDetails("category3", "name3"))

      "respond with 200 and render the page correctly on initial load when authorised" in {
        primeAuthServiceSuccess()
        primeGetAllCategories(categories)
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/email-preferences/select-tax-regime", validHeaders)
        result.status shouldBe OK

        validateEmailPreferencesSelectTaxRegimeResultsPage(Jsoup.parse(result.body), categories, "/api-gatekeeper/emails/email-preferences/by-specific-tax-regime")
      }

      "respond with 303 and render the page with selected user tax regime" in {
        primeAuthServiceSuccess()
        primeGetAllCategories(categories)
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/email-preferences/add-another-tax-regime-option?selectOption=", validHeaders)

        validateRedirect(result, "/api-gatekeeper/emails/email-preferences/selected-user-tax-regime")
      }

      "respond with 403 when not authorised" in {
        primeAuthServiceFail()
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/email-preferences/select-tax-regime", validHeaders)
        result.status shouldBe FORBIDDEN
      }
    }

    "GET /emails/email-preferences/select-api" should {

      "respond with 200 and render the new page correctly on initial load when authorised" in {
        primeAuthServiceSuccess()
        primeFetchAllCombinedApisSuccess(combinedApis)

        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/email-preferences/select-api", validHeaders)
        result.status shouldBe OK

        validateSelectAPIPageWithNonePreviouslySelectedNew(Jsoup.parse(result.body), combinedApis, "/api-gatekeeper/emails/email-preferences/by-specific-api")
      }

      "respond with 200 and render the page correctly when selectedAPis provided" in {
        val selectedApis = List(combinedApi4, combinedApi5, combinedApi6)

        primeAuthServiceSuccess()
        primeFetchAllCombinedApisSuccess(combinedApis ++ selectedApis)

        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/email-preferences/select-api?${selectedApis.map("selectedAPIs=" + _.serviceName).mkString("&")}", validHeaders)
        result.status shouldBe OK

        validateSelectAPIPageWithPreviouslySelectedAPIs(Jsoup.parse(result.body), combinedApis, selectedApis, "/api-gatekeeper/emails/email-preferences/by-specific-api")
      }

      "respond with 403 when not authorised" in {
        primeAuthServiceFail()
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/email-preferences/select-api", validHeaders)
        result.status shouldBe FORBIDDEN
      }
    }

    "GET /emails/email-preferences/by-specific-api" should {
      val selectedApis = Seq(combinedApi4, combinedApi5, combinedApi6)

      "respond with 200 and render the page correctly on initial load with selectedApis" in {
        primeAuthServiceSuccess()

        primeFetchAllCombinedApisSuccess(combinedApis ++ selectedApis)
        val result =
          callGetEndpoint(s"$url/api-gatekeeper/emails/email-preferences/by-specific-api?${selectedApis.map("selectedAPIs=" + _.serviceName).mkString("&")}", validHeaders)
        result.status shouldBe OK

        validateEmailPreferencesSpecificApiPageNew(Jsoup.parse(result.body), selectedApis)
      }

      "redirect to select api new page when no selectedApis in query params" in {
        primeAuthServiceSuccess()
        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/email-preferences/by-specific-api", validHeaders)
        validateRedirect(result, "/api-gatekeeper/emails/email-preferences/select-api")
      }

      "respond with 200 and render the page with selectedApis" in {
        primeAuthServiceSuccess()

        primeFetchAllCombinedApisSuccess(combinedApis ++ selectedApis)
        primeDeveloperServiceEmailPreferencesBySelectedAPisTopicAndCategory(allUsers, apis, TopicOptionChoice.BUSINESS_AND_POLICY)

        val result =
          callGetEndpoint(
            s"$url/api-gatekeeper/emails/email-preferences/by-specific-api?selectedTopic=${TopicOptionChoice.BUSINESS_AND_POLICY.toString}${apis.map("&selectedAPIs=" + _.serviceName).mkString}",
            validHeaders
          )
        validateEmailPreferencesSpecificApiPageNew(Jsoup.parse(result.body), combinedApis)
      }

      "respond with 403 when specific api page is not authorised" in {
        primeAuthServiceFail()
        val result =
          callGetEndpoint(s"$url/api-gatekeeper/emails/email-preferences/by-specific-api?${selectedApis.map("&selectedAPIs=" + _.serviceName).mkString}", validHeaders)
        result.status shouldBe FORBIDDEN
      }
    }

    "GET /emails/email-preferences/selected-user-topic" should {
      val selectedApis = Seq(combinedApi4, combinedApi5, combinedApi6)
      val offset       = 0
      val limit        = 15

      "respond with 200 and render the page correctly on initial load when authorised" in {
        primeAuthServiceSuccess()
        primeFetchAllCombinedApisSuccess(combinedApis ++ selectedApis)
        primeDeveloperServiceEmailPreferencesBySelectedTopicPaginated(allUsers, TopicOptionChoice.TECHNICAL, offset, limit)

        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/email-preferences/selected-user-topic?selectedTopic=TECHNICAL&offset=$offset&limit=$limit", validHeaders)

        result.status shouldBe OK
      }
    }

    "GET /emails/email-preferences/selected-subscribed-api" should {
      val selectedApis = Seq(combinedApi4, combinedApi5, combinedApi6)
      val offset       = 0
      val limit        = 15

      "respond with 200 and render the page correctly on initial load when authorised" in {
        primeAuthServiceSuccess()
        primeFetchAllCombinedApisSuccess(combinedApis ++ selectedApis)
        primeDeveloperServiceEmailPreferencesBySelectedSubscribedApisPaginated(allUsers, selectedApis.map(_.serviceName), offset, limit)

        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/email-preferences/selected-subscribed-api?selectedAPIs=api-4&offset=$offset&limit=$limit", validHeaders)

        result.status shouldBe OK
      }
    }

    "GET /emails/email-preferences/selected-user-tax-regime" should {
      val categories   = List(APICategoryDetails("category1", "name1"), APICategoryDetails("category2", "name2"), APICategoryDetails("category3", "name3"))
      val selectedApis = Seq(combinedApi4, combinedApi5, combinedApi6)
      val offset       = 0
      val limit        = 15

      "respond with 200 and render the page correctly on initial load when authorised" in {
        primeAuthServiceSuccess()
        primeFetchAllCombinedApisSuccess(combinedApis ++ selectedApis)
        primeGetAllCategories(categories)
        primeDeveloperServiceEmailPreferencesBySelectedUserTaxRegimePaginated(allUsers, categories.map(_.category), offset, limit)

        val result = callGetEndpoint(s"$url/api-gatekeeper/emails/email-preferences/selected-user-tax-regime?selectedCategories=category1&offset=$offset&limit=$limit", validHeaders)

        result.status shouldBe OK
      }
    }

    def validateRedirect(response: WSResponse, expectedLocation: String): Unit = {
      response.status shouldBe SEE_OTHER
      val mayBeLocationHeader: Option[Seq[String]] = response.headers.get(LOCATION).map(_.toSeq)
      mayBeLocationHeader.fold(fail("redirect Location header missing")) { locationHeader =>
        locationHeader.head shouldBe expectedLocation
      }
    }
  }
}
