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

import java.util.UUID

import akka.actor.ActorSystem
import config.AppConfig
import model.Environment._
import model._
import org.joda.time.DateTime
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.BeforeAndAfterEach
import play.api.test.Helpers._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ApplicationConnectorSpec extends UnitSpec with MockitoSugar with ArgumentMatchersSugar with ScalaFutures with BeforeAndAfterEach {
  private val baseUrl = "https://example.com"
  private val environmentName = "ENVIRONMENT"
  private val bearer = "TestBearerToken"
  private val apiKeyTest = UUID.randomUUID().toString
  val apiVersion1 = ApiVersion.random

  class Setup(proxyEnabled: Boolean = false) {
    val authToken = "Bearer Token"
    implicit val hc = HeaderCarrier().withExtraHeaders(("Authorization", authToken))

    val mockHttpClient = mock[HttpClient]
    val mockProxiedHttpClient = mock[ProxiedHttpClient]
    val mockEnvironment = mock[Environment]
    val mockAppConfig: AppConfig = mock[AppConfig]

    when(mockEnvironment.toString).thenReturn(environmentName)
    when(mockProxiedHttpClient.withHeaders(*, *)).thenReturn(mockProxiedHttpClient)

    val connector = new ApplicationConnector {
      val httpClient = mockHttpClient
      val proxiedHttpClient = mockProxiedHttpClient
      val serviceBaseUrl = baseUrl
      val useProxy = proxyEnabled
      val bearerToken = bearer
      val environment = mockEnvironment
      val appConfig = mockAppConfig
      val apiKey = apiKeyTest
    }
  }

  "updateRateLimitTier" should {

    val applicationId = ApplicationId.random
    val url = s"$baseUrl/application/${applicationId.value}/rate-limit-tier"

    "send Authorisation and return OK if the rate limit tier update was successful on the backend" in new Setup {
      val body = UpdateRateLimitTierRequest(RateLimitTier.GOLD)

      when(mockHttpClient.POST[UpdateRateLimitTierRequest, HttpResponse](eqTo(url), eqTo(body), *)(*, *, *, *))
        .thenReturn(Future.successful(HttpResponse(NO_CONTENT)))

      val result = await(connector.updateRateLimitTier(applicationId, RateLimitTier.GOLD))

      result shouldBe ApplicationUpdateSuccessResult
    }

    "send Authorisation and propagates 5xx errors" in new Setup {
      val body = UpdateRateLimitTierRequest(RateLimitTier.SILVER)

      when(mockHttpClient.POST[UpdateRateLimitTierRequest, HttpResponse](eqTo(url), eqTo(body), *)(*, *, *, *))
        .thenReturn(Future.failed(Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      intercept[Upstream5xxResponse] {
        await(connector.updateRateLimitTier(applicationId, RateLimitTier.SILVER))
      }
    }
  }

  "fetchAllSubscribedApplications" should {
    val url = s"$baseUrl/application/subscriptions"

    "retrieve all applications" in new Setup {
      val apiContext = ApiContext.random
      val response = Seq(
        SubscriptionResponse(
          ApiIdentifier(apiContext, ApiVersion.random),
          Seq("a97541e8-f93d-4d0a-ab0b-862e63204b7d", "4bf49df9-523a-4aa3-a446-683ff24b619f", "42695949-c7e8-4de9-a443-15c0da43143a")))

      when(mockHttpClient.GET[Seq[SubscriptionResponse]](eqTo(url))(*, *, *))
        .thenReturn(Future.successful(response))

      val result: Seq[SubscriptionResponse] = await(connector.fetchAllSubscriptions())

      result.head.apiIdentifier.context shouldBe apiContext
    }
  }

  "approveUplift" should {
    val applicationId = ApplicationId.random
    val url = s"$baseUrl/application/${applicationId.value}/approve-uplift"
    val gatekeeperId = "loggedin.gatekeeper"
    val body = ApproveUpliftRequest("loggedin.gatekeeper")

    "send Authorisation and return OK if the uplift was successful on the backend" in new Setup {
      when(mockHttpClient.POST[ApproveUpliftRequest, HttpResponse](eqTo(url), eqTo(body), *)(*, *, *, *))
        .thenReturn(Future.successful(HttpResponse(NO_CONTENT)))

      val result = await(connector.approveUplift(applicationId, gatekeeperId))

      result shouldBe ApproveUpliftSuccessful
    }

    "handle 412 precondition failed" in new Setup {
      when(mockHttpClient.POST[ApproveUpliftRequest, HttpResponse](eqTo(url), eqTo(body), *)(*, *, *, *))
        .thenReturn(Future.failed(Upstream4xxResponse("Application is not in state 'PENDING_GATEKEEPER_APPROVAL'", PRECONDITION_FAILED, PRECONDITION_FAILED)))

      intercept[PreconditionFailed] {
        await(connector.approveUplift(applicationId, gatekeeperId))
      }
    }
  }

  "rejectUplift" should {
    val applicationId = ApplicationId.random
    val url = s"$baseUrl/application/${applicationId.value}/reject-uplift"
    val gatekeeperId = "loggedin.gatekeeper"
    val rejectionReason = "A similar name is already taken by another application"
    val body = RejectUpliftRequest(gatekeeperId, rejectionReason)

    "send Authorisation and return Ok if the uplift rejection was successful on the backend" in new Setup {
      when(mockHttpClient.POST[RejectUpliftRequest, HttpResponse](eqTo(url), eqTo(body), *)(*, *, *, *))
        .thenReturn(Future.successful(HttpResponse(NO_CONTENT)))

      val result = await(connector.rejectUplift(applicationId, gatekeeperId, rejectionReason))

      result shouldBe RejectUpliftSuccessful
    }

    "hande 412 preconditions failed" in new Setup {
      when(mockHttpClient.POST[RejectUpliftRequest, HttpResponse](eqTo(url), eqTo(body), *)(*, *, *, *))
        .thenReturn(Future.failed(Upstream4xxResponse("Application is not in state 'PENDING_GATEKEEPER_APPROVAL'", PRECONDITION_FAILED, PRECONDITION_FAILED)))

      intercept[PreconditionFailed] {
        await(connector.rejectUplift(applicationId, gatekeeperId, rejectionReason))
      }
    }
  }

  "resend verification email" should {
    val applicationId = ApplicationId.random
    val url = s"$baseUrl/application/${applicationId.value}/resend-verification"
    val gatekeeperId = "loggedin.gatekeeper"
    val body = ResendVerificationRequest(gatekeeperId)

    "send Verification request and return OK if the resend was successful on the backend" in new Setup {
      when(mockHttpClient.POST[ResendVerificationRequest, HttpResponse](eqTo(url), eqTo(body), *)(*, *, *, *))
        .thenReturn(Future.successful(HttpResponse(NO_CONTENT)))

      val result = await(connector.resendVerification(applicationId, gatekeeperId))

      result shouldBe ResendVerificationSuccessful
    }

    "handle 412 precondition failed" in new Setup {
      when(mockHttpClient.POST[ResendVerificationRequest, HttpResponse](eqTo(url), eqTo(body), *)(*, *, *, *))
        .thenReturn(
          Future.failed(Upstream4xxResponse("Application is not in state 'PENDING_REQUESTOR_VERIFICATION'", PRECONDITION_FAILED, PRECONDITION_FAILED)))

      intercept[PreconditionFailed] {
        await(connector.resendVerification(applicationId, gatekeeperId))
      }
    }
  }

  "fetchAllApplicationsBySubscription" should {
    val url = s"$baseUrl/application?subscribesTo=some-context&version=some-version"

    "retrieve all applications subscribed to a specific API" in new Setup {
      when(mockHttpClient.GET[Seq[ApplicationResponse]](eqTo(url))(*, *, *))
        .thenReturn(Future.successful(Seq.empty))

      val result = await(connector.fetchAllApplicationsBySubscription("some-context", "some-version"))

      result shouldBe Seq.empty
    }

    "propagate fetchAllApplicationsBySubscription exception" in new Setup {

      private val thrownException: Upstream5xxResponse = Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)

      when(mockHttpClient.GET[Seq[ApplicationResponse]](eqTo(url))(*, *, *))
        .thenReturn(Future.failed(thrownException))

      val exception: FetchApplicationsFailed = intercept[FetchApplicationsFailed] {
        await(connector.fetchAllApplicationsBySubscription("some-context", "some-version"))
      }

      exception.getCause shouldBe thrownException
    }
  }

  "fetchAllApplications" should {
    val url = s"$baseUrl/application"
    val collaborators = Set(
      Collaborator("sample@example.com", CollaboratorRole.ADMINISTRATOR),
      Collaborator("someone@example.com", CollaboratorRole.DEVELOPER))
    val applications = Seq(ApplicationResponse(
      ApplicationId.random, ClientId("clientid1"), "gatewayId1", "application1", "PRODUCTION", None, collaborators, DateTime.now(), DateTime.now(), Standard(), ApplicationState()))

    "retrieve all applications" in new Setup {
      when(mockHttpClient.GET[Seq[ApplicationResponse]](eqTo(url))(*, *, *))
        .thenReturn(Future.successful(applications))

      val result = await(connector.fetchAllApplications())

      result shouldBe applications
    }

    "propagate fetchAllApplications exception" in new Setup {
      when(mockHttpClient.GET[Seq[ApplicationResponse]](eqTo(url))(*, *, *))
        .thenReturn(Future.failed(Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      intercept[FetchApplicationsFailed] {
        await(connector.fetchAllApplications())
      }
    }
  }

  "fetchApplication" should {
    val applicationId = ApplicationId.random
    val url = s"$baseUrl/gatekeeper/application/${applicationId.value}"
    val collaborators = Set(
      Collaborator("sample@example.com", CollaboratorRole.ADMINISTRATOR),
      Collaborator("someone@example.com", CollaboratorRole.DEVELOPER))
    val applicationState = StateHistory(ApplicationId.random, State(2), Actor(UUID.randomUUID().toString))
    val application = ApplicationResponse(
      applicationId, ClientId("clientid1"), "gatewayId1", "application1", "PRODUCTION", None, collaborators, DateTime.now(), DateTime.now(), Standard(), ApplicationState())
    val response = ApplicationWithHistory(application, Seq(applicationState))


    "retrieve an application" in new Setup {
      when(mockHttpClient.GET[ApplicationWithHistory](*)(*, *, *))
        .thenReturn(Future.successful(response))

      val result = await(connector.fetchApplication(applicationId))

      verify(mockHttpClient).GET(eqTo(url))(*, *, *)

      result shouldBe response
    }

    "propagate fetchApplication exception" in new Setup {
      when(mockHttpClient.GET[Seq[ApplicationWithHistory]](eqTo(url))(*, *, *))
        .thenReturn(Future.failed(Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      intercept[Upstream5xxResponse] {
        await(connector.fetchApplication(applicationId))
      }
    }
  }

  "fetchStateHistory" should {
    val applicationId = ApplicationId.random
    val url = s"$baseUrl/gatekeeper/application/${applicationId.value}/stateHistory"
    val applicationState = StateHistory(ApplicationId.random, State(2), Actor(UUID.randomUUID().toString))
    val response = Seq(applicationState)

    "retrieve state history for app id" in new Setup {
      when(mockHttpClient.GET[Seq[StateHistory]](*)(*, *, *))
        .thenReturn(Future.successful(response))

      val result = await(connector.fetchStateHistory(applicationId))

      verify(mockHttpClient).GET(eqTo(url))(*, *, *)

      result shouldBe response
    }
  }

  "updateOverrides" should {
    val applicationId = ApplicationId.random
    val url = s"$baseUrl/application/${applicationId.value}/access/overrides"
    val overridesRequest = UpdateOverridesRequest(Set(PersistLogin(), SuppressIvForAgents(Set("hello", "read:individual-benefits"))))

    "send Authorisation and return OK if the request was successful on the backend" in new Setup {
      when(mockHttpClient.PUT[UpdateOverridesRequest, HttpResponse](eqTo(url), eqTo(overridesRequest), *)(*, *, *, *))
        .thenReturn(Future.successful(HttpResponse(OK)))

      val result = await(connector.updateOverrides(applicationId, overridesRequest))

      result shouldBe UpdateOverridesSuccessResult
    }

    "fail if the request failed on the backend" in new Setup {
      when(mockHttpClient.PUT[UpdateOverridesRequest, HttpResponse](eqTo(url), eqTo(overridesRequest), *)(*, *, *, *))
        .thenReturn(Future.failed(Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      intercept[Upstream5xxResponse] {
        await(connector.updateOverrides(applicationId, overridesRequest))
      }
    }
  }

  "updateScopes" should {
    val applicationId = ApplicationId.random
    val url = s"$baseUrl/application/${applicationId.value}/access/scopes"
    val scopesRequest = UpdateScopesRequest(Set("hello", "read:individual-benefits"))

    "send Authorisation and return OK if the request was successful on the backend" in new Setup {
      when(mockHttpClient.PUT[UpdateScopesRequest, HttpResponse](eqTo(url), eqTo(scopesRequest), *)(*, *, *, *))
        .thenReturn(Future.successful(HttpResponse(OK)))

      val result = await(connector.updateScopes(applicationId, scopesRequest))

      result shouldBe UpdateScopesSuccessResult
    }

    "fail if the request failed on the backend" in new Setup {
      when(mockHttpClient.PUT[UpdateScopesRequest, HttpResponse](eqTo(url), eqTo(scopesRequest), *)(*, *, *, *))
        .thenReturn(Future.failed(Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      intercept[Upstream5xxResponse] {
        await(connector.updateScopes(applicationId, scopesRequest))
      }
    }
  }

  "updateIpAllowlist" should {
    val applicationId = ApplicationId.random
    val url = s"$baseUrl/application/${applicationId.value}/ipAllowlist"
    val newIpAllowlist = IpAllowlist(required = false, Set("192.168.1.0/24", "192.168.2.0/24"))

    "make a PUT request and return a successful result if the request was successful on the backend" in new Setup {
      when(mockHttpClient.PUT[UpdateIpAllowlistRequest, HttpResponse](eqTo(url), eqTo(UpdateIpAllowlistRequest(newIpAllowlist.required, newIpAllowlist.allowlist)), *)(*, *, *, *))
        .thenReturn(Future.successful(HttpResponse(OK)))

      val result = await(connector.updateIpAllowlist(applicationId, newIpAllowlist.required, newIpAllowlist.allowlist))

      result shouldBe UpdateIpAllowlistSuccessResult
      verify(mockHttpClient).PUT(eqTo(url), eqTo(UpdateIpAllowlistRequest(newIpAllowlist.required, newIpAllowlist.allowlist)), *)(*, *, *, *)
    }

    "fail if the request failed on the backend" in new Setup {
      when(mockHttpClient.PUT[UpdateIpAllowlistRequest, HttpResponse](eqTo(url), eqTo(UpdateIpAllowlistRequest(newIpAllowlist.required, newIpAllowlist.allowlist)), *)(*, *, *, *))
        .thenReturn(Future.failed(Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      intercept[Upstream5xxResponse] {
        await(connector.updateIpAllowlist(applicationId, newIpAllowlist.required, newIpAllowlist.allowlist))
      }
    }
  }

  "unsubscribeFromApi" should {
    val apiContext = ApiContext.random
    val applicationId = ApplicationId.random
    val url = s"$baseUrl/application/${applicationId.value}/subscription?context=${apiContext.value}&version=${apiVersion1.value}"

    "send Authorisation and return OK if the request was successful on the backend" in new Setup {
      when(mockHttpClient.DELETE[HttpResponse](eqTo(url), *)(*, *, *))
        .thenReturn(Future.successful(HttpResponse(CREATED)))

      val result = await(connector.unsubscribeFromApi(applicationId, apiContext, apiVersion1))

      result shouldBe ApplicationUpdateSuccessResult
    }

    "fail if the request failed on the backend" in new Setup {
      when(mockHttpClient.DELETE[HttpResponse](eqTo(url), *)(*, *, *))
        .thenReturn(Future.failed(Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      intercept[Upstream5xxResponse] {
        await(connector.unsubscribeFromApi(applicationId, apiContext, apiVersion1))
      }
    }
  }

  "createPrivOrROPCApp" should {
    val url = s"$baseUrl/application"

    "successfully create an application" in new Setup {

      val applicationId = ApplicationId.random
      val appName = "My new app"
      val appDescription = "An application description"
      val admin = Seq(Collaborator("admin@example.com", CollaboratorRole.ADMINISTRATOR))
      val access = AppAccess(AccessType.PRIVILEGED, Seq())
      val totpSecrets = Some(TotpSecrets("secret"))
      val appAccess = AppAccess(AccessType.PRIVILEGED, Seq())

      val createPrivOrROPCAppRequest = CreatePrivOrROPCAppRequest("PRODUCTION", appName, appDescription, admin, access)
      val createPrivOrROPCAppResponse = CreatePrivOrROPCAppSuccessResult(applicationId, appName, "PRODUCTION", ClientId("client ID"), totpSecrets, appAccess)

      when(mockHttpClient
        .POST[CreatePrivOrROPCAppRequest, CreatePrivOrROPCAppSuccessResult](eqTo(url), eqTo(createPrivOrROPCAppRequest), *)(*, *, *, *))
        .thenReturn(Future.successful(createPrivOrROPCAppResponse))

      val result = await(connector.createPrivOrROPCApp(createPrivOrROPCAppRequest))

      result shouldBe createPrivOrROPCAppResponse
    }
  }

  "removeCollaborator" should {

    val applicationId = ApplicationId.random
    val emailAddress = "toRemove@example.com"
    val gatekeeperUserId = "maxpower"
    val adminsToEmail = Seq("admin1@example.com", "admin2@example.com")

    "send a DELETE request to the service with the correct params" in new Setup {
      when(mockHttpClient.DELETE[HttpResponse](*, *)(*, *, *))
        .thenReturn(Future.successful(HttpResponse(OK)))

      await(connector.removeCollaborator(applicationId, emailAddress, gatekeeperUserId, adminsToEmail))

      verify(mockHttpClient).DELETE[HttpResponse](*, *)(*, *, *)
    }

    "return ApplicationUpdateSuccessResult when the call is successful" in new Setup {
      when(mockHttpClient.DELETE[HttpResponse](*, *)(*, *, *))
        .thenReturn(Future.successful(HttpResponse(OK)))

      val result = await(connector.removeCollaborator(applicationId, emailAddress, gatekeeperUserId, adminsToEmail))

      result shouldBe ApplicationUpdateSuccessResult
    }

    "throw TeamMemberLastAdmin when the service responds with 403" in new Setup {
      when(mockHttpClient.DELETE[HttpResponse](*, *)(*, *, *))
        .thenReturn(Future.failed(Upstream4xxResponse("Forbidden", FORBIDDEN, FORBIDDEN)))

      intercept[TeamMemberLastAdmin] {
        await(connector.removeCollaborator(applicationId, emailAddress, gatekeeperUserId, adminsToEmail))
      }
    }

    "throw the error when the service returns any other error" in new Setup {
      when(mockHttpClient.DELETE[HttpResponse](*, *)(*, *, *))
        .thenReturn(Future.failed(Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      intercept[Upstream5xxResponse] {
        await(connector.removeCollaborator(applicationId, emailAddress, gatekeeperUserId, adminsToEmail))
      }
    }
  }

  "searchApplications" should {
    val url = s"$baseUrl/applications"
    val params = Map("page" -> "1", "pageSize" -> "10")
    val expectedResponse = PaginatedApplicationResponse(Seq.empty, 0, 0, 0, 0)

    "send a GET request to the service with the correct params" in new Setup {
      when(mockHttpClient.GET[PaginatedApplicationResponse](*, *)(*, *, *))
        .thenReturn(Future.successful(expectedResponse))

      await(connector.searchApplications(params))

      verify(mockHttpClient).GET[PaginatedApplicationResponse](eqTo(url), eqTo(params.toSeq))(*, *, *)
    }

    "return the paginated application response when the call is successful" in new Setup {
      when(mockHttpClient.GET[PaginatedApplicationResponse](*, *)(*, *, *))
        .thenReturn(Future.successful(PaginatedApplicationResponse(Seq.empty, 0, 0, 0, 0)))

      val result = await(connector.searchApplications(params))

      result shouldBe expectedResponse
    }

    "throw the error when the service returns an error" in new Setup {
      when(mockHttpClient.GET[PaginatedApplicationResponse](*, *)(*, *, *))
        .thenReturn(Future.failed(Upstream5xxResponse("Internal Server Error", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      intercept[Upstream5xxResponse] {
        await(connector.searchApplications(params))
      }
    }
  }

  "search collaborators" should {
    val url = s"$baseUrl/collaborators"
    val apiContext = ApiContext.random

    "return emails" in new Setup {
      val expectedQueryParams = Seq("context" -> apiContext.value, "version" -> apiVersion1.value)
      private val email = "user@example.com"
      when(mockHttpClient.GET[Seq[String]](eqTo(url), eqTo(expectedQueryParams))(*, *, *))
        .thenReturn(Future.successful(Seq(email)))

      val result: Seq[String] = await(connector.searchCollaborators(apiContext, apiVersion1, None))

      verify(mockHttpClient).GET[Seq[String]](eqTo(url), eqTo(expectedQueryParams))(*, *, *)

      result shouldBe Seq(email)
    }

    "return emails with emailFilter" in new Setup {
      private val email = "user@example.com"

      val expectedQueryParams = Seq(
        "context" -> apiContext.value, "version" -> apiVersion1.value,
        "partialEmailMatch" -> email)

      when(mockHttpClient.GET[Seq[String]](eqTo(url), eqTo(expectedQueryParams))(*, *, *))
        .thenReturn(Future.successful(Seq(email)))

      val result: Seq[String] = await(connector.searchCollaborators(apiContext, apiVersion1, Some(email)))

      verify(mockHttpClient).GET[Seq[String]](eqTo(url), eqTo(expectedQueryParams))(*, *, *)

      result shouldBe Seq(email)
    }
  }

  "http" when {
    "configured not to use the proxy" should {
      "use the HttpClient" in new Setup(proxyEnabled = false) {
        connector.http shouldBe mockHttpClient
      }
    }

    "configured to use the proxy" should {
      "use the ProxiedHttpClient with the correct authorisation" in new Setup(proxyEnabled = true) {
        connector.http shouldBe mockProxiedHttpClient

        verify(mockProxiedHttpClient).withHeaders(bearer, apiKeyTest)
      }
    }
  }
}
