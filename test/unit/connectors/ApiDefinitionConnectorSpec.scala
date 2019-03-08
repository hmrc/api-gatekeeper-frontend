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

import config.AppConfig
import connectors.{ApiDefinitionConnector, ProxiedHttpClient}
import model._
import model.Environment._
import org.mockito.Matchers.{any, eq => meq}
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Matchers}
import play.api.http.Status._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class ApiDefinitionConnectorSpec extends UnitSpec with MockitoSugar with Matchers with ScalaFutures with BeforeAndAfterEach {
  private val baseUrl = "https://example.com"
  private val environmentName = "ENVIRONMENT"

  trait Setup {
    implicit val hc = HeaderCarrier()

    val mockHttpClient = mock[HttpClient]
    val mockProxiedHttpClient = mock[ProxiedHttpClient]
    val mockEnvironment = mock[Environment]

    when(mockEnvironment.toString).thenReturn(environmentName)

    val connector = new ApiDefinitionConnector {
      val httpClient = mockHttpClient
      val proxiedHttpClient = mockProxiedHttpClient
      val serviceBaseUrl = baseUrl
      val useProxy = false
      val bearerToken = "TestBearerToken"
      val environment = mockEnvironment
    }
  }

  "fetchAll" should {
    val url = s"$baseUrl/api-definition"

    "respond with 200 and convert body" in new Setup {
      val response = Seq(APIDefinition(
        "dummyAPI", "http://localhost/",
        "dummyAPI", "dummy api.", "dummy-api",
        Seq(APIVersion("1.0", APIStatus.STABLE, Some(APIAccess(APIAccessType.PUBLIC)))), Some(false)))

      when(mockHttpClient.GET[Seq[APIDefinition]](meq(url))( any(), any(), any())).thenReturn(Future.successful(response))

      await(connector.fetchPublic()) shouldBe response
    }

    // TODO: test in spec for JsonFormatters -> APIStatusJson
    // "map API status of PROTOTYPED to BETA" in new Setup {
    //   val applicationId = "anApplicationId"
    //   val gatekeeperId = "loggedin.gatekeeper"
    //   stubFor(get(urlEqualTo(s"/api-definition")).willReturn(aResponse().withStatus(200).withBody(
    //     """
    //       |[
    //       | {
    //       |   "serviceName": "dummyAPI",
    //       |   "serviceBaseUrl": "http://localhost/",
    //       |   "name": "dummyAPI",
    //       |   "description": "dummy api.",
    //       |   "context": "dummy-api",
    //       |   "versions": [
    //       |     {
    //       |       "version": "1.0",
    //       |       "status": "PROTOTYPED",
    //       |       "access": {
    //       |         "type": "PUBLIC"
    //       |       },
    //       |       "endpoints": [
    //       |         {
    //       |           "uriPattern": "/arrgh",
    //       |           "endpointName": "dummyAPI",
    //       |           "method": "GET",
    //       |           "authType": "USER",
    //       |           "throttlingTier": "UNLIMITED",
    //       |           "scope": "read:dummy-api-2"
    //       |         }
    //       |       ]
    //       |     }
    //       |   ],
    //       |   "requiresTrust": false
    //       | }
    //       |]
    //     """.stripMargin)))
    //   val result: Seq[APIDefinition] = await(connector.fetchPublic())

    //   result shouldBe Seq(APIDefinition(
    //     "dummyAPI", "http://localhost/",
    //     "dummyAPI", "dummy api.", "dummy-api",
    //     Seq(APIVersion("1.0", APIStatus.BETA, Some(APIAccess(APIAccessType.PUBLIC)))), Some(false)
    //   ))
    // }

    // TODO: test in spec for JsonFormatters -> APIStatusJson
    // "map API status of PUBLISHED to STABLE" in new Setup {
    //   val applicationId = "anApplicationId"
    //   val gatekeeperId = "loggedin.gatekeeper"
    //   stubFor(get(urlEqualTo(s"/api-definition")).willReturn(aResponse().withStatus(200).withBody(
    //     """
    //       |[
    //       | {
    //       |   "serviceName": "dummyAPI",
    //       |   "serviceBaseUrl": "http://localhost/",
    //       |   "name": "dummyAPI",
    //       |   "description": "dummy api.",
    //       |   "context": "dummy-api",
    //       |   "versions": [
    //       |     {
    //       |       "version": "1.0",
    //       |       "status": "PUBLISHED",
    //       |       "access": {
    //       |         "type": "PUBLIC"
    //       |       },
    //       |       "endpoints": [
    //       |         {
    //       |           "uriPattern": "/arrgh",
    //       |           "endpointName": "dummyAPI",
    //       |           "method": "GET",
    //       |           "authType": "USER",
    //       |           "throttlingTier": "UNLIMITED",
    //       |           "scope": "read:dummy-api-2"
    //       |         }
    //       |       ]
    //       |     }
    //       |   ],
    //       |   "requiresTrust": false
    //       | }
    //       |]
    //     """.stripMargin)))
    //   val result: Seq[APIDefinition] = await(connector.fetchPublic())

    //   result shouldBe Seq(APIDefinition(
    //     "dummyAPI", "http://localhost/",
    //     "dummyAPI", "dummy api.", "dummy-api",
    //     Seq(APIVersion("1.0", APIStatus.STABLE, Some(APIAccess(APIAccessType.PUBLIC)))), Some(false)
    //   ))
    // }

    "propagate FetchApiDefinitionsFailed exception" in new Setup {
      when(mockHttpClient.GET[Seq[APIDefinition]](meq(url))(any(), any(), any()))
        .thenReturn(Future.failed(Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      intercept[FetchApiDefinitionsFailed](await(connector.fetchPublic()))
    }
  }

  "fetchPrivate" should {
    val url = s"$baseUrl/api-definition?type=private"

    "respond with 200 and convert body" in new Setup {
      val applicationId = "anApplicationId"
      val gatekeeperId = "loggedin.gatekeeper"
      val response = Seq(APIDefinition(
        "dummyAPI", "http://localhost/",
        "dummyAPI", "dummy api.", "dummy-api",
        Seq(APIVersion("1.0", APIStatus.STABLE, Some(APIAccess(APIAccessType.PRIVATE)))), Some(false)))

      when(mockHttpClient.GET[Seq[APIDefinition]](meq(url))(any(), any(), any())).thenReturn(Future.successful(response))

      await(connector.fetchPrivate()) shouldBe response
    }

    "propagate FetchApiDefinitionsFailed exception" in new Setup {
      when(mockHttpClient.GET[Seq[APIDefinition]](meq(url))(any(), any(), any()))
        .thenReturn(Future.failed(Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      intercept[FetchApiDefinitionsFailed](await(connector.fetchPrivate()))
    }
  }
}
