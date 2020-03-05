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

import com.github.tomakehurst.wiremock.client.WireMock._
import config.AppConfig
import connectors.HttpDeveloperConnector
import model.{DeleteDeveloperRequest, DeveloperDeleteFailureResult, DeveloperDeleteSuccessResult, DeveloperStatusFilter, User}
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Matchers}
import play.api.libs.json.Json
import play.api.test.Helpers.{INTERNAL_SERVER_ERROR, NO_CONTENT, OK}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext.Implicits.global

class HttpDeveloperConnectorSpec
  extends UnitSpec
    with Matchers
    with MockitoSugar
    with ScalaFutures
    with WiremockSugar
    with BeforeAndAfterEach
    with WithFakeApplication {

  trait Setup {
    implicit val hc = HeaderCarrier()

    val mockAppConfig = mock[AppConfig]
    val httpClient = fakeApplication.injector.instanceOf[HttpClient]

    when(mockAppConfig.developerBaseUrl).thenReturn(wireMockUrl)

    val connector = new HttpDeveloperConnector(mockAppConfig, httpClient)
  }

  "Developer connector" should {
    val developerEmail = "developer1@example.com"
    val developerEmailWithSpecialCharacter = "developer2+test@example.com"

    def encode(str: String) = URLEncoder.encode(str, "UTF-8")

    def aUserResponse(email: String) = User(email, "first", "last", verified = Some(false))

    def verifyUserResponse(userResponse: User,
                           expectedEmail: String,
                           expectedFirstName: String, expectedLastName: String) = {
      userResponse.email shouldBe expectedEmail
      userResponse.firstName shouldBe expectedFirstName
      userResponse.lastName shouldBe expectedLastName
    }


    "fetch developer by email" in new Setup {
      stubFor(get(urlEqualTo(s"/developer?email=${encode(developerEmail)}")).willReturn(
        aResponse().withStatus(OK).withBody(
          Json.toJson(aUserResponse(developerEmail)).toString()))
      )
      val result = await(connector.fetchByEmail(developerEmail))
      verifyUserResponse(result, developerEmail, "first", "last")
    }

    "fetch developer by email when special characters in the email" in new Setup {
      stubFor(get(urlEqualTo(s"/developer?email=${encode(developerEmailWithSpecialCharacter)}")).willReturn(
        aResponse().withStatus(OK).withBody(
          Json.toJson(aUserResponse(developerEmailWithSpecialCharacter)).toString()))
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
            Json.toJson(Seq(aUserResponse(developerEmail), aUserResponse(developerEmailWithSpecialCharacter))).toString()))
      )

      val result = await(connector.fetchByEmails(Seq(developerEmail, developerEmailWithSpecialCharacter)))
      verifyUserResponse(result(0), developerEmail, "first", "last")
      verifyUserResponse(result(1), developerEmailWithSpecialCharacter, "first", "last")
    }


    "fetch all developers" in new Setup {
      stubFor(get(urlEqualTo("/developers/all")).willReturn(
        aResponse().withStatus(OK).withBody(
          Json.toJson(Seq(aUserResponse(developerEmail), aUserResponse(developerEmailWithSpecialCharacter))).toString()))
      )
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
      val user: User = aUserResponse(developerEmail)
      val loggedInUser: String = "admin-user"
      stubFor(post(urlEqualTo(s"/developer/$developerEmail/mfa/remove"))
        .willReturn(aResponse().withStatus(OK).withBody(Json.toJson(user).toString())))

      val result: User = await(connector.removeMfa(developerEmail, loggedInUser))

      result shouldBe user
    }

    "search by email filter" in new Setup {

      val url = s"/developers?emailFilter=${encode(developerEmail)}&status=${"ALL"}"
      stubFor(get(urlEqualTo(url)).willReturn(
        aResponse().withStatus(OK).withBody(
          Json.toJson(Seq(aUserResponse(developerEmail))).toString()))
      )
      val result = await(connector.searchDevelopers(Some(developerEmail), DeveloperStatusFilter.AllStatus))

      verify(getRequestedFor(urlPathEqualTo(url)))

      result shouldBe List(aUserResponse(developerEmail))

    }

    "search by developer status filter" in new Setup {

      val url = s"/developers?status=${encode("VERIFIED")}"
      stubFor(get(urlEqualTo(url)).willReturn(
        aResponse().withStatus(OK).withBody(
          Json.toJson(Seq(aUserResponse(developerEmail))).toString()))
      )
      val result = await(connector.searchDevelopers(None, DeveloperStatusFilter.VerifiedStatus))

      verify(getRequestedFor(urlPathEqualTo(url)))

      result shouldBe List(aUserResponse(developerEmail))

    }

    "search by email filter and developer status filter" in new Setup {

      val url = s"/developers?emailFilter=${encode(developerEmail)}&status=${encode("VERIFIED")}"
      stubFor(get(urlEqualTo(url)).willReturn(
        aResponse().withStatus(OK).withBody(
          Json.toJson(Seq(aUserResponse(developerEmail))).toString()))
      )
      val result = await(connector.searchDevelopers(Some(developerEmail), DeveloperStatusFilter.VerifiedStatus))

      verify(getRequestedFor(urlPathEqualTo(url)))

      result shouldBe List(aUserResponse(developerEmail))

    }
  }
}