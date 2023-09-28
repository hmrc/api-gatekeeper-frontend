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

package uk.gov.hmrc.gatekeeper.services

import mocks.connectors.ApmConnectorMockProvider
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.Environment

class ApmServiceSpec extends AsyncHmrcSpec {

  trait Setup extends MockitoSugar with ArgumentMatchersSugar with ApmConnectorMockProvider {
    implicit val hc: HeaderCarrier = new HeaderCarrier

    val apmService = new ApmService(mockApmConnector)

    val anAppId = ApplicationId.random

    val combinedRestApi1 = CombinedApi(
      "displayName1",
      "serviceName1",
      Set(ApiCategory.CUSTOMS),
      ApiType.REST_API,
      Some(ApiAccessType.PUBLIC)
    )

    val combinedXmlApi2 = CombinedApi(
      "displayName2",
      "serviceName2",
      Set(ApiCategory.VAT),
      ApiType.XML_API,
      Some(ApiAccessType.PUBLIC)
    )
    val combinedList    = List(combinedRestApi1, combinedXmlApi2)

  }

  "ApmService" when {

    "fetchApplicationById" should {
      "return None" in new Setup {

        FetchApplicationById.returns(None)

        val result = await(apmService.fetchApplicationById(anAppId))
        result shouldBe None
      }
    }

    "fetchAllPossibleSubscriptions" should {
      "return empty Map" in new Setup {

        FetchAllPossibleSubscriptions.returns(Map.empty)

        val result = await(apmService.fetchAllPossibleSubscriptions(anAppId))
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
}
