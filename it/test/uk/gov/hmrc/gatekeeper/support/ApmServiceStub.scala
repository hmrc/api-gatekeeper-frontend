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

package uk.gov.hmrc.gatekeeper.support

import com.github.tomakehurst.wiremock.client.WireMock._

import play.api.http.Status

import uk.gov.hmrc.apiplatform.modules.common.utils.WireMockExtensions
import uk.gov.hmrc.gatekeeper.models.CombinedApi

trait ApmServiceStub extends WireMockExtensions {

  def primeFetchAllCombinedApisSuccess(combinedApis: List[CombinedApi]): Unit = {
    stubFor(get(urlEqualTo("/combined-rest-xml-apis"))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withJsonBody(combinedApis)
      ))
  }
}
