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
import connectors.{ProductionApiDefinitionConnector, WiremockSugarIt}
import model._
import org.scalatest.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.{Configuration, Mode}
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import model.ApiContext

class ApiStatusMappingIntegrationSpec extends UnitSpec with Matchers with GuiceOneAppPerSuite with WiremockSugarIt {
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
    val apiContext = ApiContext.random
    val apiVersion = ApiVersion.random
  }

  "API status mapping" should {
    "map API status of PROTOTYPED to BETA" in new Setup {
      stubFor(get(urlEqualTo(s"/api-definition")).willReturn(aResponse().withStatus(200).withBody(
        s"""
          |[
          | {
          |   "serviceName": "dummyAPI",
          |   "serviceBaseUrl": "http://localhost/",
          |   "name": "dummyAPI",
          |   "description": "dummy api.",
          |   "context": "${apiContext.value}",
          |   "categories": ["VAT"],
          |   "versions": [
          |     {
          |       "version": "${apiVersion.value}",
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

      val result: Seq[ApiDefinition] = await(connector.fetchPublic())
      
      result shouldBe Seq(ApiDefinition(
        "dummyAPI", "http://localhost/",
        "dummyAPI", "dummy api.", apiContext,
        List(ApiVersionDefinition(apiVersion, ApiStatus.BETA, Some(ApiAccess(APIAccessType.PUBLIC)))), Some(false), Some(List(APICategory("VAT")))))
    }

    "map API status of PUBLISHED to STABLE" in new Setup {
      stubFor(get(urlEqualTo(s"/api-definition")).willReturn(aResponse().withStatus(200).withBody(
        s"""
          |[
          | {
          |   "serviceName": "dummyAPI",
          |   "serviceBaseUrl": "http://localhost/",
          |   "name": "dummyAPI",
          |   "description": "dummy api.",
          |   "context": "${apiContext.value}",
          |   "versions": [
          |     {
          |       "version": "${apiVersion.value}",
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

      val result: Seq[ApiDefinition] = await(connector.fetchPublic())
      
      result shouldBe Seq(ApiDefinition(
        "dummyAPI", "http://localhost/",
        "dummyAPI", "dummy api.", apiContext,
        List(ApiVersionDefinition(apiVersion, ApiStatus.STABLE, Some(ApiAccess(APIAccessType.PUBLIC)))), Some(false), None))
    }
  }
}
