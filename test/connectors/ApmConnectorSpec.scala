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

import org.mockito.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec
import org.scalatest.concurrent.ScalaFutures
import org.mockito.ArgumentMatchersSugar
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import model.applications.ApplicationWithSubscriptionData
import model.ApplicationId
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier
import builder.{ApplicationBuilder, ApiBuilder}
import model.ApiContext
import model.subscriptions.ApiData
import model.ApiIdentifier
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.Upstream5xxResponse
import play.api.test.Helpers._
import model.ApplicationUpdateSuccessResult
import model.ApiVersion

class ApmConnectorSpec extends UnitSpec with MockitoSugar with ArgumentMatchersSugar with ScalaFutures {
    val mockHttp = mock[HttpClient] 
    val mockApmConnectorConfig: ApmConnector.Config = mock[ApmConnector.Config]

    val applicationId = ApplicationId.random

    when(mockApmConnectorConfig.serviceBaseUrl).thenReturn("https://example.com")

    trait Setup extends ApplicationBuilder with ApiBuilder {
        implicit val hc = HeaderCarrier()
        
        val application = buildApplication(applicationId)
    }

    val underTest = new ApmConnector(mockHttp, mockApmConnectorConfig)

    "fetchApplicationById" should {
        val url = s"${mockApmConnectorConfig.serviceBaseUrl}/applications/${applicationId.value}"

        "return ApplicationWithSubscriptionData" in new Setup {
            val applicationWithSubscriptionData = ApplicationWithSubscriptionData(application, Set.empty, Map.empty)

            when(mockHttp.GET[Option[ApplicationWithSubscriptionData]](eqTo(url))(*, *, *)).thenReturn(Future.successful(Some(applicationWithSubscriptionData)))

            val result = await(underTest.fetchApplicationById(applicationId))
            result should not be None
            result.map { appWithSubsData =>
                appWithSubsData.application shouldBe application
            }
        }
    }

    "fetchAllPossibleSubscriptions" should {
        val url = s"${mockApmConnectorConfig.serviceBaseUrl}/api-definitions"
        
        val queryParams = Seq(
            ApmConnector.applicationIdQueryParam -> applicationId.value,
            ApmConnector.restrictedQueryParam -> "false"
        )
        
        "return all subscribeable API's and their ApiData" in new Setup with ApiBuilder {
            val apiData = DefaultApiData.addVersion(VersionOne, DefaultVersionData)
            val apiContext = ApiContext("Api Context")
            val apiContextAndApiData = Map(apiContext -> apiData)

            when(mockHttp.GET[Map[ApiContext, ApiData]](eqTo(url), eqTo(queryParams))(*, *, *)).thenReturn(Future.successful(apiContextAndApiData))

            val result = await(underTest.fetchAllPossibleSubscriptions(applicationId))
            result(apiContext).name shouldBe "API Name" 
        }
    }

    "subscribeToApi" should {
        val applicationId = ApplicationId.random
        val apiContext = ApiContext.random
        val apiVersion = ApiVersion.random
        val apiIdentifier = ApiIdentifier(apiContext, apiVersion)
        val url = s"https://example.com/applications/${applicationId.value}/subscriptions"

        "send Authorisation and return OK if the request was successful on the backend" in new Setup {
            when(mockHttp.POST[ApiIdentifier, HttpResponse](eqTo(url), eqTo(apiIdentifier), *)(*, *, *, *))
                .thenReturn(Future.successful(HttpResponse(CREATED))) 

            val result = await(underTest.subscribeToApi(applicationId, apiIdentifier))

            result shouldBe ApplicationUpdateSuccessResult
        }

        "fail if the request failed on the backend" in new Setup {
            when(mockHttp.POST[ApiIdentifier, HttpResponse](eqTo(url), eqTo(apiIdentifier), *)(*, *, *, *))
                .thenReturn(Future.failed(Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

            intercept[Upstream5xxResponse] {
                await(underTest.subscribeToApi(applicationId, apiIdentifier))
            }
        }
    }
}
