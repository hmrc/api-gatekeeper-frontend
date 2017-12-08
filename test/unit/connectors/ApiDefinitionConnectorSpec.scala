/*
 * Copyright 2017 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock._
import config.WSHttp
import connectors.{ApiDefinitionConnector, ApplicationConnector}
import model._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, Matchers}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.http.HeaderCarrier

class ApiDefinitionConnectorSpec extends UnitSpec with Matchers with ScalaFutures with WiremockSugar with BeforeAndAfterEach with WithFakeApplication {

  trait Setup {
    implicit val hc = HeaderCarrier()

    val connector = new ApiDefinitionConnector {
      override val http = WSHttp
      override val serviceBaseUrl: String = wireMockUrl
    }
  }

  "fetchAll" should {
    "respond with 200 and convert body" in new Setup {
      val applicationId = "anApplicationId"
      val gatekeeperId = "loggedin.gatekeeper"
      stubFor(get(urlEqualTo(s"/api-definition")).willReturn(aResponse().withStatus(200).withBody(
        """
          |[
          | {
          |   "serviceName": "dummyAPI",
          |   "serviceBaseUrl": "http://dummy-api.protected.mdtp/",
          |   "name": "dummyAPI",
          |   "description": "dummy api.",
          |   "context": "dummy-api",
          |   "versions": [
          |     {
          |       "version": "1.0",
          |       "status": "STABLE",
          |       "access": {
          |         "type": "PUBLIC"
          |       },
          |       "endpoints": [
          |         {
          |           "uriPattern": "/arrgh",
          |           "endpointName": "dummyAPI",
          |           "method": "GET",
          |           "authType": "USER",
          |           "throttlingTier": "UNLIMITED",
          |           "scope": "read:dummy-api-2"
          |         }
          |       ]
          |     }
          |   ],
          |   "requiresTrust": false
          | }
          |]
        """.stripMargin)))
      val result: Seq[APIDefinition] = await(connector.fetchPublic())

      result shouldBe Seq(APIDefinition(
        "dummyAPI", "http://dummy-api.protected.mdtp/",
        "dummyAPI", "dummy api.", "dummy-api",
        Seq(APIVersion("1.0", APIStatus.STABLE, Some(APIAccess(APIAccessType.PUBLIC)))), Some(false)
      ))
    }

    "map API status of PROTOTYPED to BETA" in new Setup {
      val applicationId = "anApplicationId"
      val gatekeeperId = "loggedin.gatekeeper"
      stubFor(get(urlEqualTo(s"/api-definition")).willReturn(aResponse().withStatus(200).withBody(
        """
          |[
          | {
          |   "serviceName": "dummyAPI",
          |   "serviceBaseUrl": "http://dummy-api.protected.mdtp/",
          |   "name": "dummyAPI",
          |   "description": "dummy api.",
          |   "context": "dummy-api",
          |   "versions": [
          |     {
          |       "version": "1.0",
          |       "status": "PROTOTYPED",
          |       "access": {
          |         "type": "PUBLIC"
          |       },
          |       "endpoints": [
          |         {
          |           "uriPattern": "/arrgh",
          |           "endpointName": "dummyAPI",
          |           "method": "GET",
          |           "authType": "USER",
          |           "throttlingTier": "UNLIMITED",
          |           "scope": "read:dummy-api-2"
          |         }
          |       ]
          |     }
          |   ],
          |   "requiresTrust": false
          | }
          |]
        """.stripMargin)))
      val result: Seq[APIDefinition] = await(connector.fetchPublic())

      result shouldBe Seq(APIDefinition(
        "dummyAPI", "http://dummy-api.protected.mdtp/",
        "dummyAPI", "dummy api.", "dummy-api",
        Seq(APIVersion("1.0", APIStatus.BETA, Some(APIAccess(APIAccessType.PUBLIC)))), Some(false)
      ))
    }

    "map API status of PUBLISHED to STABLE" in new Setup {
      val applicationId = "anApplicationId"
      val gatekeeperId = "loggedin.gatekeeper"
      stubFor(get(urlEqualTo(s"/api-definition")).willReturn(aResponse().withStatus(200).withBody(
        """
          |[
          | {
          |   "serviceName": "dummyAPI",
          |   "serviceBaseUrl": "http://dummy-api.protected.mdtp/",
          |   "name": "dummyAPI",
          |   "description": "dummy api.",
          |   "context": "dummy-api",
          |   "versions": [
          |     {
          |       "version": "1.0",
          |       "status": "PUBLISHED",
          |       "access": {
          |         "type": "PUBLIC"
          |       },
          |       "endpoints": [
          |         {
          |           "uriPattern": "/arrgh",
          |           "endpointName": "dummyAPI",
          |           "method": "GET",
          |           "authType": "USER",
          |           "throttlingTier": "UNLIMITED",
          |           "scope": "read:dummy-api-2"
          |         }
          |       ]
          |     }
          |   ],
          |   "requiresTrust": false
          | }
          |]
        """.stripMargin)))
      val result: Seq[APIDefinition] = await(connector.fetchPublic())

      result shouldBe Seq(APIDefinition(
        "dummyAPI", "http://dummy-api.protected.mdtp/",
        "dummyAPI", "dummy api.", "dummy-api",
        Seq(APIVersion("1.0", APIStatus.STABLE, Some(APIAccess(APIAccessType.PUBLIC)))), Some(false)
      ))
    }

    "propagate FetchApiDefinitionsFailed exception" in new Setup {
      stubFor(get(urlEqualTo(s"/api-definition")).willReturn(aResponse().withStatus(500)))

      intercept[FetchApiDefinitionsFailed](await(connector.fetchPublic()))
    }
  }

  "fetchPrivate" should {
    "respond with 200 and convert body" in new Setup {
      val applicationId = "anApplicationId"
      val gatekeeperId = "loggedin.gatekeeper"
      stubFor(get(urlEqualTo(s"/api-definition?type=private")).willReturn(aResponse().withStatus(200).withBody(
        """
          |[
          | {
          |   "serviceName": "dummyAPI",
          |   "serviceBaseUrl": "http://dummy-api.protected.mdtp/",
          |   "name": "dummyAPI",
          |   "description": "dummy api.",
          |   "context": "dummy-api",
          |   "versions": [
          |     {
          |       "version": "1.0",
          |       "status": "STABLE",
          |       "access": {
          |         "type": "PRIVATE"
          |       },
          |       "endpoints": [
          |         {
          |           "uriPattern": "/arrgh",
          |           "endpointName": "dummyAPI",
          |           "method": "GET",
          |           "authType": "USER",
          |           "throttlingTier": "UNLIMITED",
          |           "scope": "read:dummy-api-2"
          |         }
          |       ]
          |     }
          |   ],
          |   "requiresTrust": false
          | }
          |]
        """.stripMargin)))
      val result: Seq[APIDefinition] = await(connector.fetchPrivate())

      result shouldBe Seq(APIDefinition(
        "dummyAPI", "http://dummy-api.protected.mdtp/",
        "dummyAPI", "dummy api.", "dummy-api",
        Seq(APIVersion("1.0", APIStatus.STABLE, Some(APIAccess(APIAccessType.PRIVATE)))), Some(false)
      ))
    }

    "propagate FetchApiDefinitionsFailed exception" in new Setup {
      stubFor(get(urlEqualTo(s"/api-definition?type=private")).willReturn(aResponse().withStatus(500)))

      intercept[FetchApiDefinitionsFailed](await(connector.fetchPrivate()))
    }
  }
}
