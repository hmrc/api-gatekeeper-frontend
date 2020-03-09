/*
 * Copyright 2020 HM Revenue & Customs
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

import java.net.URLEncoder
import java.util.UUID

import akka.actor.ActorSystem
import config.AppConfig
import model.Environment._
import model._
import org.joda.time.DateTime
import org.mockito.Matchers.{any, eq => meq}
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Matchers}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.test.UnitSpec
import utils.FutureTimeoutSupportImpl

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ApplicationConnectorSpec extends UnitSpec with Matchers with MockitoSugar with ScalaFutures with BeforeAndAfterEach {
  private val baseUrl = "https://example.com"
  private val environmentName = "ENVIRONMENT"
  private val bearer = "TestBearerToken"
  private val futureTimeoutSupport = new FutureTimeoutSupportImpl
  private val actorSystemTest = ActorSystem("test-actor-system")
  private val apiKeyTest = UUID.randomUUID().toString

  class Setup(proxyEnabled: Boolean = false) {
    val authToken = "Bearer Token"
    implicit val hc = HeaderCarrier().withExtraHeaders(("Authorization", authToken))

    val mockHttpClient = mock[HttpClient]
    val mockProxiedHttpClient = mock[ProxiedHttpClient]
    val mockEnvironment = mock[Environment]
    val mockAppConfig: AppConfig = mock[AppConfig]

    when(mockEnvironment.toString).thenReturn(environmentName)
    when(mockProxiedHttpClient.withHeaders(any(), any())).thenReturn(mockProxiedHttpClient)

    val connector = new ApplicationConnector {
      val httpClient = mockHttpClient
      val proxiedHttpClient = mockProxiedHttpClient
      val serviceBaseUrl = baseUrl
      val useProxy = proxyEnabled
      val bearerToken = bearer
      val environment = mockEnvironment
      val appConfig = mockAppConfig
      val actorSystem = actorSystemTest
      val futureTimeout = futureTimeoutSupport
      val apiKey = apiKeyTest
    }
  }

  "updateRateLimitTier" should {

    val applicationId = "anApplicationId"
    val url = s"$baseUrl/application/$applicationId/rate-limit-tier"

    "send Authorisation and return OK if the rate limit tier update was successful on the backend" in new Setup {
      val body = UpdateRateLimitTierRequest(RateLimitTier.GOLD)

      when(mockHttpClient.POST[UpdateRateLimitTierRequest, HttpResponse](meq(url), meq(body), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(NO_CONTENT)))

      val result = await(connector.updateRateLimitTier(applicationId, RateLimitTier.GOLD))

      result shouldBe ApplicationUpdateSuccessResult
    }

    "send Authorisation and propagates 5xx errors" in new Setup {
      val body = UpdateRateLimitTierRequest(RateLimitTier.SILVER)

      when(mockHttpClient.POST[UpdateRateLimitTierRequest, HttpResponse](meq(url), meq(body), any())(any(), any(), any(), any()))
        .thenReturn(Future.failed(Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      intercept[Upstream5xxResponse] {
        await(connector.updateRateLimitTier(applicationId, RateLimitTier.SILVER))
      }
    }
  }

  "fetchAllSubscribedApplications" should {
    val url = s"$baseUrl/application/subscriptions"

    "retrieve all applications" in new Setup {
      val response = Seq(
        SubscriptionResponse(
          APIIdentifier("individual-benefits", "1.0"),
          Seq("a97541e8-f93d-4d0a-ab0b-862e63204b7d", "4bf49df9-523a-4aa3-a446-683ff24b619f", "42695949-c7e8-4de9-a443-15c0da43143a")))

      when(mockHttpClient.GET[Seq[SubscriptionResponse]](meq(url))(any(), any(), any()))
        .thenReturn(Future.successful(response))

      val result: Seq[SubscriptionResponse] = await(connector.fetchAllSubscriptions())

      result.head.apiIdentifier.context shouldBe "individual-benefits"
    }
  }

  "approveUplift" should {
    val applicationId = "anApplicationId"
    val url = s"$baseUrl/application/$applicationId/approve-uplift"
    val gatekeeperId = "loggedin.gatekeeper"
    val body = ApproveUpliftRequest("loggedin.gatekeeper")

    "send Authorisation and return OK if the uplift was successful on the backend" in new Setup {
      when(mockHttpClient.POST[ApproveUpliftRequest, HttpResponse](meq(url), meq(body), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(NO_CONTENT)))

      val result = await(connector.approveUplift(applicationId, gatekeeperId))

      result shouldBe ApproveUpliftSuccessful
    }

    "handle 412 precondition failed" in new Setup {
      when(mockHttpClient.POST[ApproveUpliftRequest, HttpResponse](meq(url), meq(body), any())(any(), any(), any(), any()))
        .thenReturn(Future.failed(Upstream4xxResponse("Application is not in state 'PENDING_GATEKEEPER_APPROVAL'", PRECONDITION_FAILED, PRECONDITION_FAILED)))

      intercept[PreconditionFailed] {
        await(connector.approveUplift(applicationId, gatekeeperId))
      }
    }
  }

  "rejectUplift" should {
    val applicationId = "anApplicationId"
    val url = s"$baseUrl/application/$applicationId/reject-uplift"
    val gatekeeperId = "loggedin.gatekeeper"
    val rejectionReason = "A similar name is already taken by another application"
    val body = RejectUpliftRequest(gatekeeperId, rejectionReason)

    "send Authorisation and return Ok if the uplift rejection was successful on the backend" in new Setup {
      when(mockHttpClient.POST[RejectUpliftRequest, HttpResponse](meq(url), meq(body), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(NO_CONTENT)))

      val result = await(connector.rejectUplift(applicationId, gatekeeperId, rejectionReason))

      result shouldBe RejectUpliftSuccessful
    }

    "hande 412 preconditions failed" in new Setup {
      when(mockHttpClient.POST[RejectUpliftRequest, HttpResponse](meq(url), meq(body), any())(any(), any(), any(), any()))
        .thenReturn(Future.failed(Upstream4xxResponse("Application is not in state 'PENDING_GATEKEEPER_APPROVAL'", PRECONDITION_FAILED, PRECONDITION_FAILED)))

      intercept[PreconditionFailed] {
        await(connector.rejectUplift(applicationId, gatekeeperId, rejectionReason))
      }
    }
  }

  "resend verification email" should {
    val applicationId = "anApplicationId"
    val url = s"$baseUrl/application/$applicationId/resend-verification"
    val gatekeeperId = "loggedin.gatekeeper"
    val body = ResendVerificationRequest(gatekeeperId)

    "send Verification request and return OK if the resend was successful on the backend" in new Setup {
      when(mockHttpClient.POST[ResendVerificationRequest, HttpResponse](meq(url), meq(body), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(NO_CONTENT)))

      val result = await(connector.resendVerification(applicationId, gatekeeperId))

      result shouldBe ResendVerificationSuccessful
    }

    "handle 412 precondition failed" in new Setup {
      when(mockHttpClient.POST[ResendVerificationRequest, HttpResponse](meq(url), meq(body), any())(any(), any(), any(), any()))
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
      when(mockHttpClient.GET[Seq[ApplicationResponse]](meq(url))(any(), any(), any()))
        .thenReturn(Future.successful(Seq.empty))

      val result = await(connector.fetchAllApplicationsBySubscription("some-context", "some-version"))

      result shouldBe Seq.empty
    }

    "propagate fetchAllApplicationsBySubscription exception" in new Setup {

      private val thrownException: Upstream5xxResponse = Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)

      when(mockHttpClient.GET[Seq[ApplicationResponse]](meq(url))(any(), any(), any()))
        .thenReturn(Future.failed(thrownException))

      val exception: FetchApplicationsFailed = intercept[FetchApplicationsFailed] {
        await(connector.fetchAllApplicationsBySubscription("some-context", "some-version"))
      }

      exception.getCause shouldBe thrownException
    }

    "when retry logic is enabled should retry on failure" in new Setup {
      when(mockAppConfig.retryCount).thenReturn(1)
      when(mockHttpClient.GET[Seq[ApplicationResponse]](meq(url))(any(), any(), any())).thenReturn(
        Future.failed(new BadRequestException("")),
        Future.successful(Seq.empty)
      )

      await(connector.fetchAllApplicationsBySubscription("some-context", "some-version")) shouldBe Seq.empty
    }
  }

  "fetchAllApplications" should {
    val url = s"$baseUrl/application"
    val collaborators = Set(
      Collaborator("sample@example.com", CollaboratorRole.ADMINISTRATOR),
      Collaborator("someone@example.com", CollaboratorRole.DEVELOPER))
    val applications = Seq(ApplicationResponse(
      UUID.randomUUID(), "clientid1", "gatewayId1", "application1", "PRODUCTION", None, collaborators, DateTime.now(), DateTime.now(), Standard(), ApplicationState()))

    "retrieve all applications" in new Setup {
      when(mockHttpClient.GET[Seq[ApplicationResponse]](meq(url))(any(), any(), any()))
        .thenReturn(Future.successful(applications))

      val result = await(connector.fetchAllApplications())

      result shouldBe applications
    }

    "propagate fetchAllApplications exception" in new Setup {
      when(mockHttpClient.GET[Seq[ApplicationResponse]](meq(url))(any(), any(), any()))
        .thenReturn(Future.failed(Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      intercept[FetchApplicationsFailed] {
        await(connector.fetchAllApplications())
      }
    }
    "when retry logic is enabled should retry on failure" in new Setup {
      when(mockAppConfig.retryCount).thenReturn(1)
      when(mockHttpClient.GET[Seq[ApplicationResponse]](meq(url))(any(), any(), any())).thenReturn(
        Future.failed(new BadRequestException("")),
        Future.successful(applications)
      )

      await(connector.fetchAllApplications()) shouldBe applications
    }
  }

  "fetchApplication" should {
    val url = s"$baseUrl/gatekeeper/application/anApplicationId"
    val collaborators = Set(
      Collaborator("sample@example.com", CollaboratorRole.ADMINISTRATOR),
      Collaborator("someone@example.com", CollaboratorRole.DEVELOPER))
    val applicationId = "anApplicationId"
    val applicationState = StateHistory(UUID.randomUUID(), State(2), Actor(UUID.randomUUID().toString))
    val application = ApplicationResponse(
      UUID.randomUUID(), "clientid1", "gatewayId1", "application1", "PRODUCTION", None, collaborators, DateTime.now(), DateTime.now(), Standard(), ApplicationState())
    val response = ApplicationWithHistory(application, Seq(applicationState))


    "retrieve an application" in new Setup {
      when(mockHttpClient.GET[ApplicationWithHistory](any())(any(), any(), any()))
        .thenReturn(Future.successful(response))

      val result = await(connector.fetchApplication(applicationId))

      verify(mockHttpClient).GET(meq(url))(any(), any(), any())

      result shouldBe response
    }

    "propagate fetchApplication exception" in new Setup {
      when(mockHttpClient.GET[Seq[ApplicationWithHistory]](meq(url))(any(), any(), any()))
        .thenReturn(Future.failed(Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      intercept[Upstream5xxResponse] {
        await(connector.fetchApplication(applicationId))
      }
    }
    "when retry logic is enabled should retry on failure" in new Setup {
      when(mockAppConfig.retryCount).thenReturn(1)
      when(mockHttpClient.GET[ApplicationWithHistory](meq(url))(any(), any(), any())).thenReturn(
        Future.failed(new BadRequestException("")),
        Future.successful(response)
      )

      await(connector.fetchApplication(applicationId)) shouldBe response
    }
  }

  "updateOverrides" should {
    val applicationId = "anApplicationId"
    val url = s"$baseUrl/application/$applicationId/access/overrides"
    val overridesRequest = UpdateOverridesRequest(Set(PersistLogin(), SuppressIvForAgents(Set("hello", "read:individual-benefits"))))

    "send Authorisation and return OK if the request was successful on the backend" in new Setup {
      when(mockHttpClient.PUT[UpdateOverridesRequest, HttpResponse](meq(url), meq(overridesRequest))(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(OK)))

      val result = await(connector.updateOverrides(applicationId, overridesRequest))

      result shouldBe UpdateOverridesSuccessResult
    }

    "fail if the request failed on the backend" in new Setup {
      when(mockHttpClient.PUT[UpdateOverridesRequest, HttpResponse](meq(url), meq(overridesRequest))(any(), any(), any(), any()))
        .thenReturn(Future.failed(Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      intercept[Upstream5xxResponse] {
        await(connector.updateOverrides(applicationId, overridesRequest))
      }
    }
  }

  "updateScopes" should {
    val applicationId = "anApplicationId"
    val url = s"$baseUrl/application/$applicationId/access/scopes"
    val scopesRequest = UpdateScopesRequest(Set("hello", "read:individual-benefits"))

    "send Authorisation and return OK if the request was successful on the backend" in new Setup {
      when(mockHttpClient.PUT[UpdateScopesRequest, HttpResponse](meq(url), meq(scopesRequest))(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(OK)))

      val result = await(connector.updateScopes(applicationId, scopesRequest))

      result shouldBe UpdateScopesSuccessResult
    }

    "fail if the request failed on the backend" in new Setup {
      when(mockHttpClient.PUT[UpdateScopesRequest, HttpResponse](meq(url), meq(scopesRequest))(any(), any(), any(), any()))
        .thenReturn(Future.failed(Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      intercept[Upstream5xxResponse] {
        await(connector.updateScopes(applicationId, scopesRequest))
      }
    }
  }

  "updateIpWhitelist" should {
    val applicationId = "anApplicationId"
    val url = s"$baseUrl/application/$applicationId/ipWhitelist"
    val newIpWhitelist = Set("192.168.1.0/24", "192.168.2.0/24")

    "make a PUT request and return a successful result if the request was successful on the backend" in new Setup {
      when(mockHttpClient.PUT[UpdateIpWhitelistRequest, HttpResponse](meq(url), meq(UpdateIpWhitelistRequest(newIpWhitelist)))(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(OK)))

      val result = await(connector.manageIpWhitelist(applicationId, newIpWhitelist))

      result shouldBe UpdateIpWhitelistSuccessResult
      verify(mockHttpClient).PUT(meq(url), meq(UpdateIpWhitelistRequest(newIpWhitelist)))(any(), any(), any(), any())
    }

    "fail if the request failed on the backend" in new Setup {
      when(mockHttpClient.PUT[UpdateIpWhitelistRequest, HttpResponse](meq(url), meq(UpdateIpWhitelistRequest(newIpWhitelist)))(any(), any(), any(), any()))
        .thenReturn(Future.failed(Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      intercept[Upstream5xxResponse] {
        await(connector.manageIpWhitelist(applicationId, newIpWhitelist))
      }
    }
  }

  "subscribeToApi" should {
    val applicationId = "anApplicationId"
    val url = s"$baseUrl/application/$applicationId/subscription"
    val apiIdentifier = APIIdentifier("hello", "1.0")

    "send Authorisation and return OK if the request was successful on the backend" in new Setup {
      when(mockHttpClient.POST[APIIdentifier, HttpResponse](meq(url), meq(apiIdentifier), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(CREATED)))

      val result = await(connector.subscribeToApi(applicationId, apiIdentifier))

      result shouldBe ApplicationUpdateSuccessResult
    }

    "fail if the request failed on the backend" in new Setup {
      when(mockHttpClient.POST[APIIdentifier, HttpResponse](meq(url), meq(apiIdentifier), any())(any(), any(), any(), any()))
        .thenReturn(Future.failed(Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      intercept[Upstream5xxResponse] {
        await(connector.subscribeToApi(applicationId, APIIdentifier("hello", "1.0")))
      }
    }
  }

  "unsubscribeFromApi" should {
    val applicationId = "anApplicationId"
    val url = s"$baseUrl/application/$applicationId/subscription?context=hello&version=1.0"

    "send Authorisation and return OK if the request was successful on the backend" in new Setup {
      when(mockHttpClient.DELETE[HttpResponse](meq(url))(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(CREATED)))

      val result = await(connector.unsubscribeFromApi(applicationId, "hello", "1.0"))

      result shouldBe ApplicationUpdateSuccessResult
    }

    "fail if the request failed on the backend" in new Setup {
      when(mockHttpClient.DELETE[HttpResponse](meq(url))(any(), any(), any()))
        .thenReturn(Future.failed(Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      intercept[Upstream5xxResponse] {
        await(connector.unsubscribeFromApi(applicationId, "hello", "1.0"))
      }
    }
  }

  "createPrivOrROPCApp" should {
    val url = s"$baseUrl/application"

    "successfully create an application" in new Setup {

      val applicationId = "applicationId"
      val appName = "My new app"
      val appDescription = "An application description"
      val admin = Seq(Collaborator("admin@example.com", CollaboratorRole.ADMINISTRATOR))
      val access = AppAccess(AccessType.PRIVILEGED, Seq())
      val totpSecrets = Some(TotpSecrets("secret"))
      val appAccess = AppAccess(AccessType.PRIVILEGED, Seq())

      val createPrivOrROPCAppRequest = CreatePrivOrROPCAppRequest("PRODUCTION", appName, appDescription, admin, access)
      val createPrivOrROPCAppRequestJson = Json.toJson(createPrivOrROPCAppRequest).toString()
      val createPrivOrROPCAppResponse = CreatePrivOrROPCAppSuccessResult(applicationId, appName, "PRODUCTION", "client ID", totpSecrets, appAccess)

      when(mockHttpClient
        .POST[CreatePrivOrROPCAppRequest, CreatePrivOrROPCAppSuccessResult](meq(url), meq(createPrivOrROPCAppRequest), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(createPrivOrROPCAppResponse))

      val result = await(connector.createPrivOrROPCApp(createPrivOrROPCAppRequest))

      result shouldBe createPrivOrROPCAppResponse
    }
  }

  "addCollaborator" should {
    val appId = "APP_ID"
    val url = s"$baseUrl/application/$appId/collaborator"
    val teamMember = Collaborator("newUser@example.com", role = CollaboratorRole.DEVELOPER)
    val addTeamMemberRequest = AddTeamMemberRequest("admin@example.com", teamMember, isRegistered = true, Set.empty)

    "post the team member to the service" in new Setup {
      when(mockHttpClient.POST[AddTeamMemberRequest, HttpResponse](any[String], any[AddTeamMemberRequest], any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(OK)))

      await(connector.addCollaborator(appId, addTeamMemberRequest))

      verify(mockHttpClient).POST[AddTeamMemberRequest, HttpResponse](meq(url), meq(addTeamMemberRequest), any())(any(), any(), any(), any())
    }

    "return ApplicationUpdateSuccessResult when the call is successful" in new Setup {
      when(mockHttpClient.POST[AddTeamMemberRequest, HttpResponse](meq(url), meq(addTeamMemberRequest), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(OK)))

      val result = await(connector.addCollaborator(appId, addTeamMemberRequest))

      result shouldBe ApplicationUpdateSuccessResult
    }

    "throw TeamMemberAlreadyExists when the service returns 409 Conflict" in new Setup {
      when(mockHttpClient.POST[AddTeamMemberRequest, HttpResponse](meq(url), meq(addTeamMemberRequest), any())(any(), any(), any(), any()))
        .thenReturn(Future.failed(Upstream4xxResponse("Conflict", CONFLICT, CONFLICT)))

      intercept[TeamMemberAlreadyExists] {
        await(connector.addCollaborator(appId, addTeamMemberRequest))
      }
    }

    "throw ApplicationNotFound when the service returns 404 Not Found" in new Setup {
      when(mockHttpClient.POST[AddTeamMemberRequest, HttpResponse](meq(url), meq(addTeamMemberRequest), any())(any(), any(), any(), any()))
        .thenReturn(Future.failed(new NotFoundException("Not Found")))

      intercept[ApplicationNotFound] {
        await(connector.addCollaborator(appId, addTeamMemberRequest))
      }
    }

    "throw the error when the service returns any other error" in new Setup {
      when(mockHttpClient.POST[AddTeamMemberRequest, HttpResponse](meq(url), meq(addTeamMemberRequest), any())(any(), any(), any(), any()))
        .thenReturn(Future.failed( Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      intercept[Upstream5xxResponse] {
        await(connector.addCollaborator(appId, addTeamMemberRequest))
      }
    }
  }

  "removeCollaborator" should {
    def encode(str: String): String = URLEncoder.encode(str, "UTF-8")

    val appId = "APP_ID"
    val emailAddress = "toRemove@example.com"
    val gatekeeperUserId = "maxpower"
    val adminsToEmail = Seq("admin1@example.com", "admin2@example.com")
    val url =
      s"$baseUrl/application/$appId/collaborator/${encode(emailAddress)}?admin=${encode(gatekeeperUserId)}&adminsToEmail=${encode(adminsToEmail.mkString(","))}"

    "send a DELETE request to the service with the correct params" in new Setup {
      when(mockHttpClient.DELETE[HttpResponse](any[String])(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(OK)))

      await(connector.removeCollaborator(appId, emailAddress, gatekeeperUserId, adminsToEmail))

      verify(mockHttpClient).DELETE[HttpResponse](meq(url))(any(), any(), any())
    }

    "return ApplicationUpdateSuccessResult when the call is successful" in new Setup {
      when(mockHttpClient.DELETE[HttpResponse](meq(url))(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(OK)))

      val result = await(connector.removeCollaborator(appId, emailAddress, gatekeeperUserId, adminsToEmail))

      result shouldBe ApplicationUpdateSuccessResult
    }

    "throw TeamMemberLastAdmin when the service responds with 403" in new Setup {
      when(mockHttpClient.DELETE[HttpResponse](meq(url))(any(), any(), any()))
        .thenReturn(Future.failed(Upstream4xxResponse("Forbidden", FORBIDDEN, FORBIDDEN)))

      intercept[TeamMemberLastAdmin] {
        await(connector.removeCollaborator(appId, emailAddress, gatekeeperUserId, adminsToEmail))
      }
    }

    "throw the error when the service returns any other error" in new Setup {
      when(mockHttpClient.DELETE[HttpResponse](meq(url))(any(), any(), any()))
        .thenReturn(Future.failed(Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      intercept[Upstream5xxResponse] {
        await(connector.removeCollaborator(appId, emailAddress, gatekeeperUserId, adminsToEmail))
      }
    }
  }

  "searchApplications" should {
    val url = s"$baseUrl/applications"
    val params = Map("page" -> "1", "pageSize" -> "10")
    val expectedResponse = PaginatedApplicationResponse(Seq.empty, 0, 0, 0, 0)

    "send a GET request to the service with the correct params" in new Setup {
      when(mockHttpClient.GET[PaginatedApplicationResponse](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(expectedResponse))

      await(connector.searchApplications(params))

      verify(mockHttpClient).GET[PaginatedApplicationResponse](meq(url), meq(params.toSeq))(any(), any(), any())
    }

    "return the paginated application response when the call is successful" in new Setup {
      when(mockHttpClient.GET[PaginatedApplicationResponse](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(PaginatedApplicationResponse(Seq.empty, 0, 0, 0, 0)))

      val result = await(connector.searchApplications(params))

      result shouldBe expectedResponse
    }

    "throw the error when the service returns an error" in new Setup {
      when(mockHttpClient.GET[PaginatedApplicationResponse](any(), any())(any(), any(), any()))
        .thenReturn(Future.failed(Upstream5xxResponse("Internal Server Error", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      intercept[Upstream5xxResponse] {
        await(connector.searchApplications(params))
      }
    }

    "when retry logic is enabled should retry on failure" in new Setup {
      when(mockAppConfig.retryCount).thenReturn(1)
      when(mockHttpClient.GET[PaginatedApplicationResponse](any(), any())(any(), any(), any())).thenReturn(
        Future.failed(new BadRequestException("")),
        Future.successful(expectedResponse)
      )
      val result = await(connector.searchApplications(params))
      result shouldBe expectedResponse
    }
  }

  "search collaborators" should {
    val url = s"$baseUrl/collaborators"
    "return emails" in new Setup {

      val expectedQueryParams = Seq("context" -> "api-context", "version" -> "1.0")
      private val email = "user@example.com"
      when(mockHttpClient.GET[Seq[String]](meq(url), meq(expectedQueryParams))(any(), any(), any()))
        .thenReturn(Future.successful(Seq(email)))

      val result: Seq[String] = await(connector.searchCollaborators("api-context", "1.0", None))

      verify(mockHttpClient).GET[Seq[String]](meq(url), meq(expectedQueryParams))(any(), any(), any())

      result shouldBe Seq(email)
    }

    "return emails with emailFilter" in new Setup {
      private val email = "user@example.com"

      val expectedQueryParams = Seq(
        "context" -> "api-context", "version" -> "1.0",
        "partialEmailMatch" -> email)

      when(mockHttpClient.GET[Seq[String]](meq(url), meq(expectedQueryParams))(any(), any(), any()))
        .thenReturn(Future.successful(Seq(email)))

      val result: Seq[String] = await(connector.searchCollaborators("api-context", "1.0", Some(email)))

      verify(mockHttpClient).GET[Seq[String]](meq(url), meq(expectedQueryParams))(any(), any(), any())

      result shouldBe Seq(email)
    }

    "when retry logic is enabled should retry on failure" in new Setup {
      val expectedQueryParams = Seq("context" -> "api-context", "version" -> "1.0")
      private val email = "user@example.com"
      when(mockAppConfig.retryCount).thenReturn(1)
      when(mockHttpClient.GET[Seq[String]](meq(url), meq(expectedQueryParams))(any(), any(), any())).thenReturn(
        Future.failed(new BadRequestException("")),
        Future.successful(Seq(email))
      )
      val result: Seq[String] = await(connector.searchCollaborators("api-context", "1.0", None))
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
