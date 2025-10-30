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

package uk.gov.hmrc.gatekeeper.connectors

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.AccessType
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models.{CreateApplicationRequestV1, CreationAccess}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.utils._
import uk.gov.hmrc.gatekeeper.config.AppConfig
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.models.applications.ApplicationsByAnswer
import uk.gov.hmrc.gatekeeper.utils.UrlEncoding

class ApplicationConnectorSpec
    extends AsyncHmrcSpec
    with WireMockSugar
    with GuiceOneAppPerSuite
    with UrlEncoding
    with ApiIdentifierFixtures
    with ApplicationWithCollaboratorsFixtures
    with FixedClock {

  val apiVersion1   = ApiVersionNbr.random
  val applicationId = ApplicationId.random
  val administrator = Collaborators.Administrator(UserId.random, "sample@example.com".toLaxEmail)
  val developer     = Collaborators.Developer(UserId.random, "someone@example.com".toLaxEmail)
  val bearerToken   = "proxyBearerToken"
  val apiKey        = "apiKey"
  val authToken     = "Bearer Token"

  implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(("Authorization", authToken))

  class Setup(proxyEnabled: Boolean = false) {
    val httpClient               = app.injector.instanceOf[HttpClientV2]
    val mockAppConfig: AppConfig = mock[AppConfig]
    when(mockAppConfig.applicationProductionBaseUrl).thenReturn(wireMockUrl)
    when(mockAppConfig.applicationSandboxBaseUrl).thenReturn(wireMockUrl)
    when(mockAppConfig.applicationSandboxUseProxy).thenReturn(true)
    when(mockAppConfig.applicationSandboxBearerToken).thenReturn(bearerToken)
    when(mockAppConfig.applicationSandboxApiKey).thenReturn(apiKey)

    val productionConnector = new ProductionApplicationConnector(mockAppConfig, httpClient) {}

    val sandboxConnector = new SandboxApplicationConnector(mockAppConfig, httpClient) {}
  }

  // To solve issue with LocalDateTime serialisation without a timezone id.
  private def compareByString[A](a1: A, a2: A) = a1.toString shouldBe a2.toString

  // "fetchAllApplicationsBySubscription" should {
  //   val url = s"/application?subscribesTo=some-context&version=some-version"

  //   "retrieve all applications subscribed to a specific API" in new Setup {
  //     stubFor(
  //       get(urlEqualTo(url))
  //         .willReturn(
  //           aResponse()
  //             .withStatus(OK)
  //             .withBody("[]")
  //         )
  //     )
  //     await(productionConnector.fetchAllApplicationsBySubscription("some-context", "some-version")) shouldBe List.empty
  //   }

  //   "propagate fetchAllApplicationsBySubscription exception" in new Setup {
  //     stubFor(
  //       get(urlEqualTo(url))
  //         .willReturn(
  //           aResponse()
  //             .withStatus(INTERNAL_SERVER_ERROR)
  //         )
  //     )

  //     intercept[FetchApplicationsFailed] {
  //       await(productionConnector.fetchAllApplicationsBySubscription("some-context", "some-version"))
  //     }
  //   }
  // }

  // "fetchAllApplications" should {
  //   val url                              = "/application"
  //   val collaborators: Set[Collaborator] = Set(
  //     administrator,
  //     developer
  //   )

  //   "retrieve all applications" in new Setup {
  //     val applications = List(standardApp.withCollaborators(collaborators))
  //     val payload      = Json.toJson(applications).toString

  //     stubFor(
  //       get(urlEqualTo(url))
  //         .willReturn(
  //           aResponse()
  //             .withStatus(OK)
  //             .withBody(payload)
  //         )
  //     )
  //     val result = await(productionConnector.fetchAllApplications())
  //     result.head.id shouldBe applications.toList.head.id
  //   }

  //   "retrieve all applications from sandbox" in new Setup {
  //     val applications = List(standardApp.withCollaborators(collaborators))
  //     val payload      = Json.toJson(applications).toString

  //     stubFor(
  //       get(urlEqualTo(url))
  //         .withHeader(HeaderNames.AUTHORIZATION, equalTo(s"Bearer $bearerToken"))
  //         .withHeader(HeaderNames.ACCEPT, equalTo("application/hmrc.vnd.1.0+json"))
  //         .withHeader("x-api-key", equalTo(apiKey))
  //         .willReturn(
  //           aResponse()
  //             .withStatus(OK)
  //             .withBody(payload)
  //         )
  //     )
  //     val result = await(sandboxConnector.fetchAllApplications())
  //     result.head.id shouldBe applications.toList.head.id
  //   }

  //   "propagate fetchAllApplications exception" in new Setup {
  //     stubFor(
  //       get(urlEqualTo(url))
  //         .willReturn(
  //           aResponse()
  //             .withStatus(INTERNAL_SERVER_ERROR)
  //         )
  //     )

  //     intercept[FetchApplicationsFailed] {
  //       await(productionConnector.fetchAllApplications())
  //     }
  //   }
  // }

  // "fetchAllApplicationsWithSubscriptions" should {
  //   val url = "/gatekeeper/applications/subscriptions"

  //   "retrieve all applications" in new Setup {
  //     val application = AppWithSubscriptionsForCsvResponse(
  //       applicationIdOne,
  //       appNameOne,
  //       Some(Instant.parse("2002-02-03T12:01:02Z")),
  //       Set(
  //         apiIdentifierOne,
  //         apiIdentifierTwo,
  //         apiIdentifierThree
  //       )
  //     )

  //     val payload = s"""[{
  //                      |  "id": "${applicationIdOne}",
  //                      |  "name": "${appNameOne}",
  //                      |  "lastAccess": "2002-02-03T12:01:02Z",
  //                      |  "apiIdentifiers": [
  //                      |    {"context":"${apiIdentifierOne.context}","version":"${apiIdentifierOne.versionNbr}"},
  //                      |    {"context":"${apiIdentifierTwo.context}","version":"${apiIdentifierTwo.versionNbr}"},
  //                      |    {"context":"${apiIdentifierThree.context}","version":"${apiIdentifierThree.versionNbr}"}
  //                      |  ]
  //                      |}]""".stripMargin

  //     stubFor(
  //       get(urlEqualTo(url))
  //         .willReturn(
  //           aResponse()
  //             .withStatus(OK)
  //             .withBody(payload)
  //         )
  //     )
  //     val result = await(productionConnector.fetchApplicationsWithSubscriptions())
  //     result.head shouldBe application
  //   }
  // }

  "fetchAllApplicationsWithStateHistories" should {
    val url = "/gatekeeper/applications/stateHistory"

    "retrieve all applications with state histories" in new Setup {
      val applicationsWithStateHistories = List(
        ApplicationStateHistory(
          ApplicationId.random,
          "app 1 name",
          1,
          List(
            ApplicationStateHistoryItem(State.TESTING, LocalDateTime.now),
            ApplicationStateHistoryItem(State.PRODUCTION, LocalDateTime.now)
          )
        ),
        ApplicationStateHistory(ApplicationId.random, "app 2 name", 2, List(ApplicationStateHistoryItem(State.TESTING, LocalDateTime.now)))
      )
      val payload                        = Json.toJson(applicationsWithStateHistories).toString

      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(payload)
          )
      )
      val result = await(productionConnector.fetchAllApplicationsWithStateHistories())
      result shouldBe applicationsWithStateHistories
    }
  }

  // "fetchApplication" should {
  //   val url = s"/gatekeeper/application/${applicationId.value.toString()}"

  //   val collaborators: Set[Collaborator] = Set(
  //     administrator,
  //     developer
  //   )
  //   val stateHistory                     = StateHistory(
  //     ApplicationId.random,
  //     State.PENDING_GATEKEEPER_APPROVAL,
  //     Actors.AppCollaborator(collaborators.head.emailAddress),
  //     changedAt = instant
  //   )
  //   val applicationState                 = ApplicationState(State.TESTING, updatedOn = instant)
  //   val application                      = standardApp.withCollaborators(collaborators).withState(applicationState)
  //   val appWithHistory                   = ApplicationWithHistory(application, List(stateHistory))
  //   val response                         = Json.toJson(appWithHistory).toString

  //   "retrieve an application" in new Setup {
  //     stubFor(
  //       get(urlEqualTo(url))
  //         .willReturn(
  //           aResponse()
  //             .withStatus(OK)
  //             .withBody(response)
  //         )
  //     )

  //     val result = await(productionConnector.fetchApplication(applicationId))

  //     compareByString(result, appWithHistory)
  //   }

  //   "propagate fetchApplication exception" in new Setup {
  //     stubFor(
  //       get(urlEqualTo(url))
  //         .willReturn(
  //           aResponse()
  //             .withStatus(INTERNAL_SERVER_ERROR)
  //         )
  //     )

  //     intercept[UpstreamErrorResponse] {
  //       await(productionConnector.fetchApplication(applicationId))
  //     }.statusCode shouldBe INTERNAL_SERVER_ERROR
  //   }
  // }

  "fetchStateHistory" should {
    "retrieve state history for app id" in new Setup {
      val url          = s"/gatekeeper/application/${applicationId.value.toString()}/stateHistory"
      val stateHistory = StateHistory(ApplicationId.random, State.PENDING_GATEKEEPER_APPROVAL, Actors.Unknown, changedAt = instant)
      val response     = Json.toJson(List(stateHistory)).toString

      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(response)
          )
      )
      compareByString(await(productionConnector.fetchStateHistory(applicationId)), List(stateHistory))
    }
  }

  "createPrivApp" should {
    val url = s"/application"

    "successfully create an application" in new Setup {

      val appDescription = "My app description"
      val app            = privilegedCoreApp.copy(description = Some(appDescription))
      val admin          = List(administrator)
      val totpSecrets    = Some(TotpSecrets("secret"))
      val appAccess      = AppAccess(AccessType.PRIVILEGED, List())

      val createPrivAppRequest = CreateApplicationRequestV1(app.name, CreationAccess.Privileged, Some(appDescription), Environment.PRODUCTION, admin.toSet, None)

      val request               = Json.toJson(createPrivAppRequest).toString
      val createPrivAppResponse = CreatePrivAppSuccessResult(app.id, app.name, Environment.PRODUCTION, app.token.clientId, totpSecrets, appAccess)
      val response              =
        s"""{
           |  "details": ${Json.toJson(app).toString()},
           |  "totp": "secret"
           |}""".stripMargin

      stubFor(
        post(urlEqualTo(url))
          .withRequestBody(equalTo(request))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(response)
          )
      )

      await(productionConnector.createPrivApp(createPrivAppRequest)) shouldBe createPrivAppResponse
    }
  }

  // "searchApplications" should {
  //   val url              = s"/applications"
  //   val params           = Map("page" -> "1", "pageSize" -> "10")
  //   val expectedResponse = PaginatedApplications(List.empty, 0, 0, 0, 0)
  //   val response         = Json.toJson(expectedResponse).toString

  //   "return the paginated application response when the call is successful" in new Setup {
  //     stubFor(
  //       get(urlPathEqualTo(url))
  //         .withQueryParam("page", equalTo("1"))
  //         .withQueryParam("pageSize", equalTo("10"))
  //         .willReturn(
  //           aResponse()
  //             .withStatus(OK)
  //             .withBody(response)
  //         )
  //     )

  //     await(productionConnector.searchApplications(params)) shouldBe expectedResponse
  //   }

  //   "throw the error when the service returns an error" in new Setup {
  //     stubFor(
  //       get(urlPathEqualTo(url))
  //         .withQueryParam("page", equalTo("1"))
  //         .withQueryParam("pageSize", equalTo("10"))
  //         .willReturn(
  //           aResponse()
  //             .withStatus(INTERNAL_SERVER_ERROR)
  //         )
  //     )

  //     intercept[UpstreamErrorResponse] {
  //       await(productionConnector.searchApplications(params))
  //     }.statusCode shouldBe INTERNAL_SERVER_ERROR
  //   }
  // }

  "fetch applications by answer" should {
    "return apps" in new Setup {
      private val appsByAnswer = List(ApplicationsByAnswer("12345", List(applicationId)))
      private val questionType = "vat-registration-number"
      stubFor(
        get(urlPathEqualTo(s"/submissions/answers/$questionType"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.toJson(appsByAnswer).toString())
          )
      )

      await(productionConnector.fetchApplicationsByAnswer(questionType)) shouldBe appsByAnswer
    }
  }

  "search collaborators" should {
    val url        = s"/collaborators"
    val apiContext = ApiContext.random

    "return emails" in new Setup {
      val email    = "user@example.com"
      val response = Json.toJson(List(email)).toString
      val request  = ApplicationConnector.SearchCollaboratorsRequest(apiContext, apiVersion1)

      stubFor(
        post(urlPathEqualTo(url))
          .withJsonRequestBody(request)
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(response)
          )
      )
      await(productionConnector.searchCollaborators(apiContext, apiVersion1)) shouldBe List(email.toLaxEmail)
    }
  }
}
