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

import java.util.UUID

import akka.actor.ActorSystem
import config.AppConfig
import model.Environment._
import model._
import org.mockito.Matchers.{any, eq => meq}
import org.mockito.Mockito.{verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.test.UnitSpec
import utils.FutureTimeoutSupportImpl

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class ApiPublisherConnectorSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {
  private val baseUrl = "https://example.com"
  private val environmentName = "ENVIRONMENT"
  private val bearer = "TestBearerToken"
  private val futureTimeoutSupport = new FutureTimeoutSupportImpl
  private val actorSystemTest = ActorSystem("test-actor-system")
  private val apiKeyTest = UUID.randomUUID().toString

  implicit val hc = HeaderCarrier()

  class Setup(proxyEnabled: Boolean = false) {
    val mockHttpClient = mock[HttpClient]
    val mockProxiedHttpClient = mock[ProxiedHttpClient]
    val mockEnvironment = mock[Environment]
    val mockAppConfig: AppConfig = mock[AppConfig]

    when(mockEnvironment.toString).thenReturn(environmentName)
    when(mockProxiedHttpClient.withHeaders(any(), any())).thenReturn(mockProxiedHttpClient)

    val underTest = new ApiPublisherConnector {
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
      implicit val ec: ExecutionContext = ExecutionContext.global
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

    "when retry logic is enabled should retry on failure" in new Setup {
      val response = Seq(APIApprovalSummary(serviceName, "aName", None, Some(mockEnvironment)))

      when(mockAppConfig.retryCount).thenReturn(1)
      when(mockHttpClient.GET[Seq[APIApprovalSummary]](meq(url))(any(), any(), any()))
        .thenReturn(
          Future.failed(new BadRequestException("")),
          Future.successful(response)
        )
      await(underTest.fetchUnapproved()) shouldBe response
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

    "when retry logic is enabled should retry on failure" in new Setup {
      val validResponse = (APIApprovalSummary(serviceName, "aName", Some("aDescription"), Some(mockEnvironment)))

      when(mockAppConfig.retryCount).thenReturn(1)
      when(mockHttpClient.GET[APIApprovalSummary](meq(url))(any(), any(), any()))
        .thenReturn(
          Future.failed(new BadRequestException("")),
          Future.successful(validResponse)
        )
      await(underTest.fetchApprovalSummary(serviceName)) shouldBe validResponse
    }
  }

  "approveService" should {

    val serviceName = "ServiceName" + UUID.randomUUID()
    val url = s"$baseUrl/service/$serviceName/approve"
    val approveServiceRequest = ApproveServiceRequest(serviceName)

    "save the fields" in new Setup {
      when(mockHttpClient.POST[ApproveServiceRequest, HttpResponse](meq(url), meq(approveServiceRequest), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(OK)))

      await(underTest.approveService(serviceName)) shouldBe ((): Unit)
    }

    "fail when api-subscription-fields returns an internal server error" in new Setup {
      when(mockHttpClient.POST[ApproveServiceRequest, HttpResponse](meq(url), meq(approveServiceRequest), any())(any(), any(), any(), any()))
        .thenReturn(Future.failed(Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      intercept[UpdateApiDefinitionsFailed] {
        await(underTest.approveService(serviceName))
      }
    }
  }

  "http" when {
    "configured not to use the proxy" should {
      "use the HttpClient" in new Setup(proxyEnabled = false) {
        underTest.http shouldBe mockHttpClient
      }
    }

    "configured to use the proxy" should {
      "use the ProxiedHttpClient with the correct authorisation" in new Setup(proxyEnabled = true) {
        underTest.http shouldBe mockProxiedHttpClient

        verify(mockProxiedHttpClient).withHeaders(bearer, apiKeyTest)
      }
    }
  }
}
