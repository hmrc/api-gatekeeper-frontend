/*
 * Copyright 2021 HM Revenue & Customs
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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import config.AppConfig
import model._
import play.api.test.Helpers._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.AsyncHmrcSpec

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.Json
import org.joda.time.DateTime
import java.util.UUID
import utils.UrlEncoding
import utils.WireMockSugar
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.UpstreamErrorResponse
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class ApplicationConnectorSpec 
    extends AsyncHmrcSpec 
    with WireMockSugar
    with GuiceOneAppPerSuite
    with UrlEncoding {

  val apiVersion1 = ApiVersion.random
  val applicationId = ApplicationId.random
  class Setup(proxyEnabled: Boolean = false) {
    val authToken = "Bearer Token"
    implicit val hc = HeaderCarrier().withExtraHeaders(("Authorization", authToken))

    val httpClient = fakeApplication.injector.instanceOf[HttpClient]
    val mockAppConfig: AppConfig = mock[AppConfig]
    when(mockAppConfig.applicationProductionBaseUrl).thenReturn(wireMockUrl)

    val connector = new ProductionApplicationConnector(mockAppConfig, httpClient) {
    }
  }

  // To solve issue with DateTime serialisation without a timezone id.
  private def compareByString[A](a1:A, a2:A) = a1.toString shouldBe a2.toString

  "updateRateLimitTier" should {
    val url = s"/application/${applicationId.value}/rate-limit-tier"

    "send Authorisation and return OK if the rate limit tier update was successful on the backend" in new Setup {
      val body = Json.toJson(UpdateRateLimitTierRequest(RateLimitTier.GOLD)).toString

      stubFor(
        post(urlEqualTo(url))
        .withRequestBody(equalTo(body))
        .willReturn(
          aResponse()
          .withStatus(NO_CONTENT)
        )
      )
 
      await(connector.updateRateLimitTier(applicationId, RateLimitTier.GOLD)) shouldBe ApplicationUpdateSuccessResult
    }

    "send Authorisation and propagates 5xx errors" in new Setup {
      val body = Json.toJson(UpdateRateLimitTierRequest(RateLimitTier.SILVER)).toString

      stubFor(
        post(urlEqualTo(url))
        .withRequestBody(equalTo(body))
        .willReturn(
          aResponse()
          .withStatus(INTERNAL_SERVER_ERROR)
        )
      )

      intercept[UpstreamErrorResponse] {
        await(connector.updateRateLimitTier(applicationId, RateLimitTier.SILVER))
      }
    }
  }

  "approveUplift" should {
    val gatekeeperId = "loggedin.gatekeeper"
    val body = Json.toJson(ApproveUpliftRequest("loggedin.gatekeeper")).toString
    val url = s"/application/${applicationId.value}/approve-uplift"

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

      intercept[PreconditionFailed] {
        await(connector.approveUplift(applicationId, gatekeeperId))
      }
    }
  }

  "rejectUplift" should {
    val gatekeeperId = "loggedin.gatekeeper"
    val rejectionReason = "A similar name is already taken by another application"
    val body = Json.toJson(RejectUpliftRequest(gatekeeperId, rejectionReason)).toString
    val url = s"/application/${applicationId.value}/reject-uplift"

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

      intercept[PreconditionFailed] {
        await(connector.rejectUplift(applicationId, gatekeeperId, rejectionReason))
      }
    }
  }

  "resend verification email" should {
    val gatekeeperId = "loggedin.gatekeeper"
    val body = Json.toJson(ResendVerificationRequest(gatekeeperId)).toString
    val url = s"/application/${applicationId.value}/resend-verification"

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
      intercept[PreconditionFailed] {
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
    val url = "/application"
    val collaborators = Set(
      Collaborator("sample@example.com", CollaboratorRole.ADMINISTRATOR, UserId.random),
      Collaborator("someone@example.com", CollaboratorRole.DEVELOPER, UserId.random))

    "retrieve all applications" in new Setup {
      val applications = List( ApplicationResponse(applicationId, ClientId("clientid1"), "gatewayId1", "application1", "PRODUCTION", None, collaborators, DateTime.now(), DateTime.now(), Standard(), ApplicationState()) )
      val payload = Json.toJson(applications).toString

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

  "fetchApplication" should {
    val url = s"/gatekeeper/application/${applicationId.value}"
    val collaborators = Set(
      Collaborator("sample@example.com", CollaboratorRole.ADMINISTRATOR, UserId.random),
      Collaborator("someone@example.com", CollaboratorRole.DEVELOPER, UserId.random)
      )
    val applicationState = StateHistory(ApplicationId.random, State(2), Actor(UUID.randomUUID().toString))
    val application = ApplicationResponse(
      applicationId, ClientId("clientid1"), "gatewayId1", "application1", "PRODUCTION", None, collaborators, DateTime.now(), DateTime.now(), Standard(), ApplicationState()
      )
    val appWithHistory = ApplicationWithHistory(application, List(applicationState))
    val response = Json.toJson(appWithHistory).toString

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
      val url = s"/gatekeeper/application/${applicationId.value}/stateHistory"
      val applicationState = StateHistory(ApplicationId.random, State(2), Actor(UUID.randomUUID().toString))
      val response = Json.toJson(List(applicationState)).toString

      stubFor(
        get(urlEqualTo(url))
        .willReturn(
          aResponse()
          .withStatus(OK)
          .withBody(response)
        )
      )
      compareByString(await(connector.fetchStateHistory(applicationId)), List(applicationState))
    }
  }

  "updateOverrides" should {
    val overridesRequest = UpdateOverridesRequest(Set(PersistLogin(), SuppressIvForAgents(Set("hello", "read:individual-benefits"))))
    val url = s"/application/${applicationId.value}/access/overrides"

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
    val request = Json.toJson(scopesRequest).toString
    val url = s"/application/${applicationId.value}/access/scopes"

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

  "updateIpAllowlist" should {
    val url = s"/application/${applicationId.value}/ipAllowlist"
    val newIpAllowlist = IpAllowlist(required = false, Set("192.168.1.0/24", "192.168.2.0/24"))
    val request = Json.toJson(UpdateIpAllowlistRequest(newIpAllowlist.required, newIpAllowlist.allowlist)).toString
    
    "make a PUT request and return a successful result if the request was successful on the backend" in new Setup {
      stubFor(
        put(urlEqualTo(url))
        .withRequestBody(equalTo(request))
        .willReturn(
          aResponse()
          .withStatus(OK)
        )
      )
      await(connector.updateIpAllowlist(applicationId, newIpAllowlist.required, newIpAllowlist.allowlist)) shouldBe UpdateIpAllowlistSuccessResult
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
        await(connector.updateIpAllowlist(applicationId, newIpAllowlist.required, newIpAllowlist.allowlist))
      }.statusCode shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "unsubscribeFromApi" should {
    val apiContext = ApiContext.random
    val url = s"/application/${applicationId.value}/subscription?context=${apiContext.value}&version=${apiVersion1.value}"

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

      val appName = "My new app"
      val appDescription = "An application description"
      val admin = List(Collaborator("admin@example.com", CollaboratorRole.ADMINISTRATOR, UserId.random))
      val access = AppAccess(AccessType.PRIVILEGED, List())
      val totpSecrets = Some(TotpSecrets("secret"))
      val appAccess = AppAccess(AccessType.PRIVILEGED, List())

      val createPrivOrROPCAppRequest = CreatePrivOrROPCAppRequest("PRODUCTION", appName, appDescription, admin, access)
      val request = Json.toJson(createPrivOrROPCAppRequest).toString
      val createPrivOrROPCAppResponse = CreatePrivOrROPCAppSuccessResult(applicationId, appName, "PRODUCTION", ClientId("client ID"), totpSecrets, appAccess)
      val response = Json.toJson(createPrivOrROPCAppResponse).toString
      
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

  "removeCollaborator" should {
    val emailAddress = "toRemove@example.com"
    val gatekeeperUserId = "maxpower"
    val adminsToEmail = List("admin1@example.com", "admin2@example.com")

    val url = s"/application/${applicationId.value}/collaborator/$emailAddress"

    "send a DELETE request to the service with the correct params" in new Setup {
      stubFor(
        delete(urlPathEqualTo(url))
        .withQueryParam("admin", equalTo(encode(gatekeeperUserId)))
        .withQueryParam("adminsToEmail", equalTo(encode(adminsToEmail.mkString(","))))
        .willReturn(
          aResponse()
          .withStatus(OK)
        )
      )
      await(connector.removeCollaborator(applicationId, emailAddress, gatekeeperUserId, adminsToEmail)) shouldBe ApplicationUpdateSuccessResult
    }

    "throw TeamMemberLastAdmin when the service responds with 403" in new Setup {
      stubFor(
        delete(urlPathEqualTo(url))
        .withQueryParam("admin", equalTo(encode(gatekeeperUserId)))
        .withQueryParam("adminsToEmail", equalTo(encode(adminsToEmail.mkString(","))))
        .willReturn(
          aResponse()
          .withStatus(FORBIDDEN)
        )
      )
      intercept[TeamMemberLastAdmin] {
        await(connector.removeCollaborator(applicationId, emailAddress, gatekeeperUserId, adminsToEmail))
      }
    }

    "throw the error when the service returns any other error" in new Setup {
      stubFor(
        delete(urlPathEqualTo(url))
        .withQueryParam("admin", equalTo(encode(gatekeeperUserId)))
        .withQueryParam("adminsToEmail", equalTo(encode(adminsToEmail.mkString(","))))
        .willReturn(
          aResponse()
          .withStatus(INTERNAL_SERVER_ERROR)
        )
      )

      intercept[UpstreamErrorResponse] {
        await(connector.removeCollaborator(applicationId, emailAddress, gatekeeperUserId, adminsToEmail))
      }.statusCode shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "searchApplications" should {
    val url = s"/applications"
    val params = Map("page" -> "1", "pageSize" -> "10")
    val expectedResponse = PaginatedApplicationResponse(List.empty, 0, 0, 0, 0)
    val response = Json.toJson(expectedResponse).toString

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
    val url = s"/collaborators"
    val apiContext = ApiContext.random

    "return emails" in new Setup {
      val email = "user@example.com"
      val response = Json.toJson(List(email)).toString
      
      stubFor(
        get(urlPathEqualTo(url))
        .withQueryParam("context", equalTo(encode(apiContext.value)))
        .withQueryParam("version", equalTo(encode(apiVersion1.value)))
        .willReturn(
          aResponse()
          .withStatus(OK)
          .withBody(response)
        )
      )
      await(connector.searchCollaborators(apiContext, apiVersion1, None)) shouldBe List(email)
    }

    "return emails with emailFilter" in new Setup {
      val email = "user@example.com"
      val response = Json.toJson(List(email)).toString

      stubFor(
        get(urlPathEqualTo(url))
        .withQueryParam("context", equalTo(encode(apiContext.value)))
        .withQueryParam("version", equalTo(encode(apiVersion1.value)))
        .withQueryParam("partialEmailMatch", equalTo(encode(email)))
        .willReturn(
          aResponse()
          .withStatus(OK)
          .withBody(response)
        )
      )

      await(connector.searchCollaborators(apiContext, apiVersion1, Some(email))) shouldBe List(email)
    }
  }
}
