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

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.client.WireMock.{verify => wireMockVerify}
import uk.gov.hmrc.gatekeeper.config.AppConfig
import uk.gov.hmrc.gatekeeper.models._
import org.scalatest.BeforeAndAfterEach
import play.api.libs.json.Json
import play.api.test.Helpers.{INTERNAL_SERVER_ERROR, NO_CONTENT, OK}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.gatekeeper.utils._

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.gatekeeper.encryption.PayloadEncryption
import uk.gov.hmrc.gatekeeper.connectors.DeveloperConnector.FindUserIdRequestWrite
import uk.gov.hmrc.gatekeeper.connectors.DeveloperConnector.RemoveMfaRequest
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.apiplatform.modules.common.utils._
import uk.gov.hmrc.apiplatform.modules.developers.domain.models.UserId

class HttpDeveloperConnectorSpec
    extends AsyncHmrcSpec
    with WireMockSugar
    with BeforeAndAfterEach
    with GuiceOneAppPerSuite
    with UrlEncoding {

  trait Setup {
    implicit val hc = HeaderCarrier()

    val mockAppConfig         = mock[AppConfig]
    val mockPayloadEncryption = new PayloadEncryption("gvBoGdgzqG1AarzF1LY0zQ==")
    val httpClient            = app.injector.instanceOf[HttpClient]

    when(mockAppConfig.developerBaseUrl).thenReturn(wireMockUrl)

    val connector = new HttpDeveloperConnector(mockAppConfig, httpClient, mockPayloadEncryption)
  }

  def mockFetchUserId(email: String, userId: UserId) = {
    import uk.gov.hmrc.gatekeeper.connectors.DeveloperConnector._
    implicit val writer = Json.writes[FindUserIdResponse]

    stubFor(
      post(urlEqualTo("/developers/find-user-id"))
        .withJsonRequestBody(FindUserIdRequest(email))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withJsonBody(FindUserIdResponse(userId))
        )
    )
  }

  def mockSeekRegisteredUser(user: RegisteredUser) = {
    stubFor(
      get(urlPathEqualTo("/developer"))
        .withQueryParam("developerId", equalTo(user.userId.value.toString))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withJsonBody(user)
        )
    )
  }

  "Developer connector" should {
    val developerEmail                     = "developer1@example.com"
    val developerEmailWithSpecialCharacter = "developer2+test@example.com"

    def aUserResponse(email: String, id: UserId = UserId.random) = RegisteredUser(email, id, "first", "last", verified = false)

    def verifyUserResponse(userResponse: User, expectedEmail: String, expectedFirstName: String, expectedLastName: String) = {
      userResponse.email shouldBe expectedEmail
      userResponse.firstName shouldBe expectedFirstName
      userResponse.lastName shouldBe expectedLastName
    }

    "fetch developer by email" in new Setup {
      val userId = UserId.random
      mockFetchUserId(developerEmail, userId)

      stubFor(
        get(urlPathEqualTo("/developer"))
          .withQueryParam("developerId", equalTo(encode(userId.value.toString)))
          .willReturn(
            aResponse().withStatus(OK).withBody(Json.toJson(aUserResponse(developerEmail, userId)).toString)
          )
      )

      val result = await(connector.fetchByEmail(developerEmail))

      verifyUserResponse(result, developerEmail, "first", "last")
    }

    "fetch developer by email when special characters in the email" in new Setup {

      val userId = UserId.random
      mockFetchUserId(developerEmailWithSpecialCharacter, userId)

      stubFor(
        get(urlPathEqualTo("/developer"))
          .withQueryParam("developerId", equalTo(encode(userId.value.toString)))
          .willReturn(
            aResponse().withStatus(OK).withBody(Json.toJson(aUserResponse(developerEmailWithSpecialCharacter, userId)).toString)
          )
      )

      val result = await(connector.fetchByEmail(developerEmailWithSpecialCharacter))

      verifyUserResponse(result, developerEmailWithSpecialCharacter, "first", "last")
    }

    "fetch all developers by emails (new)" in new Setup {
      val postBody = Json.toJson(List(developerEmail, developerEmailWithSpecialCharacter))

      stubFor(post(urlEqualTo(s"/developers/get-by-emails"))
        .withRequestBody(equalToJson(postBody.toString))
        .willReturn(
          aResponse().withStatus(OK).withBody(
            Json.toJson(Seq(aUserResponse(developerEmail, UserId.random), aUserResponse(developerEmailWithSpecialCharacter, UserId.random))).toString()
          )
        ))

      val result = await(connector.fetchByEmails(Seq(developerEmail, developerEmailWithSpecialCharacter)))

      verifyUserResponse(result(0), developerEmail, "first", "last")
      verifyUserResponse(result(1), developerEmailWithSpecialCharacter, "first", "last")
    }

    "fetch all developers" in new Setup {
      stubFor(get(urlEqualTo("/developers/all")).willReturn(
        aResponse().withStatus(OK).withBody(
          Json.toJson(Seq(aUserResponse(developerEmail, UserId.random), aUserResponse(developerEmailWithSpecialCharacter, UserId.random))).toString()
        )
      ))

      val result = await(connector.fetchAll())

      verifyUserResponse(result(0), developerEmail, "first", "last")
      verifyUserResponse(result(1), developerEmailWithSpecialCharacter, "first", "last")
    }

    "delete a developer and return a success result" in new Setup {
      stubFor(post(urlEqualTo("/developer/delete")).willReturn(aResponse().withStatus(NO_CONTENT)))

      val result = await(connector.deleteDeveloper(DeleteDeveloperRequest("gate.keeper", "developer@example.com")))

      result shouldBe DeveloperDeleteSuccessResult
    }

    "delete a developer and return a failure result when an error occurred" in new Setup {
      stubFor(post(urlEqualTo("/developer/delete")).willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR)))

      val result = await(connector.deleteDeveloper(DeleteDeveloperRequest("gate.keeper", "developer@example.com")))

      result shouldBe DeveloperDeleteFailureResult
    }

    "remove MFA for a developer" in new Setup {
      val emailAddress = "someone@example.com"
      val user         = RegisteredUser(emailAddress, UserId.random, "Firstname", "Lastname", true)
      val developerId  = UuidIdentifier(user.userId)

      mockSeekRegisteredUser(user)
      val loggedInUser: String = "admin-user"

      stubFor(
        post(urlEqualTo(s"/developer/${user.userId.value}/mfa/remove"))
          .withJsonRequestBody(RemoveMfaRequest(loggedInUser))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withJsonBody(user)
          )
      )

      val result = await(connector.removeMfa(developerId, loggedInUser))

      result shouldBe user
    }

    "search by email filter" in new Setup {
      val url  = "/developers/search"
      val user = aUserResponse(developerEmail)

      stubFor(post(urlEqualTo(url)).willReturn(
        aResponse().withStatus(OK).withBody(
          Json.toJson(Seq(user)).toString()
        )
      ))

      val result = await(connector.searchDevelopers(Some(developerEmail), DeveloperStatusFilter.AllStatus))

      wireMockVerify(postRequestedFor(urlPathEqualTo(url)))

      result shouldBe List(user)
    }

    "search by developer status filter" in new Setup {
      val url  = "/developers/search"
      val user = aUserResponse(developerEmail)

      stubFor(post(urlEqualTo(url)).willReturn(
        aResponse().withStatus(OK).withBody(
          Json.toJson(Seq(user)).toString()
        )
      ))

      val result = await(connector.searchDevelopers(None, DeveloperStatusFilter.VerifiedStatus))

      wireMockVerify(postRequestedFor(urlPathEqualTo(url)))

      result shouldBe List(user)
    }

    "search by email filter and developer status filter" in new Setup {
      val url  = "/developers/search"
      val user = aUserResponse(developerEmail)

      stubFor(post(urlEqualTo(url)).willReturn(
        aResponse().withStatus(OK).withBody(
          Json.toJson(Seq(user)).toString()
        )
      ))

      val result = await(connector.searchDevelopers(Some(developerEmail), DeveloperStatusFilter.VerifiedStatus))

      wireMockVerify(postRequestedFor(urlPathEqualTo(url)))

      result shouldBe List(user)
    }

    "Search by Email Preferences" should {
      val url = s"/developers/email-preferences"

      "make a call with topic passed into the service and return users from response" in new Setup {
        val user = aUserResponse(developerEmail)

        stubFor(
          get(urlPathEqualTo(url))
            .withQueryParam("topic", equalTo(TopicOptionChoice.BUSINESS_AND_POLICY.toString))
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(Json.toJson(Seq(user)).toString())
            )
        )

        val result = await(connector.fetchByEmailPreferences(TopicOptionChoice.BUSINESS_AND_POLICY))

        wireMockVerify(getRequestedFor(urlPathEqualTo(url)))

        result shouldBe List(user)
      }

      "make a call with topic and api category passed into the service and return users from response" in new Setup {
        val url      = s"""/developers/email-preferences\\?topic=${TopicOptionChoice.BUSINESS_AND_POLICY.toString}&regime=VAT&regime=API1"""
        val user     = aUserResponse(developerEmail)
        val matching = urlMatching(url)

        stubFor(
          get(matching)
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(Json.toJson(Seq(user)).toString())
            )
        )

        val result =
          await(connector.fetchByEmailPreferences(TopicOptionChoice.BUSINESS_AND_POLICY, maybeApis = None, maybeApiCategories = Some(Seq(APICategory("VAT"), APICategory("API1")))))

        wireMockVerify(getRequestedFor(matching))

        result shouldBe List(user)

      }

      "make a call with topic, api categories and apis passed into the service and return users from response" in new Setup {
        val url      = s"""/developers/email-preferences\\?topic=${TopicOptionChoice.BUSINESS_AND_POLICY.toString}&regime=VAT&regime=API1&service=service1&service=service2"""
        val user     = aUserResponse(developerEmail)
        val matching = urlMatching(url)

        stubFor(
          get(matching)
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(Json.toJson(Seq(user)).toString())
            )
        )

        val result = await(connector.fetchByEmailPreferences(
          TopicOptionChoice.BUSINESS_AND_POLICY,
          maybeApis = Some(Seq("service1", "service2")),
          maybeApiCategories = Some(Seq(APICategory("VAT"), APICategory("API1")))
        ))

        wireMockVerify(getRequestedFor(matching))

        result shouldBe List(user)
      }
    }
  }
}
