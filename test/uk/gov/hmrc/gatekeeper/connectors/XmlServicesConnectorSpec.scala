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

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, UserId, _}
import uk.gov.hmrc.apiplatform.modules.common.utils._
import uk.gov.hmrc.gatekeeper.models.xml.{Collaborator, OrganisationId, VendorId, XmlApi, XmlOrganisation}
import uk.gov.hmrc.gatekeeper.models.{RemoveAllCollaboratorsForUserIdFailureResult, RemoveAllCollaboratorsForUserIdRequest, RemoveAllCollaboratorsForUserIdSuccessResult}
import uk.gov.hmrc.gatekeeper.utils.UrlEncoding

class XmlServicesConnectorSpec
    extends AsyncHmrcSpec
    with WireMockSugar
    with GuiceOneAppPerSuite
    with UrlEncoding {

  val apiVersion1   = ApiVersionNbr.random
  val applicationId = ApplicationId.random

  trait Setup {
    val authToken                  = "Bearer Token"
    implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(("Authorization", authToken))

    val httpClient                                 = app.injector.instanceOf[HttpClientV2]
    val mockAppConfig: XmlServicesConnector.Config = mock[XmlServicesConnector.Config]
    when(mockAppConfig.serviceBaseUrl).thenReturn(wireMockUrl)

    val connector = new XmlServicesConnector(mockAppConfig, httpClient)

    val xmlApiOne = XmlApi(
      name = "xml api 1",
      serviceName = "service name",
      context = "context",
      description = "description"
    )

    val xmlApiTwo = xmlApiOne.copy(name = "xml api 2")
    val xmlApis   = Seq(xmlApiOne, xmlApiTwo)

    val payeCategory    = "PAYE"
    val customsCategory = "CUSTOMS"
  }

  "getAllApis" should {
    val url = "/api-platform-xml-services/xml/apis"

    "return empty Seq when no APIs returned" in new Setup {
      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody("[]")
          )
      )
      await(connector.getAllApis()) shouldBe Seq.empty
    }

    "return Seq with XmlAPis when APIs are returned" in new Setup {
      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.toJson(xmlApis).toString)
          )
      )
      await(connector.getAllApis()) shouldBe xmlApis
    }

    "throw UpstreamErrorResponse when backend returns server error" in new Setup {
      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )
      intercept[UpstreamErrorResponse](await(connector.getAllApis())) match {
        case (e: UpstreamErrorResponse) => succeed
        case _                          => fail()
      }
    }
  }

  "getApisForCategories" should {
    val url = "/api-platform-xml-services/xml/apis/filtered"

    "return empty List when no APIs returned" in new Setup {
      stubFor(
        get(urlEqualTo(url))
          .withQueryParam("categoryFilter", equalTo(payeCategory))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody("[]")
          )
      )
      await(connector.getApisForCategories(List(payeCategory))) shouldBe List.empty
    }

    "return List with XmlAPis when APIs are returned" in new Setup {
      stubFor(
        get(urlPathEqualTo(url))
          .withQueryParam("categoryFilter", equalTo(payeCategory))
          .withQueryParam("categoryFilter", equalTo(customsCategory))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.toJson(xmlApis).toString)
          )
      )
      await(connector.getApisForCategories(List(payeCategory, customsCategory))) shouldBe xmlApis
    }

    // This is a test of XmlServicesConnector.handleUpstream404s
    "return an empty list if the backend is not deployed" in new Setup {
      stubFor(
        get(urlPathEqualTo(url))
          .withQueryParam("categoryFilter", equalTo(payeCategory))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
              .withBody(s"""{"errors":[{"message":"URI not found $url"}]}""")
          )
      )
      await(connector.getApisForCategories(List(payeCategory))) shouldBe List.empty
    }

    "throw UpstreamErrorResponse when a bad category is requested" in new Setup {
      val badCategory = "bad"
      stubFor(
        get(urlPathEqualTo(url))
          .withQueryParam("categoryFilter", equalTo(badCategory))
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST)
              .withBody(s"""{"errors":[{"message":"Unable to bind category $badCategory"}]}""")
          )
      )
      intercept[UpstreamErrorResponse] {
        await(connector.getApisForCategories(List(badCategory)))
      }.getMessage contains badCategory
    }

    "throw UpstreamErrorResponse when backend returns server error" in new Setup {
      stubFor(
        get(urlPathEqualTo(url))
          .withQueryParam("categoryFilter", equalTo(payeCategory))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )
      intercept[UpstreamErrorResponse] {
        await(connector.getApisForCategories(List(payeCategory)))
      }
    }
  }

  "findOrganisationsByUserId" should {
    val url    = "/api-platform-xml-services/organisations"
    val userId = UserId.random
    val orgOne = XmlOrganisation(name = "Organisation one", vendorId = VendorId(1), organisationId = OrganisationId(UUID.randomUUID()), collaborators = List.empty)

    "return APIs when userId exists on an organisation" in new Setup {
      stubFor(
        get(urlEqualTo(s"$url?userId=$userId&sortBy=ORGANISATION_NAME"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.toJson(List(orgOne)).toString)
          )
      )
      await(connector.findOrganisationsByUserId(userId)) shouldBe List(orgOne)
    }

    "return no APIs when userId does not exist in backend" in new Setup {
      stubFor(
        get(urlEqualTo(s"$url?userId=$userId&sortBy=ORGANISATION_NAME"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody("[]")
          )
      )
      await(connector.findOrganisationsByUserId(userId)) shouldBe List.empty
    }

    "return UpstreamErrorResponse when backend returns 400" in new Setup {
      stubFor(
        get(urlEqualTo(s"$url?userId=$userId&sortBy=ORGANISATION_NAME"))
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST)
          )
      )
      intercept[UpstreamErrorResponse](await(connector.findOrganisationsByUserId(userId))) match {
        case UpstreamErrorResponse(_, BAD_REQUEST, _, _) => succeed
        case _                                           => fail()
      }
    }
  }

  "getAllOrganisations" should {
    val url    = "/api-platform-xml-services/organisations"
    val coll   = Collaborator(UserId.random, LaxEmailAddress("developer@example.com"))
    val orgOne = XmlOrganisation(name = "Organisation one", vendorId = VendorId(1), organisationId = OrganisationId(UUID.randomUUID()), collaborators = List(coll))

    "return organisations" in new Setup {
      stubFor(
        get(urlEqualTo(s"$url?sortBy=VENDOR_ID"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.toJson(List(orgOne)).toString)
          )
      )
      await(connector.getAllOrganisations()) shouldBe List(orgOne)
    }

    "return UpstreamErrorResponse when backend returns 400" in new Setup {
      stubFor(
        get(urlEqualTo(s"$url?sortBy=VENDOR_ID"))
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST)
          )
      )
      intercept[UpstreamErrorResponse](await(connector.getAllOrganisations())) match {
        case UpstreamErrorResponse(_, BAD_REQUEST, _, _) => succeed
        case _                                           => fail()
      }
    }
  }

  "removeCollaboratorsForUserId" should {
    val url            = "/api-platform-xml-services/organisations/all/remove-collaborators"
    val userId         = UserId.random
    val gatekeeperUser = "gatekeeperuser@hmrc.gov.uk"

    "return RemoveAllCollaboratorsForUserIdSuccessResult when successful" in new Setup {
      stubFor(
        post(urlEqualTo(url))
          .withJsonRequestBody(RemoveAllCollaboratorsForUserIdRequest(userId, gatekeeperUser))
          .willReturn(
            aResponse()
              .withStatus(NO_CONTENT)
          )
      )
      await(connector.removeCollaboratorsForUserId(userId, gatekeeperUser)) shouldBe RemoveAllCollaboratorsForUserIdSuccessResult
    }

    "return RemoveAllCollaboratorsForUserIdFailureResult when backend returns 400" in new Setup {
      stubFor(
        post(urlEqualTo(url))
          .withJsonRequestBody(RemoveAllCollaboratorsForUserIdRequest(userId, gatekeeperUser))
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST)
          )
      )
      await(connector.removeCollaboratorsForUserId(userId, gatekeeperUser)) shouldBe RemoveAllCollaboratorsForUserIdFailureResult
    }
  }
}
