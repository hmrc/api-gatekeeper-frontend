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

import java.util.UUID

import connectors.{ApiPublisherConnector, ProxiedHttpClient}
import model.Environment._
import model._
import org.mockito.Matchers.{any, eq => meq}
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class ApiPublisherConnectorSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {
  private val baseUrl = "https://example.com"
  private val environmentName = "ENVIRONMENT"

  implicit val hc = HeaderCarrier()

  trait Setup {
    val mockHttpClient = mock[HttpClient]
    val mockProxiedHttpClient = mock[ProxiedHttpClient]
    val mockEnvironment = mock[Environment]

    when(mockEnvironment.toString).thenReturn(environmentName)

    val underTest = new ApiPublisherConnector {
      val httpClient = mockHttpClient
      val proxiedHttpClient = mockProxiedHttpClient
      val serviceBaseUrl = baseUrl
      val useProxy = false
      val bearerToken = "TestBearerToken"
      val environment = mockEnvironment
    }
  }

  "fetchUnapproved" should {
    val serviceName = "ServiceName" + UUID.randomUUID()
    val url = s"$baseUrl/services/unapproved"

    "return unapproved API approval summaries" in new Setup {
      val response = Seq(APIApprovalSummary(serviceName, "aName", None, Some(mockEnvironment)))

      when(mockHttpClient.GET[Seq[APIApprovalSummary]](meq(url))(any(), any(), any()))
        .thenReturn(Future.successful(response))

      await(underTest.fetchUnapproved()) shouldBe response
    }

    "fail when api-subscription-fields returns an internal server error" in new Setup {
      when(mockHttpClient.GET[Seq[APIApprovalSummary]](meq(url))(any(), any(), any()))
        .thenReturn(Future.failed(Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      intercept[Upstream5xxResponse] {
        await(underTest.fetchUnapproved())
      }
    }
  }

  "fetchApprovalSummary" should {
    val serviceName = "ServiceName" + UUID.randomUUID()
    val url = s"$baseUrl/service/$serviceName/summary"

    "return subscription fields definition for an API" in new Setup {
      val validResponse = APIApprovalSummary(serviceName, "aName", Some("aDescription"), Some(mockEnvironment))

      when(mockHttpClient.GET[APIApprovalSummary](meq(url))(any(), any(), any()))
        .thenReturn(Future.successful(validResponse))

      await(underTest.fetchApprovalSummary(serviceName)) shouldBe validResponse
    }

    "fail when api-subscription-fields returns an internal server error" in new Setup {
      when(mockHttpClient.GET[Seq[APIApprovalSummary]](meq(url))(any(), any(), any()))
        .thenReturn(Future.failed(Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      intercept[Upstream5xxResponse] {
        await(underTest.fetchApprovalSummary(serviceName))
      }
    }
  }

  "approveService" should {

    val serviceName = "ServiceName" + UUID.randomUUID()
    val url = s"$baseUrl/service/$serviceName/approve"
    val approveServiceRequest = ApproveServiceRequest(serviceName)

    "save the fields" in new Setup {
      when(mockHttpClient.POST[ApproveServiceRequest, HttpResponse](meq(url), meq(approveServiceRequest), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(OK)))

      await(underTest.approveService(serviceName)) shouldBe ()
    }

    "fail when api-subscription-fields returns an internal server error" in new Setup {
      when(mockHttpClient.POST[ApproveServiceRequest, HttpResponse](meq(url), meq(approveServiceRequest), any())(any(), any(), any(), any()))
        .thenReturn(Future.failed(Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      intercept[UpdateApiDefinitionsFailed] {
        await(underTest.approveService(serviceName))
      }
    }
  }
}
