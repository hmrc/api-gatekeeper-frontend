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

package services

import model._
import uk.gov.hmrc.http.HeaderCarrier
import utils.AsyncHmrcSpec

import org.mockito.MockitoSugar
import org.mockito.ArgumentMatchersSugar
import mocks.connectors.ApmConnectorMockProvider

class ApmServiceSpec extends AsyncHmrcSpec {

  trait Setup extends MockitoSugar with ArgumentMatchersSugar with ApmConnectorMockProvider {
    implicit val hc: HeaderCarrier = new HeaderCarrier

    val apmService = new ApmService(mockApmConnector)

    val combinedRestApi1 = CombinedApi(
      "displayName1",
      "serviceName1",
      List(CombinedApiCategory("CUSTOMS")),
      ApiType.REST_API
    )

    val combinedXmlApi2 = CombinedApi(
      "displayName2",
      "serviceName2",
      List(CombinedApiCategory("VAT")),
      ApiType.XML_API
    )
    val combinedList = List(combinedRestApi1, combinedXmlApi2)

  }

  "ApmService" when {

    "fetchApplicationById" should {
      "return None" in new Setup {

        FetchApplicationById.returns(None)

        val result = await(apmService.fetchApplicationById(ApplicationId("applicationId")))
        result shouldBe None
      }
    }

    "fetchAllPossibleSubscriptions" should {
      "return empty Map" in new Setup {

        FetchAllPossibleSubscriptions.returns(Map.empty)

        val result = await(apmService.fetchAllPossibleSubscriptions(ApplicationId("applicationId")))
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
  }

}
