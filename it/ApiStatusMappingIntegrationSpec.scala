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

import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.ProductionApiDefinitionConnector
import model._
import org.scalatest.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.{Configuration, Mode}
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import connectors.WiremockSugar

class ApiStatusMappingIntegrationSpec extends UnitSpec with Matchers with GuiceOneAppPerSuite with WiremockSugar {
  val config = Configuration(
    "microservice.services.api-definition-production.host" -> stubHost,
    "microservice.services.api-definition-production.port" -> stubPort)

  override def fakeApplication = GuiceApplicationBuilder()
    .configure(config)
    .in(Mode.Test)
    .build()

  trait Setup {
    implicit val hc = HeaderCarrier()
    val connector = app.injector.instanceOf[ProductionApiDefinitionConnector]
  }

  "API status mapping" should {
    "map API status of PROTOTYPED to BETA" in new Setup {
      stubFor(get(urlEqualTo(s"/api-definition")).willReturn(aResponse().withStatus(200).withBody(
        """
          |[
          | {
          |   "serviceName": "dummyAPI",
          |   "serviceBaseUrl": "http://localhost/",
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
        "dummyAPI", "http://localhost/",
        "dummyAPI", "dummy api.", "dummy-api",
        Seq(APIVersion("1.0", APIStatus.BETA, Some(APIAccess(APIAccessType.PUBLIC)))), Some(false)))
    }

    "map API status of PUBLISHED to STABLE" in new Setup {
      stubFor(get(urlEqualTo(s"/api-definition")).willReturn(aResponse().withStatus(200).withBody(
        """
          |[
          | {
          |   "serviceName": "dummyAPI",
          |   "serviceBaseUrl": "http://localhost/",
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
        "dummyAPI", "http://localhost/",
        "dummyAPI", "dummy api.", "dummy-api",
        Seq(APIVersion("1.0", APIStatus.STABLE, Some(APIAccess(APIAccessType.PUBLIC)))), Some(false)))
    }
  }
}