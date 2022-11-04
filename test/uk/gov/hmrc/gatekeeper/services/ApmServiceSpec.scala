/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.gatekeeper.services

import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.apiplatform.modules.common.utils.AsyncHmrcSpec

import org.mockito.MockitoSugar
import org.mockito.ArgumentMatchersSugar
import mocks.connectors.ApmConnectorMockProvider
import uk.gov.hmrc.gatekeeper.builder.{ApplicationBuilder, ApplicationResponseBuilder}
import uk.gov.hmrc.gatekeeper.models.APIAccessType.PUBLIC

import java.time.LocalDateTime

class ApmServiceSpec extends AsyncHmrcSpec {

  trait Setup extends MockitoSugar with ArgumentMatchersSugar with ApmConnectorMockProvider with ApplicationBuilder with ApplicationResponseBuilder {
    implicit val hc: HeaderCarrier = new HeaderCarrier

    val apmService = new ApmService(mockApmConnector)

    val applicationId = ApplicationId.random
    val application = buildApplicationResponse(applicationId)
    val newApplication = buildApplication(applicationId)

    val combinedRestApi1 = CombinedApi(
      "displayName1",
      "serviceName1",
      List(CombinedApiCategory("CUSTOMS")),
      ApiType.REST_API,
      Some(PUBLIC)
    )

    val combinedXmlApi2 = CombinedApi(
      "displayName2",
      "serviceName2",
      List(CombinedApiCategory("VAT")),
      ApiType.XML_API,
      Some(PUBLIC)
    )
    val combinedList = List(combinedRestApi1, combinedXmlApi2)

  }

  "ApmService" when {

    "fetchApplicationById" should {
      "return None" in new Setup {

        FetchApplicationById.returns(None)

        val result = await(apmService.fetchApplicationById(applicationId))
        result shouldBe None
      }
    }

    "fetchAllPossibleSubscriptions" should {
      "return empty Map" in new Setup {

        FetchAllPossibleSubscriptions.returns(Map.empty)

        val result = await(apmService.fetchAllPossibleSubscriptions(applicationId))
        result shouldBe Map.empty
      }
    }

    "getAllFieldDefinitions" should {
      "return empty field definitions" in new Setup {

        GetAllFieldDefinitions.returns(Map.empty)

        val result = await(apmService.getAllFieldDefinitions(Environment.PRODUCTION))
        result shouldBe Map.empty
      }
    }

    "fetchAllCombinedApis" should {
      "return a list of CombinedApi" in new Setup {

        FetchAllCombinedApis.returns(combinedList)

        val result = await(apmService.fetchAllCombinedApis())
        result shouldBe combinedList
      }
    }

    "fetchAllBoxes" should {
      "return a list of PPNS Boxes" in new Setup {
        val allBoxes = List.empty
        
        FetchAllBoxes.returns(allBoxes)

        val result = await(apmService.fetchAllBoxes())
        result shouldBe allBoxes
      }
    }
  }

  "subscribeToApi" should {
    "return success" in new Setup {
      val subscribeToApi = SubscribeToApi(GatekeeperActor("Gate Keeper"), ApiIdentifier.random, LocalDateTime.now()) 
      ApmConnectorMock.SubscribeToApi.succeeds()
        
      val result = await(apmService.subscribeToApi(application, subscribeToApi))

      result shouldBe ApplicationUpdateSuccessResult
    }
  }
  
  "unsubscribeFromApi" should {
    "return success" in new Setup {
      val unsubscribeFromApi = UnsubscribeFromApi(GatekeeperActor("Gate Keeper"), ApiIdentifier.random, LocalDateTime.now())
      ApmConnectorMock.UpdateApplication.succeeds(newApplication)

      val result = await(apmService.unsubscribeFromApi(application, unsubscribeFromApi))

      result shouldBe ApplicationUpdateSuccessResult
    }
  }
}
