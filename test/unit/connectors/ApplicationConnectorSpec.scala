/*
 * Copyright 2019 HM Revenue & Customs
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

package unit.connectors

import java.net.URLEncoder

import com.github.tomakehurst.wiremock.client.WireMock._
import config.AppConfig
import connectors.ApplicationConnector
import model._
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Matchers}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, Upstream5xxResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class ApplicationConnectorSpec extends UnitSpec with Matchers with MockitoSugar with ScalaFutures with WiremockSugar
  with BeforeAndAfterEach with WithFakeApplication {

  trait Setup {
    val authToken = "Bearer Token"
    implicit val hc = HeaderCarrier().withExtraHeaders(("Authorization", authToken))

    val mockAppConfig = mock[AppConfig]
    val httpClient = fakeApplication.injector.instanceOf[HttpClient]

    val connector = new ApplicationConnector(mockAppConfig, httpClient)

    when(mockAppConfig.applicationBaseUrl).thenReturn(wireMockUrl)
  }

  "updateRateLimitTier" should {

    val applicationId = "anApplicationId"

    "send Authorisation and return OK if the rate limit tier update was successful on the backend" in new Setup {
      stubFor(
        post(urlEqualTo(s"/application/$applicationId/rate-limit-tier"))
          .withRequestBody(equalTo(
            s"""{"rateLimitTier":"GOLD"}""".stripMargin))
          .willReturn(
            aResponse()
              .withStatus(NO_CONTENT)))

      val result = await(connector.updateRateLimitTier(applicationId, RateLimitTier.GOLD))

      verify(1, postRequestedFor(urlPathEqualTo(s"/application/$applicationId/rate-limit-tier"))
        .withHeader("Authorization", equalTo(authToken))
        .withRequestBody(equalTo( s"""{"rateLimitTier":"GOLD"}""")))

      result shouldBe ApplicationUpdateSuccessResult
    }

    "send Authorisation and propagates 5xx errors" in new Setup {
      stubFor(
        post(urlEqualTo(s"/application/$applicationId/rate-limit-tier"))
          .withRequestBody(equalTo(
            s"""{"rateLimitTier":"SILVER"}""".stripMargin))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
              .withBody( """{"code"="UNKNOWN_ERROR", "message":"An unexpected error occurred"}""")))

      intercept[Upstream5xxResponse] {
        await(connector.updateRateLimitTier(applicationId, RateLimitTier.SILVER))
      }

      verify(1, postRequestedFor(urlPathEqualTo(s"/application/$applicationId/rate-limit-tier"))
        .withHeader("Authorization", equalTo(authToken))
        .withRequestBody(equalTo( s"""{"rateLimitTier":"SILVER"}""")))
    }
  }

  "fetchAllSubscribedApplications" should {
    "retrieve all applications" in new Setup {
      val uri = "/application/subscriptions"
      val body = "[\n  {\n    \"apiIdentifier\": {\n      \"context\": \"individual-benefits\",\n      \"version\": \"1.0\"\n    },\n    \"applications\": [\n      \"a97541e8-f93d-4d0a-ab0b-862e63204b7d\",\n      \"4bf49df9-523a-4aa3-a446-683ff24b619f\",\n      \"42695949-c7e8-4de9-a443-15c0da43143a\"\n    ]\n  }]"
      stubFor(get(urlEqualTo(uri)).willReturn(aResponse().withStatus(OK).withBody(body)))
      val result: Seq[SubscriptionResponse] = await(connector.fetchAllSubscriptions())
      verify(1, getRequestedFor(urlPathEqualTo(uri)).withHeader("Authorization", equalTo(authToken)))
      result.head.apiIdentifier.context shouldBe "individual-benefits"
    }
  }

  "approveUplift" should {
    "send Authorisation and return OK if the uplift was successful on the backend" in new Setup {
      val applicationId = "anApplicationId"
      val gatekeeperId = "loggedin.gatekeeper"
      stubFor(post(urlEqualTo(s"/application/$applicationId/approve-uplift")).willReturn(aResponse().withStatus(NO_CONTENT)))
      val result = await(connector.approveUplift(applicationId, gatekeeperId))
      verify(1, postRequestedFor(urlPathEqualTo(s"/application/$applicationId/approve-uplift"))
        .withHeader("Authorization", equalTo(authToken))
        .withRequestBody(equalTo( s"""{"gatekeeperUserId":"$gatekeeperId"}""")))

      result shouldBe ApproveUpliftSuccessful
    }

    "handle 412 precondition failed" in new Setup {
      val applicationId = "anApplicationId"
      val gatekeeperId = "loggedin.gatekeeper"
      stubFor(post(urlEqualTo(s"/application/$applicationId/approve-uplift")).willReturn(aResponse().withStatus(PRECONDITION_FAILED)
        .withBody( """{"code"="INVALID_STATE_TRANSITION","message":"Application is not in state 'PENDING_GATEKEEPER_APPROVAL'"}""")))

      intercept[PreconditionFailed] {
        await(connector.approveUplift(applicationId, gatekeeperId))
      }

      verify(1, postRequestedFor(urlPathEqualTo(s"/application/$applicationId/approve-uplift"))
        .withHeader("Authorization", equalTo(authToken))
        .withRequestBody(equalTo( s"""{"gatekeeperUserId":"$gatekeeperId"}""")))
    }
  }

  "rejectUplift" should {
    "send Authorisation and return Ok if the uplift rejection was successful on the backend" in new Setup {
      val applicationId = "anApplicationId"
      val gatekeeperId = "loggedin.gatekeeper"
      val rejectionReason = "A similar name is already taken by another application"
      stubFor(post(urlEqualTo(s"/application/$applicationId/reject-uplift")).willReturn(aResponse().withStatus(NO_CONTENT)))

      val result = await(connector.rejectUplift(applicationId, gatekeeperId, rejectionReason))

      verify(1, postRequestedFor(urlPathEqualTo(s"/application/$applicationId/reject-uplift"))
        .withHeader("Authorization", equalTo(authToken))
        .withRequestBody(equalTo(
          s"""{"gatekeeperUserId":"$gatekeeperId","reason":"$rejectionReason"}""")))
      }

    "hande 412 preconditions failed" in new Setup {
      val applicationId = "anApplicationId"
      val gatekeeperId = "loggedin.gatekeeper"
      val rejectionReason = "A similar name is already taken by another application"
      stubFor(post(urlEqualTo(s"/application/$applicationId/reject-uplift")).willReturn(aResponse().withStatus(PRECONDITION_FAILED)
        .withBody( """{"code"="INVALID_STATE_TRANSITION","message":"Application is not in state 'PENDING_GATEKEEPER_APPROVAL'"}""")))

      intercept[PreconditionFailed] {
        await(connector.rejectUplift(applicationId, gatekeeperId, rejectionReason))
      }

      verify(1, postRequestedFor(urlPathEqualTo(s"/application/$applicationId/reject-uplift"))
        .withHeader("Authorization", equalTo(authToken))
        .withRequestBody(equalTo(
          s"""{"gatekeeperUserId":"$gatekeeperId","reason":"$rejectionReason"}""")))
    }
  }

  "resend verification email" should {
    "send Verification request and return OK if the resend was successful on the backend" in new Setup {
      val applicationId = "anApplicationId"
      val gatekeeperId = "loggedin.gatekeeper"
      stubFor(post(urlEqualTo(s"/application/$applicationId/resend-verification")).willReturn(aResponse().withStatus(NO_CONTENT)))
      val result = await(connector.resendVerification(applicationId, gatekeeperId))
      verify(1, postRequestedFor(urlPathEqualTo(s"/application/$applicationId/resend-verification"))
        .withHeader("Authorization", equalTo(authToken))
        .withRequestBody(equalTo( s"""{"gatekeeperUserId":"$gatekeeperId"}""")))

      result shouldBe ResendVerificationSuccessful
    }

    "handle 412 precondition failed" in new Setup {
      val applicationId = "anApplicationId"
      val gatekeeperId = "loggedin.gatekeeper"
      stubFor(post(urlEqualTo(s"/application/$applicationId/resend-verification")).willReturn(aResponse().withStatus(PRECONDITION_FAILED)
        .withBody( """{"code"="INVALID_STATE_TRANSITION","message":"Application is not in state 'PENDING_REQUESTOR_VERIFICATION'"}""")))

      intercept[PreconditionFailed] {
        await(connector.resendVerification(applicationId, gatekeeperId))
      }

      verify(1, postRequestedFor(urlPathEqualTo(s"/application/$applicationId/resend-verification"))
        .withHeader("Authorization", equalTo(authToken))
        .withRequestBody(equalTo( s"""{"gatekeeperUserId":"$gatekeeperId"}""")))
    }
  }

  "fetchAllApplicationsBySubscription" should {
    "retrieve all applications subscribed to a specific API" in new Setup {
      stubFor(get(urlEqualTo(s"/application?subscribesTo=some-context&version=some-version")).willReturn(aResponse().withStatus(OK)
        .withBody("[]")))

      val result = await(connector.fetchAllApplicationsBySubscription("some-context", "some-version"))

      verify(1, getRequestedFor(urlPathEqualTo("/application?subscribesTo=some-context&version=some-version"))
        .withHeader("Authorization", equalTo(authToken)))
    }

    "propagate fetchAllApplicationsBySubscription exception" in new Setup {
      stubFor(get(urlEqualTo(s"/application?subscribesTo=some-context&version=some-version")).willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR)))

      intercept[FetchApplicationsFailed] {
        await(connector.fetchAllApplicationsBySubscription("some-context", "some-version"))
      }

      verify(1, getRequestedFor(urlPathEqualTo(s"/application?subscribesTo="))
        .withHeader("Authorization", equalTo(authToken)))
    }
  }

  "fetchAllApplications" should {
    "retrieve all applications" in new Setup {
      stubFor(get(urlEqualTo(s"/application")).willReturn(aResponse().withStatus(OK)
        .withBody("[]")))

      val result = await(connector.fetchAllApplications())

      verify(1, getRequestedFor(urlPathEqualTo("/application"))
        .withHeader("Authorization", equalTo(authToken)))
    }

    "propagate fetchAllApplications exception" in new Setup {
      stubFor(get(urlEqualTo(s"/application")).willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR)))

      intercept[FetchApplicationsFailed] {
        await(connector.fetchAllApplications())
      }

      verify(1, getRequestedFor(urlPathEqualTo(s"/application"))
        .withHeader("Authorization", equalTo(authToken)))
    }
  }

  "updateOverrides" should {
    "send Authorisation and return OK if the request was successful on the backend" in new Setup {
      val applicationId = "anApplicationId"
      stubFor(put(urlEqualTo(s"/application/$applicationId/access/overrides")).willReturn(aResponse().withStatus(OK)))

      val result = await(connector.updateOverrides(applicationId,
        UpdateOverridesRequest(Set(PersistLogin(), SuppressIvForAgents(Set("hello", "read:individual-benefits"))))))

      verify(1, putRequestedFor(urlPathEqualTo(s"/application/$applicationId/access/overrides"))
        .withHeader("Authorization", equalTo(authToken))
        .withRequestBody(equalTo( s"""{"overrides":[{"overrideType":"PERSIST_LOGIN_AFTER_GRANT"},{"scopes":["hello","read:individual-benefits"],"overrideType":"SUPPRESS_IV_FOR_AGENTS"}]}""")))

      result shouldBe UpdateOverridesSuccessResult
    }

    "fail if the request failed on the backend" in new Setup {
      val applicationId = "anApplicationId"
      stubFor(put(urlEqualTo(s"/application/$applicationId/access/overrides")).willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR)))

      intercept[Upstream5xxResponse] {
        await(connector.updateOverrides(applicationId,
          UpdateOverridesRequest(Set(SuppressIvForAgents(Set("hello", "read:individual-benefits"))))))
      }

      verify(1, putRequestedFor(urlPathEqualTo(s"/application/$applicationId/access/overrides"))
        .withHeader("Authorization", equalTo(authToken))
        .withRequestBody(equalTo( s"""{"overrides":[{"scopes":["hello","read:individual-benefits"],"overrideType":"SUPPRESS_IV_FOR_AGENTS"}]}""")))
    }
  }

  "updateScopes" should {
    "send Authorisation and return OK if the request was successful on the backend" in new Setup {
      val applicationId = "anApplicationId"
      stubFor(put(urlEqualTo(s"/application/$applicationId/access/scopes")).willReturn(aResponse().withStatus(OK)))

      val result = await(connector.updateScopes(applicationId, UpdateScopesRequest(Set("hello", "read:individual-benefits"))))

      verify(1, putRequestedFor(urlPathEqualTo(s"/application/$applicationId/access/scopes"))
        .withHeader("Authorization", equalTo(authToken))
        .withRequestBody(equalTo( s"""{"scopes":["hello","read:individual-benefits"]}""")))

      result shouldBe UpdateScopesSuccessResult
    }

    "fail if the request failed on the backend" in new Setup {
      val applicationId = "anApplicationId"
      stubFor(put(urlEqualTo(s"/application/$applicationId/access/scopes")).willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR)))

      intercept[Upstream5xxResponse] {
        await(connector.updateScopes(applicationId, UpdateScopesRequest(Set("hello", "read:individual-benefits"))))
      }

      verify(1, putRequestedFor(urlPathEqualTo(s"/application/$applicationId/access/scopes"))
        .withHeader("Authorization", equalTo(authToken))
        .withRequestBody(equalTo( s"""{"scopes":["hello","read:individual-benefits"]}""")))
    }
  }

  "subscribeToApi" should {
    "send Authorisation and return OK if the request was successful on the backend" in new Setup {
      val applicationId = "anApplicationId"
      stubFor(post(urlEqualTo(s"/application/$applicationId/subscription")).willReturn(aResponse().withStatus(CREATED)))

      val result = await(connector.subscribeToApi(applicationId, APIIdentifier("hello", "1.0")))

      verify(1, postRequestedFor(urlPathEqualTo(s"/application/$applicationId/subscription"))
        .withHeader("Authorization", equalTo(authToken))
        .withRequestBody(equalTo( s"""{"context":"hello","version":"1.0"}""")))

      result shouldBe ApplicationUpdateSuccessResult
    }

    "fail if the request failed on the backend" in new Setup {
      val applicationId = "anApplicationId"
      stubFor(post(urlEqualTo(s"/application/$applicationId/subscription")).willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR)))

      intercept[Upstream5xxResponse] {
        await(connector.subscribeToApi(applicationId, APIIdentifier("hello", "1.0")))
      }

      verify(1, postRequestedFor(urlPathEqualTo(s"/application/$applicationId/subscription"))
        .withHeader("Authorization", equalTo(authToken))
        .withRequestBody(equalTo( s"""{"context":"hello","version":"1.0"}""")))
    }
  }

  "unsubscribeFromApi" should {
    "send Authorisation and return OK if the request was successful on the backend" in new Setup {
      val applicationId = "anApplicationId"
      stubFor(delete(urlEqualTo(s"/application/$applicationId/subscription?context=hello&version=1.0")).willReturn(aResponse().withStatus(CREATED)))

      val result = await(connector.unsubscribeFromApi(applicationId, "hello", "1.0"))

      verify(1, deleteRequestedFor(urlPathEqualTo(s"/application/$applicationId/subscription?context=hello&version=1.0"))
        .withHeader("Authorization", equalTo(authToken)))

      result shouldBe ApplicationUpdateSuccessResult
    }

    "fail if the request failed on the backend" in new Setup {
      val applicationId = "anApplicationId"
      stubFor(delete(urlEqualTo(s"/application/$applicationId/subscription?context=hello&version=1.0")).willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR)))

      intercept[Upstream5xxResponse] {
        await(connector.unsubscribeFromApi(applicationId, "hello", "1.0"))
      }

      verify(1, deleteRequestedFor(urlPathEqualTo(s"/application/$applicationId/subscription?context=hello&version=1.0"))
        .withHeader("Authorization", equalTo(authToken)))
    }
  }

  "createPrivOrROPCApp" should {
    "successfully create an application" in new Setup {

      val applicationId = "applicationId"
      val appName = "My new app"
      val appDescription = "An application description"
      val admin = Seq(Collaborator("admin@example.com", CollaboratorRole.ADMINISTRATOR))
      val access = AppAccess(AccessType.PRIVILEGED, Seq())
      val totpSecrets = Some(TotpSecrets("secret", "I am not used"))
      val appAccess = AppAccess(AccessType.PRIVILEGED, Seq())

      val createPrivOrROPCAppRequest = CreatePrivOrROPCAppRequest("PRODUCTION", appName, appDescription, admin, access)
      val createPrivOrROPCAppRequestJson = Json.toJson(createPrivOrROPCAppRequest).toString()
      val createPrivOrROPCAppResponse = CreatePrivOrROPCAppSuccessResult(applicationId, appName, "PRODUCTION", "client ID", totpSecrets, appAccess)

      stubFor(post(urlEqualTo("/application"))
        .withRequestBody(equalToJson(createPrivOrROPCAppRequestJson))
        .willReturn(aResponse().withStatus(OK)
          .withHeader("Content-Type", "application/json")
          .withBody(Json.toJson(createPrivOrROPCAppResponse).toString())
        ))

      val result = await(connector.createPrivOrROPCApp(createPrivOrROPCAppRequest))

      result shouldBe createPrivOrROPCAppResponse
      verify(1, postRequestedFor(urlMatching("/application")).withRequestBody(equalToJson(createPrivOrROPCAppRequestJson)))
    }
  }

  "getClientCredentials" should {
    "return the client credentials" in new Setup {
      val appId = "APP_ID"
      val productionSecret = "production-secret"
      val response =
        s"""
          |{
          |  "production": {
          |    "clientSecrets": [
          |      {
          |        "secret": "$productionSecret"
          |      }
          |    ]
          |  }
          |}
        """.stripMargin
      val expected = GetClientCredentialsResult(ClientCredentials(Seq(ClientSecret(productionSecret))))

      stubFor(get(urlEqualTo(s"/application/$appId/credentials"))
        .willReturn(aResponse().withStatus(OK)
          .withHeader("Content-Type", "application/json")
          .withBody(response)
        ))

      val result = await(connector.getClientCredentials(appId))

      result shouldBe expected
    }
  }

  "addCollaborator" should {
    val appId = "APP_ID"
    val teamMember = Collaborator("newUser@example.com", role = CollaboratorRole.DEVELOPER)
    val addTeamMemberRequest = AddTeamMemberRequest("admin@example.com", teamMember, isRegistered = true, Set.empty)
    val requestBody = Json.toJson(addTeamMemberRequest).toString

    "post the team member to the service" in new Setup {
      stubFor(post(urlEqualTo(s"/application/$appId/collaborator"))
        .withRequestBody(equalToJson(requestBody))
        .willReturn(aResponse().withStatus(OK)
          .withHeader("Content-Type", "application/json")
          .withBody("{}")))

      val result = await(connector.addCollaborator(appId, addTeamMemberRequest))

      verify(1, postRequestedFor(urlMatching(s"/application/$appId/collaborator")).withRequestBody(equalToJson(requestBody)))
    }

    "return ApplicationUpdateSuccessResult when the call is successful" in new Setup {
      stubFor(post(urlEqualTo(s"/application/$appId/collaborator"))
        .withRequestBody(equalToJson(requestBody))
        .willReturn(aResponse().withStatus(OK)
          .withHeader("Content-Type", "application/json")
          .withBody("{}")))

      val result = await(connector.addCollaborator(appId, addTeamMemberRequest))

      result shouldBe ApplicationUpdateSuccessResult
    }

    "throw TeamMemberAlreadyExists when the service returns 409 Conflict" in new Setup {
      stubFor(post(urlEqualTo(s"/application/$appId/collaborator"))
        .withRequestBody(equalToJson(requestBody))
        .willReturn(aResponse().withStatus(CONFLICT)
          .withHeader("Content-Type", "application/json")
          .withBody("{}")))

      intercept[TeamMemberAlreadyExists] {
        await(connector.addCollaborator(appId, addTeamMemberRequest))
      }
    }

    "throw ApplicationNotFound when the service returns 404 Not Found" in new Setup {
      stubFor(post(urlEqualTo(s"/application/$appId/collaborator"))
        .withRequestBody(equalToJson(requestBody))
        .willReturn(aResponse().withStatus(NOT_FOUND)
          .withHeader("Content-Type", "application/json")
          .withBody("{}")))

      intercept[ApplicationNotFound] {
        await(connector.addCollaborator(appId, addTeamMemberRequest))
      }
    }

    "throw the error when the service returns any other error" in new Setup {
      stubFor(post(urlEqualTo(s"/application/$appId/collaborator"))
        .withRequestBody(equalToJson(requestBody))
        .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR)
          .withHeader("Content-Type", "application/json")
          .withBody("{}")))

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
    val path = s"/application/$appId/collaborator/${encode(emailAddress)}?admin=${encode(gatekeeperUserId)}&adminsToEmail=${encode(adminsToEmail.mkString(","))}"

    "send a DELETE request to the service with the correct params" in new Setup {
      stubFor(delete(urlEqualTo(path)).willReturn(aResponse().withStatus(OK)))

      await(connector.removeCollaborator(appId, emailAddress, gatekeeperUserId, adminsToEmail))

      verify(1, deleteRequestedFor(urlEqualTo(path)))
    }

    "return ApplicationUpdateSuccessResult when the call is successful" in new Setup {
      stubFor(delete(urlEqualTo(path)).willReturn(aResponse().withStatus(OK)))

      val result = await(connector.removeCollaborator(appId, emailAddress, gatekeeperUserId, adminsToEmail))

      result shouldBe ApplicationUpdateSuccessResult
    }

    "throw TeamMemberLastAdmin when the service responds with 403" in new Setup {
      stubFor(delete(urlEqualTo(path)).willReturn(aResponse().withStatus(FORBIDDEN)))

      intercept[TeamMemberLastAdmin] {
        await(connector.removeCollaborator(appId, emailAddress, gatekeeperUserId, adminsToEmail))
      }
    }

    "throw the error when the service returns any other error" in new Setup {
      stubFor(delete(urlEqualTo(path)).willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR)))

      intercept[Upstream5xxResponse] {
        await(connector.removeCollaborator(appId, emailAddress, gatekeeperUserId, adminsToEmail))
      }
    }
  }
}
