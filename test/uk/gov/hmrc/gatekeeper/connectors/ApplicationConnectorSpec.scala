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

import java.time.temporal.ChronoUnit
import java.time.{LocalDateTime, Period}
import scala.concurrent.ExecutionContext.Implicits.global

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, UpstreamErrorResponse}

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.State
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.Collaborators.Administrator
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.{Collaborator, Collaborators, RateLimitTier}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{Actors, UserId, _}
import uk.gov.hmrc.apiplatform.modules.common.utils._
import uk.gov.hmrc.gatekeeper.config.AppConfig
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.utils.UrlEncoding

class ApplicationConnectorSpec
    extends AsyncHmrcSpec
    with WireMockSugar
    with GuiceOneAppPerSuite
    with UrlEncoding {

  def anApplicationResponse(createdOn: LocalDateTime = LocalDateTime.now(), lastAccess: LocalDateTime = LocalDateTime.now()): ApplicationResponse = {
    ApplicationResponse(
      ApplicationId.random,
      ClientId("clientid"),
      "gatewayId",
      "appName",
      "deployedTo",
      None,
      Set.empty,
      createdOn,
      Some(lastAccess),
      Privileged(),
      ApplicationState(),
      Period.ofDays(547),
      RateLimitTier.BRONZE,
      Some("termsUrl"),
      Some("privacyPolicyUrl"),
      None
    )
  }

  val apiVersion1   = ApiVersionNbr.random
  val applicationId = ApplicationId.random
  val administrator = Administrator(UserId.random, "sample@example.com".toLaxEmail)
  val developer     = Collaborators.Developer(UserId.random, "someone@example.com".toLaxEmail)

  val authToken   = "Bearer Token"
  implicit val hc = HeaderCarrier().withExtraHeaders(("Authorization", authToken))

  class Setup(proxyEnabled: Boolean = false) {
    val httpClient               = app.injector.instanceOf[HttpClient]
    val mockAppConfig: AppConfig = mock[AppConfig]
    when(mockAppConfig.applicationProductionBaseUrl).thenReturn(wireMockUrl)

    val connector = new ProductionApplicationConnector(mockAppConfig, httpClient) {}
  }

  // To solve issue with LocalDateTime serialisation without a timezone id.
  private def compareByString[A](a1: A, a2: A) = a1.toString shouldBe a2.toString

  "approveUplift" should {
    val gatekeeperId = "loggedin.gatekeeper"
    val body         = Json.toJson(ApproveUpliftRequest("loggedin.gatekeeper")).toString
    val url          = s"/application/${applicationId.value.toString()}/approve-uplift"

    "send Authorisation and return OK if the uplift was successful on the backend" in new Setup {
      stubFor(
        post(urlEqualTo(url))
          .withRequestBody(equalTo(body))
          .willReturn(
            aResponse()
              .withStatus(NO_CONTENT)
          )
      )

      await(connector.approveUplift(applicationId, gatekeeperId)) shouldBe ApproveUpliftSuccessful
    }

    "handle 412 precondition failed" in new Setup {
      stubFor(
        post(urlEqualTo(url))
          .withRequestBody(equalTo(body))
          .willReturn(
            aResponse()
              .withStatus(PRECONDITION_FAILED)
          )
      )

      intercept[PreconditionFailedException.type] {
        await(connector.approveUplift(applicationId, gatekeeperId))
      }
    }
  }

  "rejectUplift" should {
    val gatekeeperId    = "loggedin.gatekeeper"
    val rejectionReason = "A similar name is already taken by another application"
    val body            = Json.toJson(RejectUpliftRequest(gatekeeperId, rejectionReason)).toString
    val url             = s"/application/${applicationId.value.toString()}/reject-uplift"

    "send Authorisation and return Ok if the uplift rejection was successful on the backend" in new Setup {
      stubFor(
        post(urlEqualTo(url))
          .withRequestBody(equalTo(body))
          .willReturn(
            aResponse()
              .withStatus(NO_CONTENT)
          )
      )

      await(connector.rejectUplift(applicationId, gatekeeperId, rejectionReason)) shouldBe RejectUpliftSuccessful
    }

    "hande 412 preconditions failed" in new Setup {
      stubFor(
        post(urlEqualTo(url))
          .withRequestBody(equalTo(body))
          .willReturn(
            aResponse()
              .withStatus(PRECONDITION_FAILED)
          )
      )

      intercept[PreconditionFailedException.type] {
        await(connector.rejectUplift(applicationId, gatekeeperId, rejectionReason))
      }
    }
  }

  "resend verification email" should {
    val gatekeeperId = "loggedin.gatekeeper"
    val body         = Json.toJson(ResendVerificationRequest(gatekeeperId)).toString
    val url          = s"/application/${applicationId.value.toString()}/resend-verification"

    "send Verification request and return OK if the resend was successful on the backend" in new Setup {
      stubFor(
        post(urlEqualTo(url))
          .withRequestBody(equalTo(body))
          .willReturn(
            aResponse()
              .withStatus(NO_CONTENT)
          )
      )

      await(connector.resendVerification(applicationId, gatekeeperId)) shouldBe ResendVerificationSuccessful
    }

    "handle 412 precondition failed" in new Setup {
      stubFor(
        post(urlEqualTo(url))
          .withRequestBody(equalTo(body))
          .willReturn(
            aResponse()
              .withStatus(PRECONDITION_FAILED)
          )
      )
      intercept[PreconditionFailedException.type] {
        await(connector.resendVerification(applicationId, gatekeeperId))
      }
    }
  }

  "fetchAllApplicationsBySubscription" should {
    val url = s"/application?subscribesTo=some-context&version=some-version"

    "retrieve all applications subscribed to a specific API" in new Setup {
      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody("[]")
          )
      )
      await(connector.fetchAllApplicationsBySubscription("some-context", "some-version")) shouldBe List.empty
    }

    "propagate fetchAllApplicationsBySubscription exception" in new Setup {
      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      intercept[FetchApplicationsFailed] {
        await(connector.fetchAllApplicationsBySubscription("some-context", "some-version"))
      }
    }
  }

  "fetchAllApplications" should {
    val url                              = "/application"
    val collaborators: Set[Collaborator] = Set(
      administrator,
      developer
    )

    "retrieve all applications" in new Setup {
      val grantLength: Period = Period.ofDays(547)

      val applications = List(ApplicationResponse(
        applicationId,
        ClientId("clientid1"),
        "gatewayId1",
        "application1",
        "PRODUCTION",
        None,
        collaborators,
        LocalDateTime.now(),
        Some(LocalDateTime.now()),
        Standard(),
        ApplicationState(),
        grantLength
      ))
      val payload      = Json.toJson(applications).toString

      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(payload)
          )
      )
      val result = await(connector.fetchAllApplications())
      result.head.id shouldBe applications.toList.head.id
    }

    "propagate fetchAllApplications exception" in new Setup {
      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      intercept[FetchApplicationsFailed] {
        await(connector.fetchAllApplications())
      }
    }
  }

  "fetchAllApplicationsWithSubscriptions" should {
    val url = "/gatekeeper/applications/subscriptions"

    "retrieve all applications" in new Setup {
      val applications = List(ApplicationWithSubscriptionsResponse(
        ApplicationId.random,
        "My App",
        Some(LocalDateTime.parse("2002-02-03T12:01:02")),
        Set(
          ApiIdentifier(ApiContext("hello"), ApiVersionNbr("1.0")),
          ApiIdentifier(ApiContext("hello"), ApiVersionNbr("2.0")),
          ApiIdentifier(ApiContext("api-documentation-test-service"), ApiVersionNbr("1.5"))
        )
      ))
      val payload      = Json.toJson(applications).toString

      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(payload)
          )
      )
      val result = await(connector.fetchApplicationsWithSubscriptions())
      result.head.id shouldBe applications.head.id
    }
  }

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
      val result = await(connector.fetchAllApplicationsWithStateHistories())
      result shouldBe applicationsWithStateHistories
    }
  }

  "fetchApplication" should {
    val url                 = s"/gatekeeper/application/${applicationId.value.toString()}"
    val grantLength: Period = Period.ofDays(547)

    val collaborators: Set[Collaborator] = Set(
      administrator,
      developer
    )
    val stateHistory                     = StateHistory(ApplicationId.random, State(2), Actors.AppCollaborator(collaborators.head.emailAddress), None, LocalDateTime.now.truncatedTo(ChronoUnit.MILLIS))
    val applicationState                 = ApplicationState(State.TESTING, None, None, LocalDateTime.now)
    val application                      = ApplicationResponse(
      applicationId,
      ClientId("clientid1"),
      "gatewayId1",
      "application1",
      "PRODUCTION",
      None,
      collaborators,
      LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS),
      Some(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS)),
      Standard(),
      applicationState,
      grantLength
    )
    val appWithHistory                   = ApplicationWithHistory(application, List(stateHistory))
    val response                         = Json.toJson(appWithHistory).toString

    "retrieve an application" in new Setup {
      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(response)
          )
      )

      val result = await(connector.fetchApplication(applicationId))

      compareByString(result, appWithHistory)
    }

    "propagate fetchApplication exception" in new Setup {
      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      intercept[UpstreamErrorResponse] {
        await(connector.fetchApplication(applicationId))
      }.statusCode shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "fetchStateHistory" should {
    "retrieve state history for app id" in new Setup {
      val url          = s"/gatekeeper/application/${applicationId.value.toString()}/stateHistory"
      val stateHistory = StateHistory(ApplicationId.random, State(2), Actors.Unknown, None, LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))
      val response     = Json.toJson(List(stateHistory)).toString

      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(response)
          )
      )
      compareByString(await(connector.fetchStateHistory(applicationId)), List(stateHistory))
    }
  }

  "updateOverrides" should {
    val overridesRequest = UpdateOverridesRequest(Set(PersistLogin, SuppressIvForAgents(Set("hello", "read:individual-benefits"))))
    val url              = s"/application/${applicationId.value.toString()}/access/overrides"

    "send Authorisation and return OK if the request was successful on the backend" in new Setup {
      stubFor(
        put(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
          )
      )

      await(connector.updateOverrides(applicationId, overridesRequest)) shouldBe UpdateOverridesSuccessResult
    }

    "fail if the request failed on the backend" in new Setup {
      stubFor(
        put(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      intercept[UpstreamErrorResponse] {
        await(connector.updateOverrides(applicationId, overridesRequest))
      }.statusCode shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "updateScopes" should {
    val scopesRequest = UpdateScopesRequest(Set("hello", "read:individual-benefits"))
    val request       = Json.toJson(scopesRequest).toString
    val url           = s"/application/${applicationId.value.toString()}/access/scopes"

    "send Authorisation and return OK if the request was successful on the backend" in new Setup {
      stubFor(
        put(urlEqualTo(url))
          .withRequestBody(equalTo(request))
          .willReturn(
            aResponse()
              .withStatus(OK)
          )
      )

      await(connector.updateScopes(applicationId, scopesRequest)) shouldBe UpdateScopesSuccessResult
    }

    "fail if the request failed on the backend" in new Setup {
      stubFor(
        put(urlEqualTo(url))
          .withRequestBody(equalTo(request))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      intercept[UpstreamErrorResponse] {
        await(connector.updateScopes(applicationId, scopesRequest))
      }.statusCode shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "unsubscribeFromApi" should {
    val apiContext = ApiContext.random
    val url        = s"/application/${applicationId.value.toString()}/subscription?context=${apiContext.value}&version=${apiVersion1.value}"

    "send Authorisation and return OK if the request was successful on the backend" in new Setup {
      stubFor(
        delete(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
          )
      )
      await(connector.unsubscribeFromApi(applicationId, apiContext, apiVersion1)) shouldBe ApplicationUpdateSuccessResult
    }

    "fail if the request failed on the backend" in new Setup {
      stubFor(
        delete(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      intercept[UpstreamErrorResponse] {
        await(connector.unsubscribeFromApi(applicationId, apiContext, apiVersion1))
      }.statusCode shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "createPrivOrROPCApp" should {
    val url = s"/application"

    "successfully create an application" in new Setup {

      val appName        = "My new app"
      val appDescription = "An application description"
      val admin          = List(administrator)
      val access         = AppAccess(AccessType.PRIVILEGED, List())
      val totpSecrets    = Some(TotpSecrets("secret"))
      val appAccess      = AppAccess(AccessType.PRIVILEGED, List())

      val createPrivOrROPCAppRequest  = CreatePrivOrROPCAppRequest("PRODUCTION", appName, appDescription, admin, access)
      val request                     = Json.toJson(createPrivOrROPCAppRequest).toString
      val createPrivOrROPCAppResponse = CreatePrivOrROPCAppSuccessResult(applicationId, appName, "PRODUCTION", ClientId("client ID"), totpSecrets, appAccess)
      val response                    = Json.toJson(createPrivOrROPCAppResponse).toString

      stubFor(
        post(urlEqualTo(url))
          .withRequestBody(equalTo(request))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(response)
          )
      )

      await(connector.createPrivOrROPCApp(createPrivOrROPCAppRequest)) shouldBe createPrivOrROPCAppResponse
    }
  }

  "searchApplications" should {
    val url              = s"/applications"
    val params           = Map("page" -> "1", "pageSize" -> "10")
    val expectedResponse = PaginatedApplicationResponse(List.empty, 0, 0, 0, 0)
    val response         = Json.toJson(expectedResponse).toString

    "return the paginated application response when the call is successful" in new Setup {
      stubFor(
        get(urlPathEqualTo(url))
          .withQueryParam("page", equalTo("1"))
          .withQueryParam("pageSize", equalTo("10"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(response)
          )
      )

      await(connector.searchApplications(params)) shouldBe expectedResponse
    }

    "throw the error when the service returns an error" in new Setup {
      stubFor(
        get(urlPathEqualTo(url))
          .withQueryParam("page", equalTo("1"))
          .withQueryParam("pageSize", equalTo("10"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      intercept[UpstreamErrorResponse] {
        await(connector.searchApplications(params))
      }.statusCode shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "search collaborators" should {
    val url        = s"/collaborators"
    val apiContext = ApiContext.random

    "return emails" in new Setup {
      val email    = "user@example.com"
      val response = Json.toJson(List(email)).toString
      val request  = ApplicationConnector.SearchCollaboratorsRequest(apiContext, apiVersion1, None)

      stubFor(
        post(urlPathEqualTo(url))
          .withJsonRequestBody(request)
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(response)
          )
      )
      await(connector.searchCollaborators(apiContext, apiVersion1, None)) shouldBe List(email.toLaxEmail)
    }

    "return emails with emailFilter" in new Setup {
      val partialEmailMatch = "user@example"
      val email             = "user@example.com".toLaxEmail
      val response          = Json.toJson(List(email)).toString
      val request           = ApplicationConnector.SearchCollaboratorsRequest(apiContext, apiVersion1, Some(partialEmailMatch))

      stubFor(
        post(urlPathEqualTo(url))
          .withJsonRequestBody(request)
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(response)
          )
      )

      await(connector.searchCollaborators(apiContext, apiVersion1, Some(partialEmailMatch))) shouldBe List(email)
    }
  }

  "validateApplicationName" should {
    "return success result if name is valid" in new Setup {
      val name    = "my new name"
      val request = ValidateApplicationNameRequest(name, applicationId)

      stubFor(
        post(urlPathEqualTo("/application/name/validate"))
          .withJsonRequestBody(request)
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.obj().toString())
          )
      )

      await(connector.validateApplicationName(applicationId, name)) shouldBe ValidateApplicationNameSuccessResult
    }

    "return failure result if name is invalid" in new Setup {
      val name    = "my new name"
      val request = ValidateApplicationNameRequest(name, applicationId)

      stubFor(
        post(urlPathEqualTo("/application/name/validate"))
          .withJsonRequestBody(request)
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.obj("errors" -> Json.obj("invalidName" -> true, "duplicateName" -> false)).toString())
          )
      )

      await(connector.validateApplicationName(applicationId, name)) shouldBe ValidateApplicationNameFailureInvalidResult
    }

    "return failure result if name is duplicate" in new Setup {
      val name    = "my new name"
      val request = ValidateApplicationNameRequest(name, applicationId)

      stubFor(
        post(urlPathEqualTo("/application/name/validate"))
          .withJsonRequestBody(request)
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.obj("errors" -> Json.obj("invalidName" -> false, "duplicateName" -> true)).toString())
          )
      )

      await(connector.validateApplicationName(applicationId, name)) shouldBe ValidateApplicationNameFailureDuplicateResult
    }
  }
}
